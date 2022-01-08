package com.example.gistcompetitioncnserver.verification;

import com.example.gistcompetitioncnserver.emailsender.EmailSender;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Component
public class EmailVerificationListener implements ApplicationListener<EmailVerificationEvent> {

    private final EmailSender mailSender;
    private final SpringTemplateEngine springTemplateEngine;

    public EmailVerificationListener(EmailSender mailSender, SpringTemplateEngine springTemplateEngine) {
        this.mailSender = mailSender;
        this.springTemplateEngine = springTemplateEngine;
    }

    @Override
    public void onApplicationEvent(EmailVerificationEvent event) {
        this.confirmRegistration(event);
    }

    private void confirmRegistration(EmailVerificationEvent event) {
        String mailTo = event.getEmail();
        String subject = "[지스트 청원] 회원 가입 인증 메일";
        String body = generateMailBody(event.getToken());

        mailSender.send(mailTo, subject, body);
    }

    private String generateMailBody(String token) {
        Context context = new Context();
        context.setVariable("token", token);
        return springTemplateEngine.process("email-verification2.html", context);
    }
}
