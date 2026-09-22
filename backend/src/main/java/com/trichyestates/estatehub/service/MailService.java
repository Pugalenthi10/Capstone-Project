package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends e-mail through Spring's JavaMailSender when SMTP is configured (spring.mail.host).
 * Without SMTP nothing is sent. In local development you can set MAIL_LOG_RESET_LINKS=true to print
 * the reset link to the console instead.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    public MailService(ObjectProvider<JavaMailSender> mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendPasswordReset(String toEmail, String name, String resetLink) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(props.mail().from());
                message.setTo(toEmail);
                message.setSubject("Reset your Trichy Estates password");
                message.setText("Hello " + name + ",\n\n"
                        + "We received a request to reset your password. Use the link below to choose a new one. "
                        + "It can be used once and expires shortly.\n\n" + resetLink + "\n\n"
                        + "If you did not request this, you can safely ignore this e-mail.");
                sender.send(message);
            } catch (MailException ex) {
                // Never surface delivery problems to the caller: that would reveal whether the account exists.
                log.error("Could not send password-reset e-mail", ex);
            }
            return;
        }
        if (props.mail().logResetLinks()) {
            log.info("[DEV ONLY] Password reset link for {}: {}", toEmail, resetLink);
        } else {
            log.warn("A password reset was requested but no SMTP server is configured, so no e-mail was sent. "
                    + "Set spring.mail.* (see README) to enable delivery.");
        }
    }
}
