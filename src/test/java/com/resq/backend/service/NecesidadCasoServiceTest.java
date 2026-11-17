package com.resq.backend.service;

import com.resq.backend.dto.NecesidadCasoDTO;
import com.resq.backend.entity.EstadoVerificacion;
import com.resq.backend.entity.NecesidadCaso;
import com.resq.backend.entity.Notificacion;
import com.resq.backend.entity.Organizacion;
import com.resq.backend.entity.Reporte;
import com.resq.backend.entity.Usuario;
import com.resq.backend.repository.NecesidadCasoRepository;
import com.resq.backend.repository.OrganizacionRepository;
import com.resq.backend.repository.ReporteRepository;
import com.resq.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("NecesidadCasoService: que necesita un caso (rediseño)")
class NecesidadCasoServiceTest {

    private static final long CASO = 7L;
    private static final long REPORTANTE = 2L;
    private static final long REPRESENTANTE = 20L;
    private static final long AYUDANTE = 9L;
    private static final long AJENO = 99L;

    private NecesidadCasoRepository necesidades;
    private ReporteRepository reportes;
    private UsuarioRepository usuarios;
    private OrganizacionRepository organizaciones;
    private NotificacionService notificaciones;
    private NecesidadCasoService servicio;
    private final List<NecesidadCaso> guardadas = new ArrayList<>();
    private Reporte reporte;

    @BeforeEach
    void setUp() {
        necesidades = mock(NecesidadCasoRepository.class);
        reportes = mock(ReporteRepository.class);
        usuarios = mock(UsuarioRepository.class);
        organizaciones = mock(OrganizacionRepository.class);
        notificaciones = mock(NotificacionService.class);
        servicio = new NecesidadCasoService(necesidades, reportes, usuarios, organizaciones, notificaciones);

        reporte = new Reporte();
        reporte.setIdReporte(CASO);
        reporte.setIdUsuario(REPORTANTE);
        reporte.setEstado("EN_PROCESO");
        reporte.setIdOrganizacion(3L);
        when(reportes.findById(CASO)).thenReturn(Optional.of(reporte));

        Organizacion org = new Organizacion();
        org.setIdOrganizacion(3L);
        org.setIdRepresentante(REPRESENTANTE);
        org.setEstadoVerificacion(EstadoVerificacion.VERIFICADA);
        when(organizaciones.findByIdRepresentante(anyLong())).thenReturn(Optional.empty());
        when(organizaciones.findByIdRepresentante(REPRESENTANTE)).thenReturn(Optional.of(org));
        when(organizaciones.findById(3L)).thenReturn(Optional.of(org));

        Usuario jorge = new Usuario();
        jorge.setIdUsuario(AYUDANTE);
        jorge.setNombre("Jorge Mamani");
        when(usuarios.findById(AYUDANTE)).thenReturn(Optional.of(jorge));
        when(usuarios.findAllById(anyIterable())).thenReturn(List.of(jorge));

        AtomicLong ids = new AtomicLong(100);
        when(necesidades.save(any(NecesidadCaso.class))).thenAnswer(inv -> {
            NecesidadCaso n = inv.getArgument(0);
            if (n.getIdNecesidad() == null) {
                n.setIdNecesidad(ids.incrementAndGet());
                guardadas.add(n);
            }
            return n;
        });
        when(necesidades.findByIdReporteOrderByIdNecesidadAsc(CASO)).thenAnswer(inv -> List.copyOf(guardadas));
        when(necesidades.findById(anyLong())).thenAnswer(inv ->
                guardadas.stream().filter(n -> n.getIdNecesidad().equals(inv.getArgument(0))).findFirst());
    }

    private NecesidadCaso una(String tipo, String estado) {
        servicio.crearIniciales(CASO, List.of(tipo));
        NecesidadCaso n = guardadas.get(guardadas.size() - 1);
        n.setEstado(estado);
        return n;
    }

    private static HttpStatus estado(Throwable e) {
        return HttpStatus.valueOf(((ResponseStatusException) e).getStatusCode().value());
    }

    @Test
    @DisplayName("valida el catalogo, normaliza y quita repetidos")
    void validaTipos() {
        assertThat(servicio.validarTipos(List.of("transporte", "VETERINARIO", "TRANSPORTE")))
                .containsExactly("TRANSPORTE", "VETERINARIO");
        assertThat(servicio.validarTipos(null)).isEmpty();
        assertThatThrownBy(() -> servicio.validarTipos(List.of("PASEO")))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("alguien con sesion se ofrece; se avisa a quien reporto y a la organizacion")
    void ofrecer() {
        NecesidadCaso transporte = una("TRANSPORTE", NecesidadCaso.BUSCADA);

        List<NecesidadCasoDTO> lista = servicio.ofrecer(CASO, transporte.getIdNecesidad(), AYUDANTE);

        assertThat(lista.get(0).estado()).isEqualTo(NecesidadCaso.OFRECIDA);
        assertThat(lista.get(0).nombreAyudante()).isEqualTo("Jorge");
        verify(notificaciones).crearNotificacion(eq(REPORTANTE), eq(CASO), isNull(),
                eq(Notificacion.NECESIDAD_OFRECIDA), contains("Jorge Mamani se ofreció para transporte"));
        verify(notificaciones).crearNotificacion(eq(REPRESENTANTE), eq(CASO), isNull(),
                eq(Notificacion.NECESIDAD_OFRECIDA), any());
    }

    @Test
    @DisplayName("quien reporto no se ofrece en su propio caso y no se ofrece dos veces")
    void ofrecerNoPermitido() {
        NecesidadCaso transporte = una("TRANSPORTE", NecesidadCaso.BUSCADA);
        assertThatThrownBy(() -> servicio.ofrecer(CASO, transporte.getIdNecesidad(), REPORTANTE))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.FORBIDDEN));

        transporte.setEstado(NecesidadCaso.OFRECIDA);
        assertThatThrownBy(() -> servicio.ofrecer(CASO, transporte.getIdNecesidad(), AJENO))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("quien reporto o la organizacion confirma; un ajeno no puede")
    void confirmar() {
        NecesidadCaso vet = una("VETERINARIO", NecesidadCaso.OFRECIDA);
        vet.setIdAyudante(AYUDANTE);

        assertThatThrownBy(() -> servicio.confirmar(CASO, vet.getIdNecesidad(), AJENO))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(servicio.confirmar(CASO, vet.getIdNecesidad(), REPRESENTANTE).get(0).estado())
                .isEqualTo(NecesidadCaso.CUBIERTA);
    }

    @Test
    @DisplayName("quien se ofrecio puede retirar su oferta y vuelve a buscarse")
    void liberarPropia() {
        NecesidadCaso hogar = una("HOGAR_TEMPORAL", NecesidadCaso.OFRECIDA);
        hogar.setIdAyudante(AYUDANTE);

        NecesidadCasoDTO dto = servicio.liberar(CASO, hogar.getIdNecesidad(), AYUDANTE).get(0);

        assertThat(dto.estado()).isEqualTo(NecesidadCaso.BUSCADA);
        assertThat(dto.idAyudante()).isNull();
    }

    @Test
    @DisplayName("agregar exige gestionar el caso y no repite tipos")
    void agregar() {
        when(necesidades.existsByIdReporteAndTipo(CASO, "ALIMENTO")).thenReturn(false, true);

        assertThatThrownBy(() -> servicio.agregar(CASO, "ALIMENTO", AJENO))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(servicio.agregar(CASO, "alimento", REPORTANTE)).extracting(NecesidadCasoDTO::tipo).contains("ALIMENTO");
        assertThatThrownBy(() -> servicio.agregar(CASO, "ALIMENTO", REPORTANTE))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("con el caso cerrado no se puede cambiar nada")
    void casoCerrado() {
        NecesidadCaso transporte = una("TRANSPORTE", NecesidadCaso.BUSCADA);
        reporte.setEstado("RESUELTO");

        assertThatThrownBy(() -> servicio.ofrecer(CASO, transporte.getIdNecesidad(), AYUDANTE))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.CONFLICT));
        verify(notificaciones, never()).crearNotificacion(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("una necesidad de otro caso responde 404")
    void necesidadDeOtroCaso() {
        NecesidadCaso ajena = una("RESCATE", NecesidadCaso.BUSCADA);
        ajena.setIdReporte(8L);

        assertThatThrownBy(() -> servicio.ofrecer(CASO, ajena.getIdNecesidad(), AYUDANTE))
                .satisfies(e -> assertThat(estado(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
