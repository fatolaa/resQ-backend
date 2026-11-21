package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.dto.NecesidadCasoDTO;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.NecesidadCasoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rediseño: endpoints de necesidades con el filtro JWT real. */
@WebMvcTest(NecesidadCasoController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("NecesidadCasoController API (rediseño)")
class NecesidadCasoControllerTest {

    private static final String TOKEN = "token-jorge";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NecesidadCasoService necesidadService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        Usuario jorge = new Usuario();
        jorge.setIdUsuario(9L);
        jorge.setNombre("Jorge");
        jorge.setEmail("jorge@resq.com");
        jorge.setPasswordHash("hash");
        jorge.setRol("USUARIO");
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN)).thenReturn(9L);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(jorge));
    }

    @Test
    @DisplayName("cualquiera ve las necesidades sin sesion")
    void listarPublico() throws Exception {
        when(necesidadService.listar(7L)).thenReturn(List.of(new NecesidadCasoDTO(1L, "TRANSPORTE", "CUBIERTA", 9L, "Jorge")));

        mockMvc.perform(get("/api/reportes/7/necesidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("TRANSPORTE"))
                .andExpect(jsonPath("$[0].nombreAyudante").value("Jorge"));
    }

    @Test
    @DisplayName("ofrecerse exige sesion y usa el id del token")
    void ofrecer() throws Exception {
        mockMvc.perform(post("/api/reportes/7/necesidades/1/ofrecer")).andExpect(status().isUnauthorized());
        verify(necesidadService, never()).ofrecer(anyLong(), anyLong(), anyLong());

        when(necesidadService.ofrecer(7L, 1L, 9L)).thenReturn(List.of(new NecesidadCasoDTO(1L, "TRANSPORTE", "OFRECIDA", 9L, "Jorge")));
        mockMvc.perform(post("/api/reportes/7/necesidades/1/ofrecer").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("OFRECIDA"));
    }

    @Test
    @DisplayName("agregar una necesidad responde 201; sin tipo responde 400")
    void agregar() throws Exception {
        when(necesidadService.agregar(7L, "ALIMENTO", 9L)).thenReturn(List.of());

        mockMvc.perform(post("/api/reportes/7/necesidades").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ALIMENTO\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reportes/7/necesidades").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
