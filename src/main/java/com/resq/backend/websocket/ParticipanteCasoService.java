package com.resq.backend.websocket;

import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HU-38a: decide quien participa de un caso, que es la condicion para recibir eventos
 * de la sala case:{id}. Hoy participan el reportante y el representante de la
 * organizacion que tomo el caso. HU-22 (#41) extiende el criterio (participantes del
 * chat) sin tocar el transporte.
 */
@Service
public class ParticipanteCasoService {

    private final ReporteRepository reporteRepository;
    private final OrganizacionRepository organizacionRepository;

    public ParticipanteCasoService(ReporteRepository reporteRepository,
                                   OrganizacionRepository organizacionRepository) {
        this.reporteRepository = reporteRepository;
        this.organizacionRepository = organizacionRepository;
    }

    public boolean participa(Long idUsuario, Long idCaso) {
        if (idUsuario == null || idCaso == null) {
            return false;
        }
        return reporteRepository.findById(idCaso)
                .map(reporte -> esParticipante(reporte, idUsuario))
                .orElse(false);
    }

    /** Ids de los casos en los que participa el usuario, sin repetir. */
    public List<Long> casosDe(Long idUsuario) {
        List<Long> ids = new ArrayList<>();
        if (idUsuario == null) {
            return ids;
        }
        for (Reporte reporte : reporteRepository.findByIdUsuario(idUsuario)) {
            agregarSinDuplicar(ids, reporte.getIdReporte());
        }
        Optional<Organizacion> organizacion = organizacionRepository.findByIdRepresentante(idUsuario);
        if (organizacion.isPresent()) {
            for (Reporte reporte : reporteRepository.findByIdOrganizacion(organizacion.get().getIdOrganizacion())) {
                agregarSinDuplicar(ids, reporte.getIdReporte());
            }
        }
        return ids;
    }

    private boolean esParticipante(Reporte reporte, Long idUsuario) {
        if (idUsuario.equals(reporte.getIdUsuario())) {
            return true;
        }
        return reporte.getIdOrganizacion() != null && esRepresentanteDe(reporte, idUsuario);
    }

    private boolean esRepresentanteDe(Reporte reporte, Long idUsuario) {
        return organizacionRepository.findById(reporte.getIdOrganizacion())
                .map(Organizacion::getIdRepresentante)
                .filter(idUsuario::equals)
                .isPresent();
    }

    private void agregarSinDuplicar(List<Long> ids, Long idCaso) {
        if (idCaso != null && !ids.contains(idCaso)) {
            ids.add(idCaso);
        }
    }
}
