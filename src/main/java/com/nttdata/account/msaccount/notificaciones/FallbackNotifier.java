package com.nttdata.account.msaccount.notificaciones;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FallbackNotifier {

    private static final Logger logger = LoggerFactory.getLogger(FallbackNotifier.class);
    private final JavaMailSender mailSender;

    public void sendFallbackEmail(String source, Throwable throwable) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo("soporte@tubanco.com");
            helper.setSubject("Fallback activado desde " + source);
            String body = """
                    ️ Fallback activado en el microservicio de cuentas

                     Método afectado: newAccount
                     Origen: %s
                     Excepción: %s
                     Mensaje: %s
                     Fecha: %s
                    Este mensaje fue generado automáticamente.
                    """.formatted(
                    source,
                    throwable.getClass().getSimpleName(),
                    throwable.getMessage(),
                    LocalDateTime.now()
            );
            helper.setText(body, false);
            mailSender.send(message);
            logger.info("Correo de fallback enviado desde {}", source);
        } catch (MessagingException e) {
            logger.error("Error enviando correo de fallback: {}", e.getMessage());
        }
    }
}
