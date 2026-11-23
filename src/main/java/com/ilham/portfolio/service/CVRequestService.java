package com.ilham.portfolio.service;

import com.ilham.portfolio.dto.CVRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CVRequestService {

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String notificationEmail;

    public CVRequestService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail,
            @Value("${portfolio.cv.notification-email}") String notificationEmail) {

        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.notificationEmail = notificationEmail;
    }

    public void processRequest(CVRequest request) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setFrom(senderEmail);
        email.setTo(notificationEmail);
        email.setSubject("New CV Request - Portfolio");

        email.setText("""
                Someone has requested your CV.

                Name: %s
                Email: %s
                Company: %s

                Message:
                %s
                """.formatted(
                request.fullName(),
                request.email(),
                request.company() == null ? "" : request.company(),
                request.message() == null ? "" : request.message()
        ));

        email.setReplyTo(request.email());

        try {
            mailSender.send(email);
        } catch (MailException exception) {
            throw new CVEmailException(
                    "Unable to send CV request notification",
                    exception
            );
        }
    }
}
