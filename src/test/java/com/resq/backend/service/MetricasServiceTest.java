package com.resq.backend.service;

import com.resq.backend.dto.MetricasDTO;
import com.resq.backend.entity.AccesoUsuario;
import com.resq.backend.entity.EstadoSolicitudVoluntario;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.SolicitudVoluntario;
import com.resq.backend.entity.TipoOrganizacion;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.AccesoUsuarioRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.SolicitudVoluntarioRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MetricasService: indicadores de usuarios, voluntarios y organizaciones (HU-34)")
class MetricasServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 7);
    private static final LocalDateTime INICIO = DESDE.atStartOfDay();
    private static final LocalDateTime FIN = HASTA.atTime(LocalTime.MAX);

    private UsuarioRepository usuarioRepository;
    private AccesoUsuarioRepository accesoUsuarioRepository;
    private SolicitudVoluntarioRepository solicitudVoluntarioRepository;
    private OrganizacionRepository organizacionRepository;
    private MetricasService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        accesoUsuarioRepository = mock(AccesoUsuarioRepository.class);
        solicitudVoluntarioRepository = mock(SolicitudVoluntarioRepository.class);
        organizacionRepository = mock(OrganizacionRepository.class);
        servicio = new MetricasService(usuarioRepository, accesoUsuarioRepository,
                solicitudVoluntarioRepository, organizacionRepository);

        // Por defecto la base esta vacia; cada prueba carga solo lo que necesita.
        when(usuarioRepository.findByFechaRegistroBetween(any(), any())).thenReturn(List.of());
        when(usuarioRepository.findByRolIgnoreCase(anyString())).thenReturn(List.of());
        when(accesoUsuarioRepository.findByFechaBetween(any(), any())).thenReturn(List.of());
        when(solicitudVoluntarioRepository.findByEstado(any())).thenReturn(List.of());
        when(organizacionRepository.findByFechaRegistroLessThanEqualOrFechaRegistroIsNull(any()))
                .thenReturn(List.of());
    }

    private static Usuario usuario(long id, LocalDateTime fechaRegistro) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setFechaRegistro(fechaRegistro);
        usuario.setRol("USUARIO");
        return usuario;
    }

    private static Usuario voluntario(long id, String tipoAyuda) {
        Usuario usuario = usuario(id, null);
        usuario.setRol("VOLUNTARIO");
        usuario.setTipoAyuda(tipoAyuda);
        return usuario;
    }

    private static SolicitudVoluntario aprobada(Usuario usuario, String tipoAyuda, LocalDateTime fechaResolucion) {
        SolicitudVoluntario solicitud = new SolicitudVoluntario();
        solicitud.setUsuario(usuario);
        solicitud.setTipoAyuda(tipoAyuda);
        solicitud.setEstado(EstadoSolicitudVoluntario.APROBADA);
        solicitud.setFechaResolucion(fechaResolucion);
        return solicitud;
    }

    private static Organizacion organizacion(String tipo, String estado, LocalDateTime fechaRegistro) {
        Organizacion organizacion = new Organizacion();
        organizacion.setTipo(tipo);
        organizacion.setEstadoVerificacion(estado);
        organizacion.setFechaRegistro(fechaRegistro);
        return organizacion;
    }

    private static LocalDateTime dia(int diaDeOctubre) {
        return LocalDateTime.of(2026, 10, diaDeOctubre, 12, 0);
    }

    // ---------------------------------------------------------------- periodo

    @Test
    @DisplayName("rechaza un periodo sin fechas")
    void rechazaPeriodoSinFechas() {
        assertThatThrownBy(() -> servicio.calcular(null, HASTA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha inicial y la final");
    }

    @Test
    @DisplayName("rechaza un periodo cuya fecha inicial es posterior a la final")
    void rechazaPeriodoInvertido() {
        assertThatThrownBy(() -> servicio.calcular(HASTA, DESDE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("rechaza un periodo de mas de dos anios")
    void rechazaPeriodoDemasiadoLargo() {
        LocalDate desde = LocalDate.of(2024, 1, 1);

        assertThatThrownBy(() -> servicio.calcular(desde, desde.plusDays(MetricasService.MAX_DIAS_PERIODO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("731");
    }

    @Test
    @DisplayName("acepta un periodo de un solo dia")
    void aceptaUnSoloDia() {
        MetricasDTO metricas = servicio.calcular(DESDE, DESDE);

        assertThat(metricas.evolucion()).hasSize(1);
    }

    @Test
    @DisplayName("agrupa la evolucion por dia, semana o mes segun lo largo del periodo")
    void eligeLaAgrupacion() {
        assertThat(MetricasService.agrupacionPara(DESDE, DESDE.plusDays(30)))
                .isEqualTo(MetricasService.Agrupacion.DIA);
        assertThat(MetricasService.agrupacionPara(DESDE, DESDE.plusDays(31)))
                .isEqualTo(MetricasService.Agrupacion.SEMANA);
        assertThat(MetricasService.agrupacionPara(DESDE, DESDE.plusDays(185)))
                .isEqualTo(MetricasService.Agrupacion.SEMANA);
        assertThat(MetricasService.agrupacionPara(DESDE, DESDE.plusDays(186)))
                .isEqualTo(MetricasService.Agrupacion.MES);
    }

    @Test
    @DisplayName("por mes, el primer tramo empieza en la fecha inicial y los demas el dia 1")
    void tramosMensuales() {
        List<LocalDate> inicios = MetricasService.iniciosDeTramo(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10), MetricasService.Agrupacion.MES);

        assertThat(inicios).containsExactly(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
    }

    // ---------------------------------------------------------------- usuarios

    @Test
    @DisplayName("cuenta los usuarios registrados en el periodo y el total al cierre")
    void cuentaRegistrados() {
        when(usuarioRepository.findByFechaRegistroBetween(INICIO, FIN))
                .thenReturn(List.of(usuario(1, dia(1)), usuario(2, dia(3)), usuario(3, dia(3))));
        when(usuarioRepository.countByFechaRegistroLessThanEqualOrFechaRegistroIsNull(FIN)).thenReturn(10L);

        MetricasDTO.Usuarios usuarios = servicio.calcular(DESDE, HASTA).usuarios();

        assertThat(usuarios.registrados()).isEqualTo(3);
        assertThat(usuarios.totalAlCierre()).isEqualTo(10);
    }

    @Test
    @DisplayName("un usuario que inicia sesion varias veces cuenta una sola vez como activo")
    void cuentaActivosSinRepetir() {
        when(accesoUsuarioRepository.findByFechaBetween(INICIO, FIN)).thenReturn(List.of(
                new AccesoUsuario(1L, dia(1)),
                new AccesoUsuario(1L, dia(2)),
                new AccesoUsuario(1L, dia(2)),
                new AccesoUsuario(2L, dia(5))));

        assertThat(servicio.calcular(DESDE, HASTA).usuarios().activos()).isEqualTo(2);
    }

    @Test
    @DisplayName("la evolucion reparte registros y usuarios activos por tramo")
    void evolucionPorDia() {
        when(usuarioRepository.findByFechaRegistroBetween(INICIO, FIN))
                .thenReturn(List.of(usuario(1, dia(1)), usuario(2, dia(3)), usuario(3, dia(3))));
        when(accesoUsuarioRepository.findByFechaBetween(INICIO, FIN)).thenReturn(List.of(
                new AccesoUsuario(1L, dia(3)),
                new AccesoUsuario(1L, dia(3)),
                new AccesoUsuario(2L, dia(3))));

        List<MetricasDTO.PuntoEvolucion> evolucion = servicio.calcular(DESDE, HASTA).evolucion();

        assertThat(evolucion).hasSize(7);
        assertThat(evolucion.get(0)).isEqualTo(new MetricasDTO.PuntoEvolucion(dia(1).toLocalDate(), 1, 0));
        assertThat(evolucion.get(2)).isEqualTo(new MetricasDTO.PuntoEvolucion(dia(3).toLocalDate(), 2, 2));
        assertThat(evolucion.get(6).registrados()).isZero();
    }

    // ---------------------------------------------------------------- voluntarios

    @Test
    @DisplayName("clasifica los voluntarios actuales por cada tipo de ayuda que ofrecen")
    void voluntariosActualesPorTipo() {
        when(usuarioRepository.findByRolIgnoreCase("VOLUNTARIO")).thenReturn(List.of(
                voluntario(1, "TRANSPORTE,ALIMENTO"),
                voluntario(2, "TRANSPORTE")));

        MetricasDTO.Voluntarios voluntarios = servicio.calcular(DESDE, HASTA).voluntarios();

        assertThat(voluntarios.actuales()).isEqualTo(2);
        assertThat(voluntarios.porTipoAyuda()).containsExactly(
                new MetricasDTO.VoluntariosPorTipo("TRANSPORTE", 0, 2),
                new MetricasDTO.VoluntariosPorTipo("HOGAR_TEMPORAL", 0, 0),
                new MetricasDTO.VoluntariosPorTipo("ALIMENTO", 0, 1),
                new MetricasDTO.VoluntariosPorTipo("RESCATE", 0, 0));
    }

    @Test
    @DisplayName("si el voluntario no tiene tipo propio, usa el de su ultima solicitud aprobada (HU-35)")
    void usaElTipoDeLaSolicitudAprobada() {
        Usuario aprobadoPorHu35 = voluntario(1, null);
        when(usuarioRepository.findByRolIgnoreCase("VOLUNTARIO")).thenReturn(List.of(aprobadoPorHu35));
        when(solicitudVoluntarioRepository.findByEstado(EstadoSolicitudVoluntario.APROBADA)).thenReturn(List.of(
                aprobada(aprobadoPorHu35, "RESCATE", dia(2)),
                aprobada(aprobadoPorHu35, "HOGAR_TEMPORAL", LocalDateTime.of(2026, 9, 1, 10, 0))));

        List<MetricasDTO.VoluntariosPorTipo> porTipo = servicio.calcular(DESDE, HASTA).voluntarios().porTipoAyuda();

        assertThat(porTipo).filteredOn(fila -> fila.actuales() > 0)
                .containsExactly(new MetricasDTO.VoluntariosPorTipo("RESCATE", 1, 1));
    }

    @Test
    @DisplayName("los nuevos voluntarios son las solicitudes aprobadas dentro del periodo")
    void nuevosVoluntarios() {
        Usuario dentro = voluntario(1, "ALIMENTO");
        Usuario fuera = voluntario(2, "ALIMENTO");
        when(solicitudVoluntarioRepository.findByEstado(EstadoSolicitudVoluntario.APROBADA)).thenReturn(List.of(
                aprobada(dentro, "ALIMENTO", dia(4)),
                aprobada(fuera, "ALIMENTO", LocalDateTime.of(2026, 9, 30, 23, 59))));

        MetricasDTO.Voluntarios voluntarios = servicio.calcular(DESDE, HASTA).voluntarios();

        assertThat(voluntarios.nuevos()).isEqualTo(1);
        assertThat(voluntarios.porTipoAyuda()).contains(new MetricasDTO.VoluntariosPorTipo("ALIMENTO", 1, 0));
    }

    @Test
    @DisplayName("un voluntario sin ningun tipo de ayuda queda en SIN_TIPO, al final")
    void voluntarioSinTipo() {
        when(usuarioRepository.findByRolIgnoreCase("VOLUNTARIO")).thenReturn(List.of(voluntario(1, " ")));

        List<MetricasDTO.VoluntariosPorTipo> porTipo = servicio.calcular(DESDE, HASTA).voluntarios().porTipoAyuda();

        assertThat(porTipo).last().isEqualTo(new MetricasDTO.VoluntariosPorTipo(MetricasService.SIN_TIPO, 0, 1));
    }

    @Test
    @DisplayName("separa los tipos de ayuda sin espacios, en mayusculas y sin repetir")
    void separaTiposDeAyuda() {
        assertThat(MetricasService.tiposDe(" transporte, ALIMENTO,transporte,")).containsExactly("TRANSPORTE", "ALIMENTO");
        assertThat(MetricasService.tiposDe(null)).containsExactly(MetricasService.SIN_TIPO);
        assertThat(MetricasService.tiposDe(" , ")).containsExactly(MetricasService.SIN_TIPO);
    }

    // ---------------------------------------------------------------- organizaciones

    @Test
    @DisplayName("agrupa las organizaciones por tipo y estado de verificacion")
    void organizacionesPorTipoYEstado() {
        when(organizacionRepository.findByFechaRegistroLessThanEqualOrFechaRegistroIsNull(FIN)).thenReturn(List.of(
                organizacion(TipoOrganizacion.REFUGIO, EstadoVerificacion.VERIFICADA, dia(2)),
                organizacion(TipoOrganizacion.REFUGIO, EstadoVerificacion.PENDIENTE, LocalDateTime.of(2026, 8, 1, 9, 0)),
                organizacion(TipoOrganizacion.VETERINARIA, EstadoVerificacion.RECHAZADA, null)));

        MetricasDTO.Organizaciones organizaciones = servicio.calcular(DESDE, HASTA).organizaciones();

        assertThat(organizaciones.nuevas()).isEqualTo(1);
        assertThat(organizaciones.totalAlCierre()).isEqualTo(3);
        assertThat(organizaciones.porTipo()).containsExactly(
                new MetricasDTO.OrganizacionesPorTipo(TipoOrganizacion.REFUGIO, 1, 1, 1, 0, 2),
                new MetricasDTO.OrganizacionesPorTipo(TipoOrganizacion.VETERINARIA, 0, 0, 0, 1, 1),
                new MetricasDTO.OrganizacionesPorTipo(TipoOrganizacion.RESCATISTA_INDEPENDIENTE, 0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("consulta las organizaciones existentes al cierre del periodo")
    void consultaAlCierre() {
        servicio.calcular(DESDE, HASTA);

        verify(organizacionRepository).findByFechaRegistroLessThanEqualOrFechaRegistroIsNull(FIN);
        verify(usuarioRepository).countByFechaRegistroLessThanEqualOrFechaRegistroIsNull(FIN);
    }
}
