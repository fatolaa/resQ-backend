package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.dto.MetricasCasosDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.MetricasCasosService;
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
 * HU-33: los indicadores de casos y reportes solo los consulta el administrador.
 */
@WebMvcTest(MetricasCasosController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("MetricasCasosController API (HU-33)")
class MetricasCasosControllerTest {

    private static final String URL = "/api/metricas/casos";
    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_USUARIO = "token-usuario";
    private static final String TOKEN_VOLUNTARIO = "token-voluntario";
    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricasCasosService metricasCasosService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_ADMIN)).thenReturn(1L);
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_USUARIO)).thenReturn(2L);
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_VOLUNTARIO)).thenReturn(3L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ADMIN")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "USUARIO")));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, "VOLUNTARIO")));
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

    private static MetricasCasosDTO metricas() {
        return new MetricasCasosDTO(DESDE, HASTA,
                new MetricasCasosDTO.Reportes(7,
                        List.of(new MetricasCasosDTO.Conteo("PENDIENTE_REVISION", 2),
                                new MetricasCasosDTO.Conteo("APROBADO", 4),
                                new MetricasCasosDTO.Conteo("RECHAZADO", 1)),
                        List.of(new MetricasCasosDTO.Conteo("PERDIDA", 5))),
                new MetricasCasosDTO.Casos(4,
                        List.of(new MetricasCasosDTO.Conteo("RESUELTO", 3)),
                        List.of(new MetricasCasosDTO.Conteo("PERDIDA", 4)),
                        List.of(new MetricasCasosDTO.Conteo("CENTRO", 2))));
    }

    // ---------------------------------------------------------------- acceso

    @Test
    @DisplayName("rechaza la consulta sin sesion iniciada (401)")
    void rechazaSinSesion() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isUnauthorized());

        verify(metricasCasosService, never()).calcular(any(), any());
    }

    @Test
    @DisplayName("rechaza la consulta de un usuario comun (403)")
    void rechazaUsuario() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden());

        verify(metricasCasosService, never()).calcular(any(), any());
    }

    @Test
    @DisplayName("rechaza la consulta de un voluntario (403)")
    void rechazaVoluntario() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + TOKEN_VOLUNTARIO)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isForbidden());

        verify(metricasCasosService, never()).calcular(any(), any());
    }

    // ---------------------------------------------------------------- consulta

    @Test
    @DisplayName("devuelve los indicadores de casos y reportes del periodo al administrador")
    void devuelveLasMetricas() throws Exception {
        when(metricasCasosService.calcular(DESDE, HASTA)).thenReturn(metricas());

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2026-10-01"))
                .andExpect(jsonPath("$.hasta").value("2026-10-31"))
                .andExpect(jsonPath("$.reportes.total").value(7))
                .andExpect(jsonPath("$.reportes.porEstado[1].clave").value("APROBADO"))
                .andExpect(jsonPath("$.reportes.porEstado[1].cantidad").value(4))
                .andExpect(jsonPath("$.reportes.porCategoria[0].clave").value("PERDIDA"))
                .andExpect(jsonPath("$.casos.total").value(4))
                .andExpect(jsonPath("$.casos.porEstado[0].clave").value("RESUELTO"))
                .andExpect(jsonPath("$.casos.porCategoria[0].cantidad").value(4))
                .andExpect(jsonPath("$.casos.porZona[0].clave").value("CENTRO"));
    }

    @Test
    @DisplayName("responde 400 con el motivo cuando el periodo no es valido")
    void periodoInvalido() throws Exception {
        when(metricasCasosService.calcular(HASTA, DESDE))
                .thenThrow(new IllegalArgumentException("La fecha inicial no puede ser posterior a la final"));

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .param("desde", "2026-10-31").param("hasta", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha inicial no puede ser posterior a la final"))
                .andExpect(jsonPath("$.errors.periodo").exists());
    }

    @Test
    @DisplayName("responde 400 cuando falta una fecha")
    void faltaUnaFecha() throws Exception {
        when(metricasCasosService.calcular(null, HASTA))
                .thenThrow(new IllegalArgumentException("Debes indicar la fecha inicial y la final del periodo"));

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .param("hasta", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debes indicar la fecha inicial y la final del periodo"));
    }
}
