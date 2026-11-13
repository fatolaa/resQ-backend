package com.resq.backend.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Telefonos: limpiar y validar el telefono de contacto")
class TelefonosTest {

    @Test
    @DisplayName("acepta espacios, guiones y parentesis y lo guarda limpio")
    void limpia() {
        assertThat(Telefonos.normalizar(" 712-345 67 ")).isEqualTo("71234567");
        assertThat(Telefonos.normalizar("+591 (4) 449-3355")).isEqualTo("+59144493355");
    }

    @Test
    @DisplayName("vacio o null es sin telefono")
    void vacio() {
        assertThat(Telefonos.normalizar(null)).isNull();
        assertThat(Telefonos.normalizar("   ")).isNull();
    }

    @Test
    @DisplayName("rechaza letras y numeros demasiado cortos o largos")
    void rechaza() {
        assertThatThrownBy(() -> Telefonos.normalizar("llamame")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Telefonos.normalizar("12345")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Telefonos.normalizar("1234567890123456")).isInstanceOf(IllegalArgumentException.class);
    }
}
