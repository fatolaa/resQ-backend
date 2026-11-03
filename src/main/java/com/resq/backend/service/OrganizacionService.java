package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionRegistradaDTO;
import com.resq.backend.dto.OrganizacionRequestDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.RevisionOrganizacion;
import com.resq.backend.entity.TipoOrganizacion;
import com.resq.backend.exception.FotoInvalidaException;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.RevisionOrganizacionRepository;
import com.resq.backend.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** HU-20: registro de organizaciones y regla de "no gestiona casos hasta ser verificada". */
@Service
public class OrganizacionService {

    /** Los logos se guardan con el mismo servicio de fotos de HU-10 y se sirven por su endpoint público. */
    private static final String RUTA_FOTOS = "/api/reportes/fotos/";

    private static final Logger logger = LoggerFactory.getLogger(OrganizacionService.class);

    private final OrganizacionRepository organizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final FotoStorageService fotoStorage;
    private final DocumentoStorageService documentoStorage;
    private final EmailService emailService;
    private final Validator validator;
    private final RevisionOrganizacionRepository revisionRepository;

    public OrganizacionService(OrganizacionRepository organizacionRepository,
                               UsuarioRepository usuarioRepository,
                               FotoStorageService fotoStorage,
                               DocumentoStorageService documentoStorage,
                               EmailService emailService,
                               Validator validator,
                               RevisionOrganizacionRepository revisionRepository) {
        this.organizacionRepository = organizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.fotoStorage = fotoStorage;
        this.documentoStorage = documentoStorage;
        this.emailService = emailService;
        this.validator = validator;
        this.revisionRepository = revisionRepository;
    }

    /**
     * Registra la organización en estado PENDIENTE_VERIFICACION y envía el correo de
     * confirmación. El correo es un efecto secundario: si falla, el registro se mantiene.
     */
    public OrganizacionRegistradaDTO registrar(OrganizacionRequestDTO solicitud, MultipartFile logo) {
        return registrar(solicitud, logo, null);
    }

    /**
     * HU-32: igual que {@link #registrar(OrganizacionRequestDTO, MultipartFile)}, con el
     * documento de verificacion (opcional) que el administrador revisa antes de aprobar.
     */
    public OrganizacionRegistradaDTO registrar(OrganizacionRequestDTO solicitud, MultipartFile logo,
                                               MultipartFile documento) {
        OrganizacionRequestDTO datos = normalizar(solicitud);
        validar(datos);

        if (!usuarioRepository.existsById(datos.idRepresentante())) {
            logger.warn("Registro de organizacion rechazado: el usuario representante ID {} no existe",
                    datos.idRepresentante());
            throw new OrganizacionException(HttpStatus.NOT_FOUND, "El usuario representante no existe",
                    Map.of("idRepresentante", "El usuario representante no existe"));
        }
        if (organizacionRepository.existsByIdRepresentante(datos.idRepresentante())) {
            logger.warn("Registro de organizacion rechazado: el usuario ID {} ya tiene una organizacion",
                    datos.idRepresentante());
            throw new OrganizacionException(HttpStatus.CONFLICT, "Ya registraste una organización",
                    Map.of("idRepresentante", "Este usuario ya tiene una organización registrada"));
        }
        if (organizacionRepository.existsByEmailIgnoreCase(datos.email())) {
            logger.warn("Registro de organizacion rechazado: el email ya esta registrado");
            throw new OrganizacionException(HttpStatus.CONFLICT, "Ese email ya está registrado",
                    Map.of("email", "Ya existe una organización registrada con este email"));
        }

        // El logo se valida y guarda antes de crear la organización: si es inválido no queda nada a medias.
        String nombreLogo = logo != null && !logo.isEmpty() ? guardarLogo(logo) : null;
        String nombreDocumento = documento != null && !documento.isEmpty()
                ? documentoStorage.guardar(documento) : null;

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
        organizacion.setDocumento(nombreDocumento);
        organizacion.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);

        Organizacion guardada = organizacionRepository.save(organizacion);
        logger.info("Organizacion ID {} registrada (representante ID {}, estado PENDIENTE)",
                guardada.getIdOrganizacion(), guardada.getIdRepresentante());

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
     * HU-29: directorio de organizaciones. Solo se listan las VERIFICADAS por un
     * administrador y se puede filtrar por tipo y por zona de cobertura. El tipo se
     * valida para no devolver un directorio vacío silenciosamente ante un valor mal escrito.
     */
    public List<Organizacion> consultarDirectorio(String tipo, String zona) {
        String tipoFiltro = normalizarTipoFiltro(tipo);
        String zonaFiltro = limpiar(zona);

        return organizacionRepository.findByEstadoVerificacion(EstadoVerificacion.VERIFICADA).stream()
                .filter(o -> tipoFiltro == null || tipoFiltro.equalsIgnoreCase(o.getTipo()))
                .filter(o -> zonaFiltro == null || coincideZona(o.getZonasCobertura(), zonaFiltro))
                .sorted(Comparator.comparing(Organizacion::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * HU-29: ficha de una organización. Pendientes y rechazadas no son públicas, así
     * que para el directorio se comportan como si no existieran (404).
     */
    public Optional<Organizacion> obtenerFicha(Long idOrganizacion) {
        if (idOrganizacion == null) {
            return Optional.empty();
        }
        return organizacionRepository.findById(idOrganizacion)
                .filter(o -> EstadoVerificacion.VERIFICADA.equals(o.getEstadoVerificacion()));
    }

    private static String normalizarTipoFiltro(String tipo) {
        String limpio = limpiar(tipo);
        if (limpio == null) {
            return null;
        }
        String mayusculas = limpio.toUpperCase(Locale.ROOT);
        if (!TipoOrganizacion.VALORES.contains(mayusculas)) {
            throw new OrganizacionException(HttpStatus.BAD_REQUEST,
                    "Tipo inválido: " + limpio + ". Permitidos: " + String.join(", ", TipoOrganizacion.VALORES),
                    Map.of("tipo", "El tipo debe ser REFUGIO, VETERINARIA o RESCATISTA_INDEPENDIENTE"));
        }
        return mayusculas;
    }

    private static boolean coincideZona(String zonasCobertura, String zona) {
        return zonasCobertura != null
                && zonasCobertura.toLowerCase(Locale.ROOT).contains(zona.toLowerCase(Locale.ROOT));
    }

    /**
     * HU-28: edita los datos de la organización conservando representante, estado de
     * verificación y fecha de registro. El logo se reemplaza si se sube uno, se quita
     * con quitarLogo, o se deja igual si no viene ninguna de las dos cosas.
     */
    public Organizacion actualizar(Long idOrganizacion, OrganizacionRequestDTO solicitud,
                                   MultipartFile logo, boolean quitarLogo) {
        return actualizar(idOrganizacion, solicitud, logo, quitarLogo, null);
    }

    /**
     * HU-32: ademas de la ficha, reemplaza el documento de verificacion si se sube uno nuevo
     * (por ejemplo, para corregir lo que indico el administrador al rechazar).
     */
    public Organizacion actualizar(Long idOrganizacion, OrganizacionRequestDTO solicitud,
                                   MultipartFile logo, boolean quitarLogo, MultipartFile documento) {
        OrganizacionRequestDTO datos = normalizar(solicitud);
        validar(datos);

        Organizacion organizacion = organizacionRepository.findById(idOrganizacion)
                .orElseThrow(() -> {
                    logger.warn("Edicion de organizacion rechazada: la organizacion ID {} no existe", idOrganizacion);
                    return new OrganizacionException(HttpStatus.NOT_FOUND, "La organización no existe",
                            Map.of("idOrganizacion", "La organización no existe"));
                });

        if (!organizacion.getIdRepresentante().equals(datos.idRepresentante())) {
            logger.warn("Edicion de organizacion rechazada: usuario ID {} no es el representante de la organizacion ID {}",
                    datos.idRepresentante(), idOrganizacion);
            throw new OrganizacionException(HttpStatus.FORBIDDEN, "Solo puedes editar tu propia organización",
                    Map.of("idRepresentante", "Solo el representante puede editar su organización"));
        }

        Optional<Organizacion> conEmail = organizacionRepository.findByEmailIgnoreCase(datos.email());
        if (conEmail.isPresent() && !conEmail.get().getIdOrganizacion().equals(idOrganizacion)) {
            logger.warn("Edicion de organizacion rechazada: el email ya pertenece a otra organizacion");
            throw new OrganizacionException(HttpStatus.CONFLICT, "Ese email ya está registrado",
                    Map.of("email", "Ya existe una organización registrada con este email"));
        }

        aplicarLogo(organizacion, logo, quitarLogo);
        aplicarDocumento(organizacion, documento);

        organizacion.setNombre(datos.nombre());
        organizacion.setTipo(datos.tipo());
        organizacion.setDireccion(datos.direccion());
        organizacion.setTelefono(datos.telefono());
        organizacion.setEmail(datos.email());
        organizacion.setDescripcion(datos.descripcion());
        organizacion.setHorarios(datos.horarios());
        organizacion.setZonasCobertura(datos.zonasCobertura());

        Organizacion guardada = organizacionRepository.save(organizacion);
        logger.info("Organizacion ID {} actualizada", guardada.getIdOrganizacion());
        return guardada;
    }

    /**
     * HU-32: una organización rechazada vuelve a enviar su solicitud después de corregir lo que
     * indicó el administrador (los datos y el documento se corrigen antes con
     * {@link #actualizar}). Vuelve a PENDIENTE_VERIFICACION y el reenvío queda en el historial.
     *
     * @throws OrganizacionException 404 si no existe, 403 si quien la reenvía no es su
     *                               representante, 409 si no está rechazada.
     */
    public Organizacion reenviar(Long idOrganizacion, Long idUsuario) {
        Organizacion organizacion = organizacionRepository.findById(idOrganizacion)
                .orElseThrow(() -> new OrganizacionException(HttpStatus.NOT_FOUND, "La organización no existe",
                        Map.of("idOrganizacion", "La organización no existe")));
        if (!organizacion.getIdRepresentante().equals(idUsuario)) {
            logger.warn("Reenvio rechazado: usuario ID {} no representa a la organizacion ID {}",
                    idUsuario, idOrganizacion);
            throw new OrganizacionException(HttpStatus.FORBIDDEN, "Solo puedes reenviar tu propia organización",
                    Map.of("idRepresentante", "Solo el representante puede reenviar la solicitud"));
        }
        if (!EstadoVerificacion.RECHAZADA.equals(organizacion.getEstadoVerificacion())) {
            throw new OrganizacionException(HttpStatus.CONFLICT,
                    "Solo se puede reenviar una solicitud rechazada",
                    Map.of("estadoVerificacion", organizacion.getEstadoVerificacion()));
        }

        organizacion.setEstadoVerificacion(EstadoVerificacion.PENDIENTE);
        Organizacion guardada = organizacionRepository.save(organizacion);
        revisionRepository.save(new RevisionOrganizacion(idOrganizacion, RevisionOrganizacion.REENVIADA,
                null, idUsuario, LocalDateTime.now()));
        logger.info("Organizacion ID {} reenviada a verificacion por su representante ID {}",
                idOrganizacion, idUsuario);
        return guardada;
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

    /** El nuevo documento se guarda antes de borrar el anterior, igual que el logo. */
    private void aplicarDocumento(Organizacion organizacion, MultipartFile documento) {
        if (documento == null || documento.isEmpty()) {
            return;
        }
        String nombreNuevo = documentoStorage.guardar(documento);
        documentoStorage.eliminar(organizacion.getDocumento());
        organizacion.setDocumento(nombreNuevo);
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
        logger.warn("Organizacion rechazada por validacion: {} campo(s) con error", errores.size());
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
