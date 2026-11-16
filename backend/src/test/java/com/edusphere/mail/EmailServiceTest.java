package com.edusphere.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.edusphere.config.AppProperties;
import com.edusphere.user.Role;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {
    JavaMailSender sender = mock(JavaMailSender.class);
    UserRepository users = mock(UserRepository.class);

    @SuppressWarnings("unchecked")
    private EmailService service(boolean enabled) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        AppProperties props = new AppProperties("http://app.test", "http://api.test", "./u", false, true,
                new AppProperties.Mail(enabled, "no-reply@edusphere.test", "EduSphere"), null);
        return new EmailService(provider, props, users, Runnable::run);
    }

    @BeforeEach void setUp() {
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private static User user(boolean notifications) {
        User u = new User();
        u.setId(7L); u.setEmail("ada@example.com"); u.setName("Ada Lovelace"); u.setEmailNotifications(notifications);
        return u;
    }

    private MimeMessage sent() {
        ArgumentCaptor<MimeMessage> c = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(c.capture());
        return c.getValue();
    }

    @Test void passwordResetContainsTheLinkAndGoesToTheUser() throws Exception {
        service(true).sendPasswordReset(user(true), "http://app.test/reset-password?token=abc");
        MimeMessage m = sent();
        assertThat(m.getAllRecipients()[0].toString()).isEqualTo("ada@example.com");
        assertThat(m.getSubject()).contains("Reset your EduSphere password");
        m.saveChanges();
        ByteArrayOutputStreamHelper.assertContains(m, "token=abc");
    }

    @Test void nothingIsSentWhenDisabled() {
        service(false).sendPasswordReset(user(true), "http://app.test/x");
        service(false).sendNotification(7L, "Hi", "msg", "/x");
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test void notificationEmailRespectsOptOut() {
        when(users.findById(7L)).thenReturn(Optional.of(user(false)));
        service(true).sendNotification(7L, "New quiz", "msg", "/student/subjects/1");
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test void notificationEmailIsSentWhenOptedIn() throws Exception {
        when(users.findById(7L)).thenReturn(Optional.of(user(true)));
        service(true).sendNotification(7L, "New quiz", "Open it", "/student/subjects/1");
        assertThat(sent().getSubject()).isEqualTo("EduSphere: New quiz");
    }

    @Test void userControlledTextIsHtmlEscaped() {
        var c = EmailTemplates.render("EduSphere", "<script>alert(1)</script>", "a <b>bold</b> & more", "Go", "http://x/?a=1&b=\"2\"", "f");
        assertThat(c.html()).doesNotContain("<script>").doesNotContain("<b>bold</b>");
        assertThat(c.html()).contains("&lt;script&gt;").contains("&amp;");
    }

    @Test void aSendFailureNeverEscapes() {
        doThrow(new org.springframework.mail.MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));
        service(true).sendWelcome(user(true)); // must not throw
        verify(sender).send(any(MimeMessage.class));
    }

    @Test void teacherWelcomeLinksToTheTeacherDashboard() {
        User u = user(true);
        com.edusphere.user.Role r = new Role(); r.setName(Role.TEACHER); u.getRoles().add(r);
        service(true).sendWelcome(u);
        verify(sender).send(any(MimeMessage.class));
    }

    /** Reads the rendered message source to assert on its content. */
    static final class ByteArrayOutputStreamHelper {
        static void assertContains(MimeMessage m, String needle) throws Exception {
            var out = new java.io.ByteArrayOutputStream();
            m.writeTo(out);
            assertThat(out.toString("UTF-8").replace("=\r\n", "").replace("=3D", "=")).contains(needle);
        }
    }
}
