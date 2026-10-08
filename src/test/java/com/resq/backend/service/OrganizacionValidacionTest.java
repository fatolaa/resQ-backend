package com.resq.backend.service;

import com.resq.backend.dto.OrganizacionRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrganizacionRequestDTO: formato de email y teléfono (HU-20)")
class OrganizacionValidacionTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean emailEsValido(String email) {
        return validator.validate(conEmail(email)).stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    private boolean telefonoEsValido(String telefono) {
        return validator.validate(conTelefono(telefono)).stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("telefono"));
    }

    private static OrganizacionRequestDTO conEmail(String email) {
        return new OrganizacionRequestDTO(1L, "Refugio", "REFUGIO", "Calle 1", "70123456", email, null, null, null);
    }

    private static OrganizacionRequestDTO conTelefono(String telefono) {
        return new OrganizacionRequestDTO(1L, "Refugio", "REFUGIO", "Calle 1", telefono, "a@b.com", null, null, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"contacto@patitas.org", "ana.perez+rescate@mail.example.com", "a_b-c@sub.dominio.bo"})
    @DisplayName("acepta emails bien formados")
    void aceptaEmailsValidos(String email) {
        assertThat(emailEsValido(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba.com", "a@b", "a@b.c", "@dominio.com", "a@@dominio.com", "a b@dominio.com",
            "a@dominio..com", "a@.com", "a@dominio.com."})
    @DisplayName("rechaza emails mal formados")
    void rechazaEmailsInvalidos(String email) {
        assertThat(emailEsValido(email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"70123456", "+591 70123456", "(4) 4123456", "591-70-123-456", "+59144123456",
            "1234567", "123456789012345"})
    @DisplayName("acepta teléfonos con 7 a 15 dígitos y separadores comunes")
    void aceptaTelefonosValidos(String telefono) {
        assertThat(telefonoEsValido(telefono)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "1234567890123456", "abc1234567", "7012-345a", "591+70123456", "++59170123456",
            "70123456\n", "()-- ---"})
    @DisplayName("rechaza teléfonos con letras, símbolos raros o cantidad de dígitos fuera de rango")
    void rechazaTelefonosInvalidos(String telefono) {
        assertThat(telefonoEsValido(telefono)).isFalse();
    }
}
