package com.expenses.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String FROM_EMAIL = "noreply@myexpenses.com";
    private static final String FROM_PASSWORD = System.getenv("EMAIL_PASSWORD");
    
    public void sendRecoveryEmail(String toEmail, String recoveryLink) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("MyExpenses - Recuperação de Senha");
            
            String htmlContent = String.format("""
                <html>
                <body>
                    <h2>Recuperação de Senha - MyExpenses</h2>
                    <p>Você solicitou a recuperação de senha para sua conta no MyExpenses.</p>
                    <p>Clique no link abaixo para criar uma nova senha:</p>
                    <p><a href="%s">Recuperar Senha</a></p>
                    <p>Se você não solicitou a recuperação de senha, ignore este email.</p>
                    <p>O link expirará em 24 horas.</p>
                    <br>
                    <p>Atenciosamente,<br>Equipe MyExpenses</p>
                </body>
                </html>
                """, recoveryLink);
            
            message.setContent(htmlContent, "text/html; charset=utf-8");
            
            Transport.send(message);
            logger.info("Recovery email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Error sending recovery email", e);
            throw new RuntimeException("Error sending recovery email", e);
        }
    }
}
