package com.resq.backend.service;

import com.resq.backend.exception.FotoInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * HU-10: guarda las fotografías de los reportes en disco.
 *
 * El formato se valida por el contenido real del archivo (firma de bytes) y no
 * por la extensión ni por el Content-Type que manda el cliente, porque ambos
 * se pueden falsear. El nombre guardado lo genera el servidor (UUID), así que
 * el nombre original del archivo nunca se usa para armar rutas.
 */
@Service
public class FotoStorageService {

    public enum Formato {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        private final String extension;
        private final String contentType;

        Formato(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public String getExtension() {
            return extension;
        }

        public String getContentType() {
            return contentType;
        }
    }

    public record FotoAlmacenada(Resource recurso, String contentType) {
    }

    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final Path directorio;
    private final long maxBytes;

    public FotoStorageService(
            @Value("${resq.fotos.directorio:uploads/reportes}") String directorio,
            @Value("${resq.fotos.max-bytes:5242880}") long maxBytes) {
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    /** Valida y guarda la foto. Devuelve el nombre de archivo generado. */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new FotoInvalidaException(HttpStatus.BAD_REQUEST, "Debes seleccionar una fotografía");
        }
        if (archivo.getSize() > maxBytes) {
            throw new FotoInvalidaException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "La fotografía supera el tamaño máximo permitido de " + (maxBytes / (1024 * 1024)) + " MB");
        }

        try {
            byte[] cabecera;
            try (InputStream in = archivo.getInputStream()) {
                cabecera = in.readNBytes(12);
            }
            Formato formato = detectarFormato(cabecera).orElseThrow(() ->
                    new FotoInvalidaException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "Formato no permitido. Usa una imagen JPG, PNG o WEBP"));

            Files.createDirectories(directorio);
            String nombre = UUID.randomUUID() + "." + formato.getExtension();
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, directorio.resolve(nombre));
            }
            return nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la fotografía", e);
        }
    }

    /** Devuelve la foto guardada, o vacío si el nombre es inválido o no existe. */
    public Optional<FotoAlmacenada> cargar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path ruta = directorio.resolve(nombre).normalize();
        if (!ruta.startsWith(directorio) || !Files.isReadable(ruta)) {
            return Optional.empty();
        }
        String contentType = nombre.endsWith(".png") ? Formato.PNG.getContentType()
                : nombre.endsWith(".webp") ? Formato.WEBP.getContentType()
                : Formato.JPEG.getContentType();
        return Optional.of(new FotoAlmacenada(new PathResource(ruta), contentType));
    }

    static Optional<Formato> detectarFormato(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of(Formato.JPEG);
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of(Formato.PNG);
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(Formato.WEBP);
        }
        return Optional.empty();
    }
}
