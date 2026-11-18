package com.resq.backend.service;

import com.resq.backend.dto.ContactoReporteDTO;
import com.resq.backend.dto.ContactoReporteDTO.Telefono;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.validation.Telefonos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contacto de quien reporta un caso.
 *
 * Quien reporta elige en cada reporte si muestra el telefono de su cuenta, otro
 * telefono solo para ese caso, los dos o ninguno. El telefono de la cuenta no se
 * copia al reporte: se lee de la cuenta al consultarlo, asi siempre esta al dia.
 *
 * El contacto solo se entrega con sesion iniciada y mientras el caso esta abierto.
 */
@Service
public class ContactoReporteService {

    private static final Logger logger = LoggerFactory.getLogger(ContactoReporteService.class);

    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;

    public ContactoReporteService(ReporteRepository reporteRepository,
                                  UsuarioRepository usuarioRepository,
                                  OrganizacionRepository organizacionRepository) {
        this.reporteRepository = reporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacionRepository = organizacionRepository;
    }

    /**
     * Valida y limpia el contacto antes de guardar un reporte nuevo.
     *
     * @throws ResponseStatusException 400 si el telefono no es valido o la cuenta no
     *         tiene telefono; 403 si se intenta compartir el telefono de otra cuenta.
     */
    public void prepararParaGuardar(Reporte reporte) {
        try {
            reporte.setTelefonoContacto(Telefonos.normalizar(reporte.getTelefonoContacto()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        if (!Boolean.TRUE.equals(reporte.getCompartirTelefonoCuenta())) {
            reporte.setCompartirTelefonoCuenta(false);
            return;
        }
        // Solo el dueño de la cuenta puede decidir mostrar su telefono: si no, alguien
        // podria crear un reporte a nombre de otra persona y publicar su numero.
        Long idActual = UsuarioAutenticado.idActual().orElse(null);
        if (idActual == null || !idActual.equals(reporte.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes compartir el teléfono de tu propia cuenta");
        }
        String telefonoCuenta = usuarioRepository.findById(idActual).map(Usuario::getTelefono).orElse(null);
        if (telefonoCuenta == null || telefonoCuenta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tu cuenta no tiene un teléfono registrado");
        }
    }

    /**
     * Contacto de quien reporto el caso, si lo acepto y el caso sigue abierto.
     *
     * @throws ResponseStatusException 404 si el caso no existe.
     */
    @Transactional(readOnly = true)
    public Optional<ContactoReporteDTO> contactoDe(Long idReporte, Long idSolicitante) {
        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El caso no existe"));
        if (!reporte.tieneContacto()) {
            return Optional.empty();
        }
        Usuario autor = usuarioRepository.findById(reporte.getIdUsuario()).orElse(null);

        List<Telefono> telefonos = new ArrayList<>();
        if (Boolean.TRUE.equals(reporte.getCompartirTelefonoCuenta()) && autor != null) {
            String deCuenta = normalizarSinFallar(autor.getTelefono());
            if (deCuenta != null) {
                telefonos.add(new Telefono(deCuenta, Telefono.CUENTA));
            }
        }
        String delCaso = reporte.getTelefonoContacto();
        if (delCaso != null && telefonos.stream().noneMatch(t -> t.numero().equals(delCaso))) {
            telefonos.add(new Telefono(delCaso, Telefono.CASO));
        }
        if (telefonos.isEmpty()) {
            return Optional.empty();
        }

        // Queda registro de quien vio el contacto, por si hay que investigar un abuso.
        logger.info("Usuario ID {} consultó el contacto del reporte ID {}", idSolicitante, idReporte);
        return Optional.of(new ContactoReporteDTO(
                autor != null ? autor.getNombre() : null,
                insigniaDe(autor),
                List.copyOf(telefonos)));
    }

    private String insigniaDe(Usuario autor) {
        if (autor == null) {
            return null;
        }
        boolean representaOrganizacionVerificada = organizacionRepository
                .findByIdRepresentante(autor.getIdUsuario())
                .map(o -> EstadoVerificacion.VERIFICADA.equals(o.getEstadoVerificacion()))
                .orElse(false);
        if (representaOrganizacionVerificada) {
            return ContactoReporteDTO.INSIGNIA_ORGANIZACION;
        }
        return "VOLUNTARIO".equals(autor.getRol()) ? ContactoReporteDTO.INSIGNIA_VOLUNTARIO : null;
    }

    /** Los telefonos de cuentas viejas no se validaban: si no es valido, no se muestra. */
    private static String normalizarSinFallar(String telefono) {
        try {
            return Telefonos.normalizar(telefono);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
