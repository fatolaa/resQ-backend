package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.entity.AccesoUsuario;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.AccesoUsuarioRepository;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-34: cada login exitoso queda registrado para contar usuarios activos.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("AuthController: registro de accesos para metricas (HU-34)")
class AuthControllerAccesoTest {

    private static final String CREDENCIALES = """
            { "email": "luis@resq.com", "password": "Clave123" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AccesoUsuarioRepository accesoUsuarioRepository;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setNombre("Luis");
        usuario.setEmail("luis@resq.com");
        usuario.setPasswordHash("hash");
        usuario.setRol("USUARIO");

        when(usuarioRepository.findByEmail("luis@resq.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken(usuario)).thenReturn("token");
    }

    @Test
    @DisplayName("un login exitoso guarda el acceso del usuario")
    void registraElAcceso() throws Exception {
        when(passwordEncoder.matches("Clave123", "hash")).thenReturn(true);

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(CREDENCIALES))
                .andExpect(status().isOk());

        ArgumentCaptor<AccesoUsuario> acceso = ArgumentCaptor.forClass(AccesoUsuario.class);
        verify(accesoUsuarioRepository).save(acceso.capture());
        assertThat(acceso.getValue().getIdUsuario()).isEqualTo(7L);
        assertThat(acceso.getValue().getFecha()).isNotNull();
    }

    @Test
    @DisplayName("un login fallido no cuenta como acceso")
    void noRegistraLoginFallido() throws Exception {
        when(passwordEncoder.matches("Clave123", "hash")).thenReturn(false);

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(CREDENCIALES))
                .andExpect(status().isUnauthorized());

        verify(accesoUsuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("si no se puede guardar el acceso, el login igual funciona")
    void elLoginNoDependeDeLaMetrica() throws Exception {
        when(passwordEncoder.matches("Clave123", "hash")).thenReturn(true);
        when(accesoUsuarioRepository.save(any())).thenThrow(new DataAccessResourceFailureException("sin base"));

        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(CREDENCIALES))
                .andExpect(status().isOk());
    }
}
