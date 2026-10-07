package com.resq.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * HU-20: envío de correos.
 *
 * Sin servidor SMTP configurado (variable de entorno SPRING_MAIL_HOST) no se envía
 * nada: el mensaje solo se escribe en el log del backend. Si el envío falla, nunca
 * se propaga la excepción: el llamador decide qué hacer con el resultado.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String host;
    private final String remitente;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${spring.mail.host:}") String host,
                        @Value("${resq.mail.remitente:no-reply@resq.local}") String remitente) {
        this.mailSenderProvider = mailSenderProvider;
        this.host = host;
        this.remitente = remitente;
    }

    /** @return true solo si el correo se entregó al servidor SMTP. */
    public boolean enviar(String destinatario, String asunto, String texto) {
        JavaMailSender sender = host == null || host.isBlank() ? null : mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("[CORREO NO ENVIADO: SMTP sin configurar] para={} asunto={}\n{}", destinatario, asunto, texto);
            return false;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            sender.send(mensaje);
            return true;
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
            return false;
        }
    }
}
