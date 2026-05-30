package com.hogger.siliconbay.provider;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.hogger.siliconbay.mail.Mailable;
import com.hogger.siliconbay.util.Env;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;

public class MailServiceProvider {
    private ThreadPoolExecutor executor;
    private Authenticator authenticator;
    private final BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
    private final Properties properties = new Properties();
    private static MailServiceProvider mailServiceProvider;

    private MailServiceProvider() {
        properties.put("mail.smtp.auth", true);
        properties.put("mail.smtp.starttls.enable", true);
        properties.put("mail.smtp.starttls.required", true);
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        properties.put("mail.debug", true);
        properties.put("mail.smtp.host", Env.get("mail.host"));
        properties.put("mail.smtp.port", Env.get("mail.port"));
    }

    public static MailServiceProvider getInstance() {
        if (mailServiceProvider == null) {
            mailServiceProvider = new MailServiceProvider();
        }
        return mailServiceProvider;
    }

    public void start() {
        final String username = Env.get("mail.username");
        final String host = Env.get("mail.host");
        final String port = Env.get("mail.port");

        authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, Env.get("mail.password"));
            }
        };
        executor = new ThreadPoolExecutor(2, 5, 5,
                TimeUnit.SECONDS, blockingQueue, new ThreadPoolExecutor.AbortPolicy());
        executor.prestartCoreThread();

        String maskedUsername = username == null || username.length() < 4
                ? "***"
                : username.substring(0, 2) + "***" + username.substring(username.length() - 2);

        System.out.println("\u001B[32mEmailServiceProvider Initialized...\u001B[32m");
        System.out.println("SMTP host=" + host + ", port=" + port + ", user=" + maskedUsername);
    }

    public Properties getProperties() {
        return properties;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void sendMail(Mailable mailable) {
        if (mailable == null) {
            throw new IllegalArgumentException("Mail content can not be null");
        }
        if (executor == null) {
            start();
        }
        try {
            executor.submit(mailable).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Mail sending was interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Mail sending failed", e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Mail sending timed out or failed", e);
        }
    }
}
