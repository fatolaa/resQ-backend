package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.dto.MetricasDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.MetricasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-34: las metricas de la comunidad solo las consulta el administrador.
 */
@WebMvcTest(MetricasController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("MetricasController API (HU-34)")
class MetricasControllerTest {

    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_USUARIO = "token-usuario";
    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricasService metricasService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_ADMIN)).thenReturn(1L);
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_USUARIO)).thenReturn(2L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ADMIN")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "VOLUNTARIO")));
    }

    private static Usuario usuario(Long id, String rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombre("Cuenta " + id);
        usuario.setEmail("cuenta" + id + "@resq.com");
        usuario.setPasswordHash("hash");
        usuario.setRol(rol);
        return usuario;
    }

    private static MetricasDTO metricas() {
        return new MetricasDTO(DESDE, HASTA, "DIA",
                new MetricasDTO.Usuarios(5, 3, 40),
                new MetricasDTO.Voluntarios(2, 9, List.of(new MetricasDTO.VoluntariosPorTipo("TRANSPORTE", 1, 4))),
                new MetricasDTO.Organizaciones(1, 6, List.of(
                        new MetricasDTO.OrganizacionesPorTipo("REFUGIO", 1, 2, 3, 1, 6))),
                List.of(new MetricasDTO.PuntoEvolucion(DESDE, 5, 3)));
    }

    // ---------------------------------------------------------------- acceso

    @Test
    @DisplayName("rechaza la consulta sin sesion iniciada (401)")
    void rechazaSinSesion() throws Exception {
        mockMvc.perform(get("/api/metricas").param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized());

        verify(metricasService, never()).calcular(any(), any());
    }

    @Test
    @DisplayName("rechaza la consulta de una cuenta que no es administradora (403)")
    void rechazaNoAdmin() throws Exception {
        mockMvc.perform(get("/api/metricas")
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden());

        verify(metricasService, never()).calcular(any(), any());
    }

    // ---------------------------------------------------------------- consulta

    @Test
    @DisplayName("devuelve las metricas del periodo al administrador")
    void devuelveLasMetricas() throws Exception {
        when(metricasService.calcular(DESDE, HASTA)).thenReturn(metricas());

        mockMvc.perform(get("/api/metricas")
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.agrupacion").value("DIA"))
                .andExpect(jsonPath("$.usuarios.registrados").value(5))
                .andExpect(jsonPath("$.usuarios.activos").value(3))
                .andExpect(jsonPath("$.voluntarios.porTipoAyuda[0].tipoAyuda").value("TRANSPORTE"))
                .andExpect(jsonPath("$.organizaciones.porTipo[0].verificadas").value(3))
                .andExpect(jsonPath("$.evolucion[0].inicio").value("2026-10-01"));
    }

    @Test
    @DisplayName("responde 400 con el motivo cuando el periodo no es valido")
    void periodoInvalido() throws Exception {
        when(metricasService.calcular(HASTA, DESDE))
                .thenThrow(new IllegalArgumentException("La fecha inicial no puede ser posterior a la final"));

        mockMvc.perform(get("/api/metricas")
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .param("desde", "2026-10-31").param("hasta", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la final"))
                .andExpect(jsonPath("$.errors.periodo").exists());
    }

    @Test
    @DisplayName("responde 400 cuando falta una fecha")
    void faltaUnaFecha() throws Exception {
        when(metricasService.calcular(DESDE, null))
                .thenThrow(new IllegalArgumentException("Debes indicar la fecha inicial y la final del periodo"));

        mockMvc.perform(get("/api/metricas")
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .param("desde", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debes indicar la fecha inicial y la final del periodo"));
    }
}
