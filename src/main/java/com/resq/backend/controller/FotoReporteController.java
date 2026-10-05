package com.resq.backend.controller;

import com.resq.backend.service.FotoStorageService;
import com.resq.backend.service.FotoStorageService.FotoAlmacenada;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HU-10: subir y consultar la fotografía de un reporte.
 *
 * Flujo: el frontend sube la foto (POST), recibe su fotoUrl y luego crea el
 * reporte con esa fotoUrl, que es el campo que ya existe en la entidad Reporte.
 */
@RestController
@RequestMapping("/api/reportes/fotos")
public class FotoReporteController {

    private final FotoStorageService fotoStorageService;

    public FotoReporteController(FotoStorageService fotoStorageService) {
        this.fotoStorageService = fotoStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirFoto(@RequestParam("foto") MultipartFile foto) {
        String nombre = fotoStorageService.guardar(foto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("fotoUrl", "/api/reportes/fotos/" + nombre));
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<Resource> verFoto(@PathVariable String nombre) {
        return fotoStorageService.cargar(nombre)
                .map((FotoAlmacenada foto) -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.contentType()))
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .body(foto.recurso()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
