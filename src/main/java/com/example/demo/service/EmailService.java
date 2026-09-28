package com.example.demo.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



import java.security.SecureRandom;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${gmail.address}")
    private String myEmail;

    @Value("${gmail.app-password}")
    private String appPassword;

    private final SecureRandom random = new SecureRandom();

    // Generate 6-digit verification code
    public String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    // Send verification code
    public void sendVerificationCode(
            String recipientEmail,
            String verificationCode
    ) {

        Properties props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(
                props,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                myEmail,
                                appPassword);
                    }
                }
        );

        try {

            Message message = new MimeMessage(session);

            message.setFrom(
                    new InternetAddress(myEmail)
            );

            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
            );

            message.setSubject(
                    "MyGaming - Email Verification Code"
            );

            String emailBody =
                    "Hello,\n\n" +
                            "Welcome to MyGaming!\n\n" +
                            "Your verification code is:\n\n" +
                            verificationCode +
                            "\n\n" +
                            "Please enter this code to verify your email.\n\n" +
                            "If you did not create a MyGaming account, " +
                            "please ignore this email.\n\n" +
                            "MyGaming Team";

            message.setText(emailBody);

            Transport.send(message);

            System.out.println(
                    "Verification code sent successfully to: "
                            + recipientEmail
            );

        } catch (MessagingException e) {

            System.err.println(
                    "Failed to send verification email: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Failed to send verification email.",
                    e
            );
        }
    }
}