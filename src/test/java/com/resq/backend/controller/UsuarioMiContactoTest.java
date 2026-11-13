package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PATCH /api/usuarios/me: cada usuario corrige su nombre y telefono. Con el filtro JWT real. */
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("UsuarioController: editar mi nombre y telefono")
class UsuarioMiContactoTest {

    private static final String TOKEN = "token-luis";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioRepository usuarioRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private PasswordEncoder passwordEncoder;

    private Usuario luis;

    @BeforeEach
    void setUp() {
        luis = new Usuario();
        luis.setIdUsuario(2L);
        luis.setNombre("Luis");
        luis.setEmail("luis@resq.com");
        luis.setPasswordHash("hash");
        luis.setTelefono("70000000");
        luis.setRol("USUARIO");
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN)).thenReturn(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(luis));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("sin sesion responde 401")
    void sinSesion() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me").contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"71234567\"}"))
                .andExpect(status().isUnauthorized());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("un usuario comun cambia su propio nombre y telefono (se guarda limpio)")
    void cambiaSuContacto() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Luis Meneses \",\"telefono\":\"7 123-4567\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis Meneses"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getTelefono()).isEqualTo("71234567");
        assertThat(captor.getValue().getRol()).isEqualTo("USUARIO");
    }

    @Test
    @DisplayName("un telefono vacio lo quita")
    void quitaTelefono() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"\"}"))
                .andExpect(status().isOk());

        assertThat(luis.getTelefono()).isNull();
    }

    @Test
    @DisplayName("un telefono invalido o un nombre vacio responden 400")
    void datosInvalidos() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/usuarios/me").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar la cuenta de otro sigue siendo solo de administrador (403)")
    void otraCuentaSigueSiendoAdmin() throws Exception {
        mockMvc.perform(patch("/api/usuarios/3").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"71234567\"}"))
                .andExpect(status().isForbidden());
    }
}
