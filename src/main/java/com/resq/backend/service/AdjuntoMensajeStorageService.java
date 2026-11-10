package com.resq.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AdjuntoMensajeStorageService {

    private static final Logger logger = LoggerFactory.getLogger(AdjuntoMensajeStorageService.class);

    private static final Pattern NOMBRE_VALIDO = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{1,10}$");

    private static final Set<String> EXTENSIONES_IMAGEN = Set.of("jpg", "jpeg", "png", "webp", "gif");

    public record AdjuntoGuardado(String nombre, String nombreOriginal, String extension) {
    }

    public record AdjuntoCargado(Resource recurso, String contentType, boolean imagen) {
    }

    private final Path directorio;
    private final long maxBytes;

    public AdjuntoMensajeStorageService(
            @Value("${resq.mensajes.directorio:uploads/mensajes}") String directorio,
            @Value("${resq.mensajes.max-bytes:5242880}") long maxBytes) {
        this.directorio = Paths.get(directorio).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public AdjuntoGuardado guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo esta vacio");
        }
        if (archivo.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El archivo supera el maximo permitido de " + (maxBytes / (1024 * 1024)) + " MB");
        }

        String original = archivo.getOriginalFilename();
        String extension = extensionDe(original);
        String nombre = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(directorio);
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, directorio.resolve(nombre));
            }
        } catch (IOException e) {
            logger.error("No se pudo guardar el adjunto en {}", directorio, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el archivo adjunto");
        }

        return new AdjuntoGuardado(nombre, original == null || original.isBlank() ? nombre : original, extension);
    }

    public Optional<AdjuntoCargado> cargar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path ruta = directorio.resolve(nombre).normalize();
        if (!ruta.startsWith(directorio) || !Files.isReadable(ruta)) {
            return Optional.empty();
        }
        String extension = extensionDe(nombre);
        boolean imagen = esImagen(extension);
        String contentType = contentTypeDe(extension);
        return Optional.of(new AdjuntoCargado(new PathResource(ruta), contentType, imagen));
    }

    public static boolean esImagen(String extension) {
        return EXTENSIONES_IMAGEN.contains(extension);
    }

    private static String extensionDe(String nombre) {
        if (nombre == null) {
            return "bin";
        }
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return "bin";
        }
        String extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,10}") ? extension : "bin";
    }

    private static String contentTypeDe(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }
}
