package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.OrganizacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rediseño: organizaciones verificadas sin sesion, con el filtro JWT real. */
@WebMvcTest({OrganizacionPublicaController.class, OrganizacionController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("Organizaciones publicas (rediseño)")
class OrganizacionPublicaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizacionService organizacionService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UsuarioRepository usuarioRepository;

    private static Organizacion refugio() {
        Organizacion o = new Organizacion();
        o.setIdOrganizacion(3L);
        o.setIdRepresentante(20L);
        o.setNombre("Refugio Patitas");
        o.setTipo("REFUGIO");
        o.setTelefono("4449999");
        o.setEmail("patitas@resq.com");
        o.setLogoUrl("/api/organizaciones/logos/patitas.png");
        o.setZonasCobertura("Cercado");
        o.setEstadoVerificacion(EstadoVerificacion.VERIFICADA);
        return o;
    }

    @Test
    @DisplayName("sin sesion lista las verificadas solo con datos publicos")
    void listaSinSesion() throws Exception {
        when(organizacionService.consultarDirectorio(null, null)).thenReturn(List.of(refugio()));

        mockMvc.perform(get("/api/organizaciones/publicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Refugio Patitas"))
                .andExpect(jsonPath("$[0].logoUrl").value("/api/organizaciones/logos/patitas.png"))
                .andExpect(jsonPath("$[0].idRepresentante").doesNotExist())
                .andExpect(jsonPath("$[0].telefono").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @DisplayName("la ficha publica de una organizacion no verificada responde 404")
    void fichaNoVerificada() throws Exception {
        when(organizacionService.obtenerFicha(3L)).thenReturn(Optional.of(refugio()));
        when(organizacionService.obtenerFicha(4L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/organizaciones/publicas/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Refugio Patitas"));
        mockMvc.perform(get("/api/organizaciones/publicas/4")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el directorio completo y la ficha con contacto siguen pidiendo sesion")
    void directorioSigueConSesion() throws Exception {
        mockMvc.perform(get("/api/organizaciones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/organizaciones/3")).andExpect(status().isUnauthorized());
    }
}
