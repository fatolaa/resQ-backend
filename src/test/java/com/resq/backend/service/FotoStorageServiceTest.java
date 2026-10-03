package com.resq.backend.service;

import com.resq.backend.exception.FotoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FotoStorageService: adjuntar fotografía (HU-10)")
class FotoStorageServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0};

    @TempDir
    Path tmp;

    private FotoStorageService servicio(long maxBytes) {
        return new FotoStorageService(tmp.toString(), maxBytes);
    }

    @Test
    @DisplayName("acepta JPG, PNG y WEBP y los guarda con extensión según su contenido")
    void aceptaFormatosPermitidos() throws Exception {
        FotoStorageService s = servicio(1024);
        assertThat(s.guardar(new MockMultipartFile("foto", "a.jpg", "image/jpeg", JPEG))).endsWith(".jpg");
        assertThat(s.guardar(new MockMultipartFile("foto", "a.png", "image/png", PNG))).endsWith(".png");
        assertThat(s.guardar(new MockMultipartFile("foto", "a.webp", "image/webp", WEBP))).endsWith(".webp");
        try (var archivos = Files.list(tmp)) {
            assertThat(archivos.count()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("rechaza formatos no permitidos (415)")
    void rechazaFormatoNoPermitido() {
        FotoStorageService s = servicio(1024);
        assertThatThrownBy(() -> s.guardar(new MockMultipartFile("foto", "a.gif", "image/gif", GIF)))
                .isInstanceOfSatisfying(FotoInvalidaException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    @DisplayName("rechaza un archivo que finge ser imagen por su nombre y Content-Type")
    void rechazaArchivoDisfrazado() {
        FotoStorageService s = servicio(1024);
        byte[] texto = "<script>alert(1)</script>".getBytes();
        assertThatThrownBy(() -> s.guardar(new MockMultipartFile("foto", "virus.jpg", "image/jpeg", texto)))
                .isInstanceOfSatisfying(FotoInvalidaException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    @DisplayName("rechaza archivos que superan el tamaño máximo (413)")
    void rechazaTamanoExcedido() {
        FotoStorageService s = servicio(8);
        assertThatThrownBy(() -> s.guardar(new MockMultipartFile("foto", "a.jpg", "image/jpeg", JPEG)))
                .isInstanceOfSatisfying(FotoInvalidaException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    @DisplayName("rechaza archivo vacío (400)")
    void rechazaVacio() {
        FotoStorageService s = servicio(1024);
        assertThatThrownBy(() -> s.guardar(new MockMultipartFile("foto", "a.jpg", "image/jpeg", new byte[0])))
                .isInstanceOfSatisfying(FotoInvalidaException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("cargar devuelve la foto guardada y bloquea nombres con rutas")
    void cargarSoloNombresGenerados() {
        FotoStorageService s = servicio(1024);
        String nombre = s.guardar(new MockMultipartFile("foto", "a.png", "image/png", PNG));
        assertThat(s.cargar(nombre)).isPresent();
        assertThat(s.cargar(nombre).get().contentType()).isEqualTo("image/png");
        assertThat(s.cargar("../application.properties")).isEmpty();
        assertThat(s.cargar("..%2Fsecreto.jpg")).isEmpty();
        assertThat(s.cargar("inexistente.jpg")).isEmpty();
    }
}
