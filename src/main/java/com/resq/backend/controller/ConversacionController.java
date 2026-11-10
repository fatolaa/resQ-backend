package com.resq.backend.controller;

import com.resq.backend.dto.AbrirConversacionRequestDTO;
import com.resq.backend.dto.ApiError;
import com.resq.backend.security.UsuarioAutenticado;
import com.resq.backend.service.AdjuntoMensajeStorageService;
import com.resq.backend.service.AdjuntoMensajeStorageService.AdjuntoCargado;
import com.resq.backend.service.ConversacionService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/conversaciones")
public class ConversacionController {

    private final ConversacionService conversacionService;
    private final AdjuntoMensajeStorageService adjuntoStorage;

    public ConversacionController(ConversacionService conversacionService,
                                  AdjuntoMensajeStorageService adjuntoStorage) {
        this.conversacionService = conversacionService;
        this.adjuntoStorage = adjuntoStorage;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        return ResponseEntity.ok(conversacionService.obtenerConversaciones(idUsuario.get()));
    }

    @PostMapping
    public ResponseEntity<?> abrir(@RequestBody(required = false) AbrirConversacionRequestDTO datos) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        return ResponseEntity.ok(conversacionService.abrirConversacion(idUsuario.get(), datos));
    }

    @GetMapping("/{idConversacion}/mensajes")
    public ResponseEntity<?> mensajes(@PathVariable Long idConversacion) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        return ResponseEntity.ok(conversacionService.obtenerMensajes(idUsuario.get(), idConversacion));
    }

    @PostMapping(value = "/{idConversacion}/mensajes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> enviar(@PathVariable Long idConversacion,
                                    @RequestParam(value = "contenido", required = false) String contenido,
                                    @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        Optional<Long> idUsuario = UsuarioAutenticado.idActual();
        if (idUsuario.isEmpty()) {
            return sinSesion();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversacionService.enviarMensaje(idUsuario.get(), idConversacion, contenido, archivo));
    }

    @GetMapping("/adjuntos/{nombre}")
    public ResponseEntity<Resource> adjunto(@PathVariable String nombre) {
        return adjuntoStorage.cargar(nombre)
                .map((AdjuntoCargado adjunto) -> {
                    ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(adjunto.contentType()))
                            .header("X-Content-Type-Options", "nosniff")
                            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate());
                    if (!adjunto.imagen()) {
                        respuesta.header(HttpHeaders.CONTENT_DISPOSITION, "attachment");
                    }
                    return respuesta.body(adjunto.recurso());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<ApiError> sinSesion() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(),
                        "Debes iniciar sesion para usar los mensajes", null));
    }
}
