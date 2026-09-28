package com.resq.backend.controller;

import com.resq.backend.entity.Reporte;
import com.resq.backend.repository.ReporteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReporteController API: ubicacion del reporte (HU-16)")
class ReporteControllerUbicacionHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteRepository reporteRepository;

    @Test
    @DisplayName("crea el reporte devolviendo la ubicacion asociada")
    void debeCrearElReporteConSuUbicacion() throws Exception {
        when(reporteRepository.save(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(post("/api/reportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idUsuario": 10,
                                  "tipoCaso": "PERDIDA",
                                  "descripcion": "Perro perdido cerca a la plaza",
                                  "estado": "PENDIENTE",
                                  "latitud": -17.3895,
                                  "longitud": -66.1568
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latitud").value(-17.3895))
                .andExpect(jsonPath("$.longitud").value(-66.1568));
    }

    @Test
    @DisplayName("rechaza una ubicacion incompleta indicando que ambas coordenadas son obligatorias juntas")
    void debeRechazarUnaUbicacionIncompleta() throws Exception {
        mockMvc.perform(post("/api/reportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idUsuario": 10,
                                  "tipoCaso": "PERDIDA",
                                  "descripcion": "Perro perdido cerca a la plaza",
                                  "estado": "PENDIENTE",
                                  "latitud": -17.3895
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.reporte")
                        .value("latitud y longitud deben enviarse juntas"));
    }

    @Test
    @DisplayName("acepta un reporte sin ubicacion porque la ubicacion es opcional")
    void debeAceptarUnReporteSinUbicacion() throws Exception {
        when(reporteRepository.save(any(Reporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(post("/api/reportes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idUsuario": 10,
                                  "tipoCaso": "PERDIDA",
                                  "descripcion": "Perro perdido cerca a la plaza",
                                  "estado": "PENDIENTE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latitud").doesNotExist())
                .andExpect(jsonPath("$.longitud").doesNotExist());
    }
}
