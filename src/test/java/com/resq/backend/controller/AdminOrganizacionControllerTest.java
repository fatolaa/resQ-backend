package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.dto.OrganizacionAdminDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.exception.OrganizacionException;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.AdminOrganizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-25: la administracion de organizaciones es solo del administrador, con el filtro JWT real.
 */
@WebMvcTest(AdminOrganizacionController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("AdminOrganizacionController API (HU-25)")
class AdminOrganizacionControllerTest {

    private static final String URL = "/api/admin/organizaciones";
    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_USUARIO = "token-usuario";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOrganizacionService adminOrganizacionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_ADMIN)).thenReturn(1L);
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_USUARIO)).thenReturn(2L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ADMIN")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "USUARIO")));
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

    private static OrganizacionAdminDTO dto(long id, String estado) {
        return new OrganizacionAdminDTO(id, "Refugio Patitas", "REFUGIO", "Av. Heroínas 123", "70000000",
                "patitas@resq.com", null, null, null, null, false, estado, LocalDateTime.of(2026, 10, 1, 10, 0),
                20L, "Ana Refugio", "ana@resq.com", null, null, null);
    }

    // ---------------------------------------------------------------- acceso

    @Test
    @DisplayName("sin sesion responde 401")
    void sinSesion() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verify(adminOrganizacionService, never()).listar(any());
    }

    @Test
    @DisplayName("un usuario que no es administrador no puede listar ni decidir (403)")
    void usuarioNoAdmin() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(URL + "/7/aprobar").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isForbidden());

        verify(adminOrganizacionService, never()).listar(any());
        verify(adminOrganizacionService, never()).aprobar(anyLong(), anyLong());
    }

    // ---------------------------------------------------------------- consultar

    @Test
    @DisplayName("el administrador lista las organizaciones filtrando por estado")
    void listar() throws Exception {
        when(adminOrganizacionService.listar("PENDIENTE_VERIFICACION"))
                .thenReturn(List.of(dto(7, "PENDIENTE_VERIFICACION")));

        mockMvc.perform(get(URL).param("estado", "PENDIENTE_VERIFICACION")
                        .header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idOrganizacion").value(7))
                .andExpect(jsonPath("$[0].estadoVerificacion").value("PENDIENTE_VERIFICACION"))
                .andExpect(jsonPath("$[0].nombreRepresentante").value("Ana Refugio"));
    }

    @Test
    @DisplayName("el administrador ve el detalle de una organizacion")
    void detalle() throws Exception {
        when(adminOrganizacionService.detalle(7L)).thenReturn(dto(7, "PENDIENTE_VERIFICACION"));

        mockMvc.perform(get(URL + "/7").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patitas@resq.com"))
                .andExpect(jsonPath("$.emailRepresentante").value("ana@resq.com"));
    }

    @Test
    @DisplayName("un estado de filtro invalido responde 400 con el motivo")
    void estadoInvalido() throws Exception {
        when(adminOrganizacionService.listar("ACTIVA")).thenThrow(new OrganizacionException(
                HttpStatus.BAD_REQUEST, "Estado inválido: ACTIVA", Map.of("estado", "inválido")));

        mockMvc.perform(get(URL).param("estado", "ACTIVA").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Estado inválido: ACTIVA"));
    }

    // ---------------------------------------------------------------- decidir

    @Test
    @DisplayName("aprobar y rechazar registran el id del administrador del token")
    void aprobarYRechazar() throws Exception {
        when(adminOrganizacionService.aprobar(7L, 1L)).thenReturn(dto(7, "VERIFICADA"));
        when(adminOrganizacionService.rechazar(8L, 1L)).thenReturn(dto(8, "RECHAZADA"));

        mockMvc.perform(put(URL + "/7/aprobar").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value("VERIFICADA"));
        mockMvc.perform(put(URL + "/8/rechazar").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value("RECHAZADA"));

        verify(adminOrganizacionService).aprobar(7L, 1L);
        verify(adminOrganizacionService).rechazar(8L, 1L);
    }

    @Test
    @DisplayName("decidir sobre una organizacion ya revisada responde 409 (no 401)")
    void yaRevisada() throws Exception {
        when(adminOrganizacionService.aprobar(7L, 1L)).thenThrow(new OrganizacionException(
                HttpStatus.CONFLICT, "La organización ya fue revisada: no está pendiente de verificación", Map.of()));

        mockMvc.perform(put(URL + "/7/aprobar").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("una organizacion inexistente responde 404 (no 401)")
    void inexistente() throws Exception {
        when(adminOrganizacionService.detalle(99L)).thenThrow(new OrganizacionException(
                HttpStatus.NOT_FOUND, "La organización no existe", Map.of()));

        mockMvc.perform(get(URL + "/99").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isNotFound());
    }
}
