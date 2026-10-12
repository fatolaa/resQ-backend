package com.resq.backend.controller;

import com.resq.backend.service.FotoStorageService;
import com.resq.backend.service.FotoStorageService.FotoAlmacenada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/reportes/fotos")
public class FotoReporteController {

    private static final Logger logger = LoggerFactory.getLogger(FotoReporteController.class);

    private final FotoStorageService fotoStorageService;

    public FotoReporteController(FotoStorageService fotoStorageService) {
        this.fotoStorageService = fotoStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirFoto(@RequestParam("foto") MultipartFile foto) {
        logger.info("Subiendo foto: nombre original = {}, tamaño = {} bytes",
                foto.getOriginalFilename(), foto.getSize());
        String nombre = fotoStorageService.guardar(foto);
        logger.info("Foto almacenada con nombre: {}", nombre);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("fotoUrl", "/api/reportes/fotos/" + nombre));
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<Resource> verFoto(@PathVariable String nombre) {
        logger.info("Consultando foto: {}", nombre);
        return fotoStorageService.cargar(nombre)
                .map((FotoAlmacenada foto) -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.contentType()))
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .body(foto.recurso()))
                .orElseGet(() -> {
                    logger.warn("Foto no encontrada: {}", nombre);
                    return ResponseEntity.notFound().build();
                });
    }
}