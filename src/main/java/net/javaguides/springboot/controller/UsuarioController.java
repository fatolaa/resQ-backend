package net.javaguides.springboot.controller;

import net.javaguides.springboot.model.Usuario;
import net.javaguides.springboot.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Obtener todos los usuarios
    @GetMapping
    public List<Usuario> obtenerUsuarios() {
        return usuarioRepository.findAll();
    }

    @GetMapping("/{id}")
public Usuario obtenerUsuarioPorId(@PathVariable Long id) {
    return usuarioRepository.findById(id).orElse(null);
}

// Crear un nuevo usuario
@PostMapping
public Usuario crearUsuario(@RequestBody Usuario usuario) {
    return usuarioRepository.save(usuario);
}

// Actualizar un usuario
@PutMapping("/{id}")
public Usuario actualizarUsuario(@PathVariable Long id, @RequestBody Usuario datosUsuario) {

    Usuario usuario = usuarioRepository.findById(id).orElse(null);

    if (usuario != null) {
        usuario.setNombre(datosUsuario.getNombre());
        usuario.setEmail(datosUsuario.getEmail());
        usuario.setPasswordHash(datosUsuario.getPasswordHash());
        usuario.setTelefono(datosUsuario.getTelefono());
        usuario.setRol(datosUsuario.getRol());

        return usuarioRepository.save(usuario);
    }

    return null;
}

// Eliminar un usuario
@DeleteMapping("/{id}")
public void eliminarUsuario(@PathVariable Long id) {
    usuarioRepository.deleteById(id);
}

}

