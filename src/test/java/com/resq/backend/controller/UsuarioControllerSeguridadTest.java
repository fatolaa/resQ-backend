package com.resq.backend.controller;

import com.resq.backend.config.SecurityConfig;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.UsuarioRepository;
import com.resq.backend.security.JwtService;
import com.resq.backend.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-23: comprueba que las funciones administrativas de usuarios solo funcionan para
 * un administrador, y que las operaciones publicas y propias siguen abiertas.
 *
 * A diferencia de los otros tests HTTP del proyecto, aqui los filtros de seguridad
 * NO se desactivan: forman parte de lo que se quiere verificar.
 */
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@DisplayName("UsuarioController API: acceso exclusivo del administrador (HU-23)")
class UsuarioControllerSeguridadTest {

    private static final String TOKEN_ADMIN = "token-admin";
    private static final String TOKEN_USUARIO = "token-usuario";
    private static final String TOKEN_VOLUNTARIO = "token-voluntario";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_ADMIN)).thenReturn(1L);
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_USUARIO)).thenReturn(2L);
        when(jwtService.extraerIdUsuarioSiEsValido(TOKEN_VOLUNTARIO)).thenReturn(3L);
        when(jwtService.extraerIdUsuarioSiEsValido("token-vencido")).thenReturn(null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "Ana Admin", "admin@resq.co", "ADMIN")));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "Luis Usuario", "luis@resq.co", "USUARIO")));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, "Sara Voluntaria", "sara@resq.co", "VOLUNTARIO")));
    }

    private static Usuario usuario(Long id, String nombre, String email, String rol) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPasswordHash("hash");
        usuario.setTelefono("70000000");
        usuario.setRol(rol);
        return usuario;
    }

    private static String cuerpoUsuario(String rol) {
        return """
                {
                  "nombre": "Luis Usuario",
                  "email": "luis@resq.co",
                  "password": "clave123",
                  "telefono": "70000000",
                  "rol": "%s"
                }
                """.formatted(rol);
    }

    // ============ Listar usuarios: solo el administrador ============

    @Test
    @DisplayName("rechaza el listado de usuarios si no hay sesion iniciada")
    void debeRechazarElListadoDeUsuariosSinSesion() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rechaza el listado de usuarios a un usuario que no es administrador")
    void debeRechazarElListadoDeUsuariosAUnNoAdministrador() throws Exception {
        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rechaza el listado de usuarios cuando el token esta vencido o es invalido")
    void debeRechazarElListadoDeUsuariosConTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer token-vencido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("permite el listado de usuarios al administrador")
    void debePermitirElListadoDeUsuariosAlAdministrador() throws Exception {
        when(usuarioRepository.findAll()).thenReturn(List.of(
                usuario(1L, "Ana Admin", "admin@resq.co", "ADMIN"),
                usuario(2L, "Luis Usuario", "luis@resq.co", "USUARIO")));

        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].rol").value("ADMIN"));
    }

    // ============ Modificar y eliminar cuentas: solo el administrador ============

    @Test
    @DisplayName("rechaza que un usuario normal modifique otra cuenta")
    void debeRechazarQueUnUsuarioNormalModifiqueOtraCuenta() throws Exception {
        mockMvc.perform(put("/api/usuarios/2")
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoUsuario("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rechaza que un voluntario elimine una cuenta")
    void debeRechazarQueUnVoluntarioElimineUnaCuenta() throws Exception {
        mockMvc.perform(delete("/api/usuarios/2").header("Authorization", "Bearer " + TOKEN_VOLUNTARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rechaza el borrado parcial si no hay sesion iniciada")
    void debeRechazarActualizacionParcialSinSesion() throws Exception {
        mockMvc.perform(patch("/api/usuarios/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Nombre cambiado" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("permite al administrador cambiar el rol de una cuenta")
    void debePermitirAlAdministradorCambiarElRol() throws Exception {
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(patch("/api/usuarios/2")
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rol": "VOLUNTARIO" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("VOLUNTARIO"));
    }

    @Test
    @DisplayName("rechaza que el administrador escriba un rol que no existe")
    void debeRechazarUnRolInexistente() throws Exception {
        mockMvc.perform(patch("/api/usuarios/2")
                        .header("Authorization", "Bearer " + TOKEN_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rol": "SUPERUSUARIO" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("permite al administrador eliminar una cuenta")
    void debePermitirAlAdministradorEliminarUnaCuenta() throws Exception {
        when(usuarioRepository.existsById(2L)).thenReturn(true);

        mockMvc.perform(delete("/api/usuarios/2").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("impide que el administrador elimine su propia cuenta")
    void debeImpedirQueElAdministradorElimineSuPropiaCuenta() throws Exception {
        // TOKEN_ADMIN pertenece al id 1. Si esto se permitiera, el panel quedaria
        // sin ningun administrador y ya no habria forma de volver a entrar.
        mockMvc.perform(delete("/api/usuarios/1").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ============ Ver una cuenta concreta: el propietario o el administrador ============

    @Test
    @DisplayName("permite a un usuario consultar su propia cuenta")
    void debePermitirConsultarLaPropiaCuenta() throws Exception {
        mockMvc.perform(get("/api/usuarios/2").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("luis@resq.co"));
    }

    @Test
    @DisplayName("impide que un usuario consulte la cuenta de otra persona")
    void debeImpedirConsultarLaCuentaDeOtraPersona() throws Exception {
        mockMvc.perform(get("/api/usuarios/3").header("Authorization", "Bearer " + TOKEN_USUARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("permite al administrador consultar cualquier cuenta")
    void debePermitirAlAdministradorConsultarCualquierCuenta() throws Exception {
        mockMvc.perform(get("/api/usuarios/2").header("Authorization", "Bearer " + TOKEN_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(2));
    }

    // ============ El alta de cuentas sigue siendo publica ============

    @Test
    @DisplayName("mantiene abierto el registro publico de usuarios")
    void debeMantenerAbiertoElRegistroPublico() throws Exception {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoUsuario("USUARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("USUARIO"));
    }

    @Test
    @DisplayName("impide que alguien se registre a si mismo como administrador")
    void debeImpedirAutoAscensoEnElRegistro() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoUsuario("ADMIN")))
                .andExpect(status().isForbidden());
    }

    // ============ HU-17: el registro como voluntario sigue siendo propio ============

    @Test
    @DisplayName("permite a un usuario registrarse como voluntario en su propia cuenta")
    void debePermitirRegistrarseComoVoluntarioEnSuPropiaCuenta() throws Exception {
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        mockMvc.perform(patch("/api/usuarios/2/voluntario")
                        .header("Authorization", "Bearer " + TOKEN_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "tiposAyuda": ["TRANSPORTE"] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("VOLUNTARIO"));
    }

    @Test
    @DisplayName("impide volleyarse en la cuenta de otra persona como voluntario")
    void debeImpedirVoluntariarseEnLaCuentaDeOtraPersona() throws Exception {
        mockMvc.perform(patch("/api/usuarios/2/voluntario")
                        .header("Authorization", "Bearer " + TOKEN_VOLUNTARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "tiposAyuda": ["TRANSPORTE"] }
                                """))
                .andExpect(status().isForbidden());
    }
}
