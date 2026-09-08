package com.resq.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> credentials) {
        return Map.of("message", "Login exitoso de " + credentials.get("username"), "status", "200 OK");
    }
}