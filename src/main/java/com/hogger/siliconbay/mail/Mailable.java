package com.hogger.siliconbay.mail;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.hogger.siliconbay.provider.MailServiceProvider;
import com.hogger.siliconbay.util.Env;

import io.rocketbase.mail.EmailTemplateBuilder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public abstract class Mailable implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Mailable.class.getName());
    private final MailServiceProvider mailServiceProvider;
    private final EmailTemplateBuilder.EmailTemplateConfigBuilder emailTemplateConfigBuilder;

    public Mailable() {
        this.mailServiceProvider = MailServiceProvider.getInstance();
        this.emailTemplateConfigBuilder = EmailTemplateBuilder.builder();
    }

    @Override
    public void run() {
        try {
            Session mailSession = Session.getInstance(mailServiceProvider.getProperties(), mailServiceProvider.getAuthenticator());
            MimeMessage mimeMessage = new MimeMessage(mailSession);
            mimeMessage.setFrom(new InternetAddress(Env.get("app.mail")));
            build(mimeMessage);
            if (mimeMessage.getRecipients(Message.RecipientType.TO).length > 0) {
                Transport.send(mimeMessage);
                System.out.println("\u001B[32mEmail sending successful...\u001B[32m");
            } else {
                throw new RuntimeException("Email recipients can not be empty...");
            }
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Email sending failed: " + e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unexpected runtime error while building/sending email: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public abstract void build(Message message) throws MessagingException;

    public EmailTemplateBuilder.EmailTemplateConfigBuilder getEmailTemplateBuilder() {
        return emailTemplateConfigBuilder;
    }
}
