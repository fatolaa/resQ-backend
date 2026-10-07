package com.resq.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmailService: envío del correo de confirmación (HU-20)")
class EmailServiceTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> proveedorCon(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(sender);
        return proveedor;
    }

    @Test
    @DisplayName("con SMTP configurado envía el correo con remitente, destinatario, asunto y texto")
    void enviaConSmtpConfigurado() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService servicio = new EmailService(proveedorCon(sender), "smtp.ejemplo.com", "no-reply@resq.com");

        boolean enviado = servicio.enviar("org@correo.com", "Asunto", "Texto");

        assertThat(enviado).isTrue();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();
        assertThat(mensaje.getFrom()).isEqualTo("no-reply@resq.com");
        assertThat(mensaje.getTo()).containsExactly("org@correo.com");
        assertThat(mensaje.getSubject()).isEqualTo("Asunto");
        assertThat(mensaje.getText()).isEqualTo("Texto");
    }

    @Test
    @DisplayName("sin servidor SMTP configurado no envía nada y devuelve false")
    void sinSmtpNoEnvia() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService servicio = new EmailService(proveedorCon(sender), "", "no-reply@resq.com");

        assertThat(servicio.enviar("org@correo.com", "Asunto", "Texto")).isFalse();
        verify(sender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("si no existe el componente de correo devuelve false sin lanzar error")
    void sinComponenteDeCorreo() {
        EmailService servicio = new EmailService(proveedorCon(null), "smtp.ejemplo.com", "no-reply@resq.com");

        assertThat(servicio.enviar("org@correo.com", "Asunto", "Texto")).isFalse();
    }

    @Test
    @DisplayName("si el servidor SMTP falla devuelve false y no propaga la excepción")
    void fallaElServidor() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("conexión rechazada")).when(sender).send(any(SimpleMailMessage.class));
        EmailService servicio = new EmailService(proveedorCon(sender), "smtp.ejemplo.com", "no-reply@resq.com");

        assertThat(servicio.enviar("org@correo.com", "Asunto", "Texto")).isFalse();
    }
}
