package com.resq.backend.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reporte: los campos de revisión no se pueden fijar desde el JSON (HU-19)")
class ReporteRevisionJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("un usuario no puede aprobar su propio reporte enviando estadoRevision")
    void ignoraCamposDeRevisionAlRecibir() throws Exception {
        String json = """
                {"idUsuario": 10, "tipoCaso": "PERDIDA", "estado": "PENDIENTE",
                 "estadoRevision": "APROBADO", "notaRevision": "me autoapruebo",
                 "idRevisor": 10, "fechaRevision": "2026-09-28T10:00:00"}
                """;

        Reporte reporte = mapper.readValue(json, Reporte.class);

        assertThat(reporte.getEstadoRevision()).isNull();
        assertThat(reporte.getNotaRevision()).isNull();
        assertThat(reporte.getIdRevisor()).isNull();
        assertThat(reporte.getFechaRevision()).isNull();
    }

    @Test
    @DisplayName("al guardar por primera vez el reporte queda pendiente de revisión")
    void nuevoReporteQuedaPendiente() {
        Reporte reporte = new Reporte();

        reporte.prePersist();

        assertThat(reporte.getEstadoRevision()).isEqualTo(EstadoRevision.PENDIENTE);
    }

    @Test
    @DisplayName("al responder sí se incluyen estado y nota para que el dueño los vea")
    void incluyeCamposDeRevisionAlResponder() throws Exception {
        Reporte reporte = new Reporte();
        reporte.setEstadoRevision(EstadoRevision.RECHAZADO);
        reporte.setNotaRevision("Foto borrosa");

        String json = mapper.writeValueAsString(reporte);

        assertThat(json).contains("\"estadoRevision\":\"RECHAZADO\"").contains("\"notaRevision\":\"Foto borrosa\"");
    }
}
