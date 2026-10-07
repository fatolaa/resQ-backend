package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionRegistradaDTO;
import com.resq.backend.dto.OrganizacionRequestDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.exception.FotoInvalidaException;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** HU-20: registro de organizaciones y regla de "no gestiona casos hasta ser verificada". */
@Service
public class OrganizacionService {

    /** Los logos se guardan con el mismo servicio de fotos de HU-10 y se sirven por su endpoint público. */
    private static final String RUTA_FOTOS = "/api/reportes/fotos/";

    private final OrganizacionRepository organizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final FotoStorageService fotoStorage;
    private final EmailService emailService;
    private final Validator validator;

    public OrganizacionService(OrganizacionRepository organizacionRepository,
                               UsuarioRepository usuarioRepository,
                               FotoStorageService fotoStorage,
                               EmailService emailService,
                               Validator validator) {
        this.organizacionRepository = organizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.fotoStorage = fotoStorage;
        this.emailService = emailService;
        this.validator = validator;
    }

    /**
     * Registra la organización en estado PENDIENTE_VERIFICACION y envía el correo de
     * confirmación. El correo es un efecto secundario: si falla, el registro se mantiene.
     */
    public OrganizacionRegistradaDTO registrar(OrganizacionRequestDTO solicitud, MultipartFile logo) {
        OrganizacionRequestDTO datos = normalizar(solicitud);
        validar(datos);

        if (!usuarioRepository.existsById(datos.idRepresentante())) {
            throw new OrganizacionException(HttpStatus.NOT_FOUND, "El usuario representante no existe",
                    Map.of("idRepresentante", "El usuario representante no existe"));
        }
        if (organizacionRepository.existsByIdRepresentante(datos.idRepresentante())) {
            throw new OrganizacionException(HttpStatus.CONFLICT, "Ya registraste una organización",
                    Map.of("idRepresentante", "Este usuario ya tiene una organización registrada"));
        }
        if (organizacionRepository.existsByEmailIgnoreCase(datos.email())) {
            throw new OrganizacionException(HttpStatus.CONFLICT, "Ese email ya está registrado",
                    Map.of("email", "Ya existe una organización registrada con este email"));
        }

        // El logo se valida y guarda antes de crear la organización: si es inválido no queda nada a medias.
        String nombreLogo = logo != null && !logo.isEmpty() ? guardarLogo(logo) : null;

        Organizacion organizacion = new Organizacion();
        organizacion.setIdRepresentante(datos.idRepresentante());
        organizacion.setNombre(datos.nombre());
        organizacion.setTipo(datos.tipo());
        organizacion.setDireccion(datos.direccion());
        organizacion.setTelefono(datos.telefono());
        organizacion.setEmail(datos.email());
        organizacion.setDescripcion(datos.descripcion());
        organizacion.setHorarios(datos.horarios());
        organizacion.setZonasCobertura(datos.zonasCobertura());
        organizacion.setLogoUrl(nombreLogo == null ? null : RUTA_FOTOS + nombreLogo);
        organizacion.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);

        Organizacion guardada = organizacionRepository.save(organizacion);

        boolean enviado = emailService.enviar(
                guardada.getEmail(),
                "ResQ: recibimos el registro de tu organización",
                cuerpoConfirmacion(guardada));

        return new OrganizacionRegistradaDTO(guardada, enviado);
    }

    public Optional<Organizacion> obtenerDeRepresentante(Long idUsuario) {
        return idUsuario == null ? Optional.empty() : organizacionRepository.findByIdRepresentante(idUsuario);
    }

    /**
     * HU-28: edita los datos de la organización conservando representante, estado de
     * verificación y fecha de registro. El logo se reemplaza si se sube uno, se quita
     * con quitarLogo, o se deja igual si no viene ninguna de las dos cosas.
     */
    public Organizacion actualizar(Long idOrganizacion, OrganizacionRequestDTO solicitud,
                                   MultipartFile logo, boolean quitarLogo) {
        OrganizacionRequestDTO datos = normalizar(solicitud);
        validar(datos);

        Organizacion organizacion = organizacionRepository.findById(idOrganizacion)
                .orElseThrow(() -> new OrganizacionException(HttpStatus.NOT_FOUND, "La organización no existe",
                        Map.of("idOrganizacion", "La organización no existe")));

        if (!organizacion.getIdRepresentante().equals(datos.idRepresentante())) {
            throw new OrganizacionException(HttpStatus.FORBIDDEN, "Solo puedes editar tu propia organización",
                    Map.of("idRepresentante", "Solo el representante puede editar su organización"));
        }

        Optional<Organizacion> conEmail = organizacionRepository.findByEmailIgnoreCase(datos.email());
        if (conEmail.isPresent() && !conEmail.get().getIdOrganizacion().equals(idOrganizacion)) {
            throw new OrganizacionException(HttpStatus.CONFLICT, "Ese email ya está registrado",
                    Map.of("email", "Ya existe una organización registrada con este email"));
        }

        aplicarLogo(organizacion, logo, quitarLogo);

        organizacion.setNombre(datos.nombre());
        organizacion.setTipo(datos.tipo());
        organizacion.setDireccion(datos.direccion());
        organizacion.setTelefono(datos.telefono());
        organizacion.setEmail(datos.email());
        organizacion.setDescripcion(datos.descripcion());
        organizacion.setHorarios(datos.horarios());
        organizacion.setZonasCobertura(datos.zonasCobertura());

        return organizacionRepository.save(organizacion);
    }

    /**
     * Regla reutilizable: solo el representante de una organización VERIFICADA puede
     * gestionar casos. Cualquier funcionalidad de organizaciones que actúe sobre casos
     * debe consultarla antes de permitir la acción.
     */
    public boolean puedeGestionarCasos(Long idUsuario) {
        return obtenerDeRepresentante(idUsuario)
                .map(Organizacion::puedeGestionarCasos)
                .orElse(false);
    }

    private String guardarLogo(MultipartFile logo) {
        try {
            return fotoStorage.guardar(logo);
        } catch (FotoInvalidaException e) {
            throw new OrganizacionException(e.getStatus(), e.getMessage(), Map.of("logo", e.getMessage()));
        }
    }

    private void aplicarLogo(Organizacion organizacion, MultipartFile logo, boolean quitarLogo) {
        if (logo != null && !logo.isEmpty()) {
            // El nuevo logo se valida y guarda ANTES de borrar el anterior: si es
            // inválido, la edición falla y la organización queda como estaba.
            String nombreNuevo = guardarLogo(logo);
            eliminarLogoActual(organizacion);
            organizacion.setLogoUrl(RUTA_FOTOS + nombreNuevo);
            return;
        }
        if (quitarLogo) {
            eliminarLogoActual(organizacion);
            organizacion.setLogoUrl(null);
        }
    }

    private void eliminarLogoActual(Organizacion organizacion) {
        String logoUrl = organizacion.getLogoUrl();
        if (logoUrl == null || !logoUrl.startsWith(RUTA_FOTOS)) {
            return;
        }
        String nombre = logoUrl.substring(RUTA_FOTOS.length());
        if (!nombre.isBlank()) {
            fotoStorage.eliminar(nombre);
        }
    }

    private void validar(OrganizacionRequestDTO datos) {
        Set<ConstraintViolation<OrganizacionRequestDTO>> violaciones = validator.validate(datos);
        if (violaciones.isEmpty()) {
            return;
        }
        // Un mensaje por campo; TreeMap + putIfAbsent para que la respuesta sea estable.
        Map<String, String> errores = new TreeMap<>();
        violaciones.stream()
                .sorted((a, b) -> a.getMessage().compareTo(b.getMessage()))
                .forEach(v -> errores.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        throw new OrganizacionException(HttpStatus.BAD_REQUEST, "Revisa los datos del formulario", errores);
    }

    private static OrganizacionRequestDTO normalizar(OrganizacionRequestDTO d) {
        return new OrganizacionRequestDTO(
                d.idRepresentante(),
                limpiar(d.nombre()),
                d.tipo() == null ? null : d.tipo().trim().toUpperCase(),
                limpiar(d.direccion()),
                limpiar(d.telefono()),
                limpiar(d.email()),
                limpiar(d.descripcion()),
                limpiar(d.horarios()),
                limpiar(d.zonasCobertura()));
    }

    /** Recorta espacios; un texto vacío queda como null para que @NotBlank lo detecte. */
    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private static String cuerpoConfirmacion(Organizacion o) {
        return "Hola,\n\n"
                + "Recibimos el registro de \"" + o.getNombre() + "\" en ResQ.\n\n"
                + "Estado: pendiente de verificación.\n"
                + "Un administrador revisará tu registro. Hasta que sea verificada, tu organización "
                + "no podrá gestionar casos. Puedes ver el estado de tu organización en cualquier "
                + "momento iniciando sesión en ResQ.\n\n"
                + "Gracias por sumarte,\n"
                + "El equipo de ResQ";
    }
}
