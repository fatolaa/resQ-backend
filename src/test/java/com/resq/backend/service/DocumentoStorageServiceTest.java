package com.resq.backend.service;

import com.resq.backend.exception.OrganizacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DocumentoStorageService: documento de verificacion de organizaciones (HU-32)")
class DocumentoStorageServiceTest {

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0, 0};

    @TempDir
    Path carpeta;

    private DocumentoStorageService servicio;

    @BeforeEach
    void setUp() {
        servicio = new DocumentoStorageService(carpeta.toString(), 1024);
    }

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("documento", nombre, "application/octet-stream", contenido);
    }

    @Test
    @DisplayName("guarda PDF, JPG y PNG con la extension de su formato real")
    void guardaFormatosPermitidos() {
        String pdf = servicio.guardar(archivo("personeria.pdf", PDF));
        String png = servicio.guardar(archivo("licencia.png", PNG));
        String jpg = servicio.guardar(archivo("licencia.jpeg", JPG));

        assertThat(pdf).endsWith(".pdf");
        assertThat(png).endsWith(".png");
        assertThat(jpg).endsWith(".jpg");
        assertThat(carpeta.resolve(pdf)).exists();
    }

    @Test
    @DisplayName("el formato se decide por los bytes, no por la extension")
    void rechazaFormatoNoPermitido() {
        assertThatThrownBy(() -> servicio.guardar(archivo("falso.pdf", GIF)))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus())
                        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    @DisplayName("rechaza un documento mas grande que el maximo (413)")
    void rechazaDocumentoGrande() {
        byte[] grande = new byte[2048];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        assertThatThrownBy(() -> servicio.guardar(archivo("grande.pdf", grande)))
                .isInstanceOf(OrganizacionException.class)
                .satisfies(e -> assertThat(((OrganizacionException) e).getStatus())
                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    @DisplayName("carga el documento con su tipo de contenido")
    void cargaDocumento() {
        String nombre = servicio.guardar(archivo("personeria.pdf", PDF));

        assertThat(servicio.cargar(nombre)).hasValueSatisfying(d -> {
            assertThat(d.contentType()).isEqualTo("application/pdf");
            assertThat(d.nombre()).isEqualTo(nombre);
        });
    }

    @Test
    @DisplayName("no sirve archivos con nombres invalidos ni fuera de su directorio")
    void nombresInvalidos() throws Exception {
        Files.writeString(carpeta.resolve("secreto.txt"), "no");

        assertThat(servicio.cargar(null)).isEmpty();
        assertThat(servicio.cargar("secreto.txt")).isEmpty();
        assertThat(servicio.cargar("../application.properties")).isEmpty();
    }

    @Test
    @DisplayName("eliminar borra el documento y tolera un nombre nulo")
    void eliminar() {
        String nombre = servicio.guardar(archivo("personeria.pdf", PDF));

        servicio.eliminar(nombre);
        servicio.eliminar(null);

        assertThat(carpeta.resolve(nombre)).doesNotExist();
    }
}
