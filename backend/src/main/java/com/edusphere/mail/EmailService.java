package com.edusphere.mail;

import com.edusphere.config.AppProperties;
import com.edusphere.mail.EmailTemplates.Content;
import com.edusphere.user.Role;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends transactional emails in the background. Failures are logged and never reach the caller, and emails
 * for a database change are only sent once that change has been committed. With app.mail.enabled=false
 * (the default) nothing is sent and only the subject and recipient are logged.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String FOOTER = "You are receiving this because you have an EduSphere account.";

    private final ObjectProvider<JavaMailSender> sender;
    private final AppProperties props;
    private final UserRepository users;
    private final Executor executor;

    public EmailService(ObjectProvider<JavaMailSender> sender, AppProperties props, UserRepository users,
                        @Qualifier("mailExecutor") Executor executor) {
        this.sender = sender; this.props = props; this.users = users; this.executor = executor;
    }

    public boolean enabled() { return props.mail() != null && props.mail().enabled(); }

    public void sendWelcome(User u) {
        boolean teacher = Role.TEACHER.equals(u.primaryRole());
        String body = teacher
                ? "Your teacher account is ready. Create a course, add lessons, quizzes and assignments, then publish it for students."
                : "Your student account is ready. Browse the catalog, enroll in a course and start learning.";
        String path = teacher ? "/teacher/dashboard" : "/student/dashboard";
        deliver(u.getEmail(), "Welcome to EduSphere",
                EmailTemplates.render(brand(), "Welcome, " + firstName(u.getName()) + "!", body, "Open EduSphere", props.frontendUrl() + path, FOOTER));
    }

    public void sendPasswordReset(User u, String link) {
        deliver(u.getEmail(), "Reset your EduSphere password",
                EmailTemplates.render(brand(), "Reset your password",
                        "We received a request to reset your password. This link works once and expires in 1 hour.\n\nIf you did not ask for this, you can ignore this email; your password stays the same.",
                        "Choose a new password", link, FOOTER));
    }

    public void sendPasswordChanged(User u) {
        deliver(u.getEmail(), "Your EduSphere password was changed",
                EmailTemplates.render(brand(), "Your password was changed",
                        "The password for your account was just changed. If this was you, no action is needed.\n\nIf it was not you, reset your password right away.",
                        "Reset password", props.frontendUrl() + "/forgot-password", FOOTER));
    }

    public void sendTeacherInvitation(String to, String inviterName, String link) {
        deliver(to, "You are invited to teach on EduSphere",
                EmailTemplates.render(brand(), "Teacher invitation",
                        inviterName + " invited you to join EduSphere as a teacher. The link is valid for 7 days and only works with this email address.",
                        "Accept invitation", link, FOOTER));
    }

    /** Emails the same text as an in-app notification, unless the user turned email notifications off. */
    public void sendNotification(Long userId, String title, String message, String link) {
        if (!enabled()) return;
        afterCommit(() -> executor.execute(() -> {
            try {
                User u = users.findById(userId).orElse(null);
                if (u == null || !u.isEnabled() || !u.isEmailNotifications()) return;
                String url = link == null ? props.frontendUrl() : props.frontendUrl() + (link.startsWith("/") ? link : "/" + link);
                String footer = FOOTER + " You can turn these emails off in your profile: " + props.frontendUrl() + "/profile";
                send(u.getEmail(), "EduSphere: " + title,
                        EmailTemplates.render(brand(), title, message == null ? "" : message, "Open in EduSphere", url, footer));
            } catch (Exception e) {
                log.warn("Could not prepare notification email for user {}: {}", userId, e.toString());
            }
        }));
    }

    private void deliver(String to, String subject, Content c) {
        if (!enabled()) { log.info("Email disabled; would send \"{}\" to {}", subject, to); return; }
        afterCommit(() -> executor.execute(() -> send(to, subject, c)));
    }

    private void send(String to, String subject, Content c) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) { log.warn("No mail sender configured; skipped \"{}\"", subject); return; }
        try {
            MimeMessage m = s.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(m, true, "UTF-8");
            h.setFrom(new InternetAddress(props.mail().from(), brand(), "UTF-8"));
            h.setTo(to);
            h.setSubject(subject);
            h.setText(c.text(), c.html());
            s.send(m);
            log.info("Email sent: \"{}\"", subject);
        } catch (Exception e) {
            log.warn("Email \"{}\" could not be sent: {}", subject, e.toString());
        }
    }

    private static void afterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { r.run(); }
            });
        } else {
            r.run();
        }
    }

    private String brand() {
        String n = props.mail() == null ? null : props.mail().fromName();
        return n == null || n.isBlank() ? "EduSphere" : n;
    }

    private static String firstName(String name) { return name == null ? "" : name.trim().split("\\s+")[0]; }
}
