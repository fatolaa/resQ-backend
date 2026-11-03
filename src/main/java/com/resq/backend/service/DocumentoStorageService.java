package com.resq.backend.service;

import com.resq.backend.exception.OrganizacionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * HU-32: documento de verificacion de una organizacion (personeria juridica, licencia, etc.).
 *
 * A diferencia de los logos y las fotos de reportes, estos documentos NO son publicos: se
 * guardan en su propio directorio y solo los sirve el endpoint de administracion. El formato
 * se valida por la firma de bytes (no por la extension): PDF, JPG o PNG.
 */
@Service
public class DocumentoStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentoStorageService.class);

    public enum Formato {
        PDF("pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'}),
        JPEG("jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG("png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        private final String extension;
        private final String contentType;
        private final byte[] firma;

        Formato(String extension, String contentType, byte[] firma) {
            this.extension = extension;
            this.contentType = contentType;
            this.firma = firma;
        }

        public String getContentType() {
            return contentType;
        }

        static Optional<Formato> detectar(byte[] cabecera) {
            return Arrays.stream(values())
                    .filter(f -> cabecera.length >= f.firma.length
                            && Arrays.equals(Arrays.copyOf(cabecera, f.firma.length), f.firma))
                    .findFirst();
        }

        static Formato deNombre(String nombre) {
            return nombre.endsWith(".pdf") ? PDF : nombre.endsWith(".png") ? PNG : JPEG;
        }
    }

    public record DocumentoAlmacenado(Resource recurso, String contentType, String nombre) {
    }

    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|jpg|png)$");

    private final Path directorio;
    private final long maxBytes;

    public DocumentoStorageService(
            @Value("${resq.documentos.directorio:uploads/documentos}") String directorio,
            @Value("${resq.documentos.max-bytes:5242880}") long maxBytes) {
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    /**
     * Valida y guarda el documento. Devuelve el nombre de archivo generado.
     *
     * @throws OrganizacionException 413 si es muy grande, 415 si no es PDF, JPG ni PNG.
     */
    public String guardar(MultipartFile archivo) {
        if (archivo.getSize() > maxBytes) {
            logger.warn("Documento rechazado: {} bytes superan el maximo de {}", archivo.getSize(), maxBytes);
            String mensaje = "El documento supera el tamaño máximo de " + (maxBytes / (1024 * 1024)) + " MB";
            throw new OrganizacionException(HttpStatus.PAYLOAD_TOO_LARGE, mensaje, Map.of("documento", mensaje));
        }
        try {
            byte[] cabecera;
            try (InputStream in = archivo.getInputStream()) {
                cabecera = in.readNBytes(8);
            }
            Formato formato = Formato.detectar(cabecera).orElseThrow(() -> {
                logger.warn("Documento rechazado: la firma de bytes no es PDF, JPG ni PNG");
                String mensaje = "Formato no permitido. Sube un PDF, JPG o PNG";
                return new OrganizacionException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, mensaje,
                        Map.of("documento", mensaje));
            });

            Files.createDirectories(directorio);
            String nombre = UUID.randomUUID() + "." + formato.extension;
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, directorio.resolve(nombre));
            }
            logger.info("Documento de organizacion guardado como {} ({} bytes)", nombre, archivo.getSize());
            return nombre;
        } catch (IOException e) {
            logger.error("No se pudo guardar el documento en {}", directorio, e);
            throw new UncheckedIOException("No se pudo guardar el documento", e);
        }
    }

    /** El documento guardado, o vacio si el nombre no es valido o el archivo no existe. */
    public Optional<DocumentoAlmacenado> cargar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path ruta = directorio.resolve(nombre).normalize();
        if (!ruta.startsWith(directorio) || !Files.isReadable(ruta)) {
            return Optional.empty();
        }
        return Optional.of(new DocumentoAlmacenado(
                new PathResource(ruta), Formato.deNombre(nombre).getContentType(), nombre));
    }

    /** Borra el documento si existe; nunca toca archivos fuera de su directorio. */
    public void eliminar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return;
        }
        Path ruta = directorio.resolve(nombre).normalize();
        if (!ruta.startsWith(directorio)) {
            return;
        }
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException ignorado) {
            logger.warn("No se pudo borrar el documento {}: queda huerfano", nombre, ignorado);
        }
    }
}
