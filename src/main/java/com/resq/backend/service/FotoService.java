package com.resq.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FotoService {

    @Value("${resq.upload.dir}")
    private String uploadDir;

    public String guardarFoto(MultipartFile foto) throws IOException {

        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("La fotografía está vacía");
        }

        String tipoContenido = foto.getContentType();

        if (tipoContenido == null || !tipoContenido.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        Path carpeta = Paths.get(uploadDir);
        Files.createDirectories(carpeta);

        String nombreOriginal = foto.getOriginalFilename();
        String extension = "";

        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        }

        String nombreArchivo = UUID.randomUUID() + extension;
        Path destino = carpeta.resolve(nombreArchivo);

        Files.copy(
                foto.getInputStream(),
                destino,
                StandardCopyOption.REPLACE_EXISTING
        );

        return nombreArchivo;
    }
}