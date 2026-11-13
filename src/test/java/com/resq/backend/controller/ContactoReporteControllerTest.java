package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.dto.ContactoReporteDTO;
import com.resq.backend.dto.ContactoReporteDTO.Telefono;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.SeguimientoCasoRepository;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import com.resq.backend.service.ContactoReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contacto de quien reporta: nunca publico, solo con sesion. Con el filtro JWT real. */
@WebMvcTest({ContactoReporteController.class, ReporteController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("Contacto de quien reporta: API")
class ContactoReporteControllerTest {

    private static final String TOKEN = "token-ana";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactoReporteService contactoReporteService;
    @MockBean
    private ReporteRepository reporteRepository;
    @MockBean
    private OrganizacionRepository organizacionRepository;
    @MockBean
    private SeguimientoCasoRepository seguimientoCasoRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        Usuario ana = new Usuario();
        ana.setIdUsuario(9L);
        ana.setNombre("Ana");
        ana.setEmail("ana@resq.com");
        ana.setPasswordHash("hash");
        ana.setRol("USUARIO");
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN)).thenReturn(9L);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(ana));
    }

    private static Reporte reporteConContacto() {
        Reporte r = new Reporte();
        r.setIdReporte(7L);
        r.setIdUsuario(2L);
        r.setTipoCaso("PERDIDA");
        r.setDescripcion("Perro perdido");
        r.setEstado("PENDIENTE");
        r.setCompartirTelefonoCuenta(true);
        r.setTelefonoContacto("60011122");
        return r;
    }

    @Test
    @DisplayName("sin sesion el contacto responde 401")
    void sinSesion() throws Exception {
        mockMvc.perform(get("/api/reportes/7/contacto")).andExpect(status().isUnauthorized());
        verify(contactoReporteService, never()).contactoDe(anyLong(), anyLong());
    }

    @Test
    @DisplayName("con sesion devuelve nombre, insignia y telefonos")
    void conSesion() throws Exception {
        when(contactoReporteService.contactoDe(7L, 9L)).thenReturn(Optional.of(new ContactoReporteDTO(
                "Ana Quiroga", null, List.of(new Telefono("71234567", Telefono.CUENTA)))));

        mockMvc.perform(get("/api/reportes/7/contacto").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Quiroga"))
                .andExpect(jsonPath("$.telefonos[0].numero").value("71234567"))
                .andExpect(jsonPath("$.telefonos[0].origen").value("CUENTA"));
    }

    @Test
    @DisplayName("si no hay contacto compartido responde 204")
    void sinContacto() throws Exception {
        when(contactoReporteService.contactoDe(7L, 9L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reportes/7/contacto").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("el reporte publico no expone los telefonos, solo si tiene contacto")
    void reportePublicoSinTelefonos() throws Exception {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporteConContacto()));

        mockMvc.perform(get("/api/reportes/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneContacto").value(true))
                .andExpect(jsonPath("$.telefonoContacto").doesNotExist())
                .andExpect(jsonPath("$.compartirTelefonoCuenta").doesNotExist());
    }

    @Test
    @DisplayName("al crear un reporte se reciben los campos de contacto y se validan antes de guardar")
    void crearConContacto() throws Exception {
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/reportes").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idUsuario": 9, "tipoCaso": "PERDIDA", "descripcion": "Perro perdido",
                                 "estado": "PENDIENTE", "compartirTelefonoCuenta": true, "telefonoContacto": "600 111 22"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefonoContacto").doesNotExist());

        ArgumentCaptor<Reporte> captor = ArgumentCaptor.forClass(Reporte.class);
        verify(contactoReporteService).prepararParaGuardar(captor.capture());
        assertThat(captor.getValue().getCompartirTelefonoCuenta()).isTrue();
        assertThat(captor.getValue().getTelefonoContacto()).isEqualTo("600 111 22");
    }

    @Test
    @DisplayName("un telefono con letras se rechaza al crear (400)")
    void telefonoConLetras() throws Exception {
        mockMvc.perform(post("/api/reportes").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idUsuario": 9, "tipoCaso": "PERDIDA", "descripcion": "Perro perdido",
                                 "estado": "PENDIENTE", "telefonoContacto": "llamame"}
                                """))
                .andExpect(status().isBadRequest());
        verify(reporteRepository, never()).save(any());
    }
}
