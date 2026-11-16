package com.edusphere.auth;

import com.edusphere.config.AppProperties;
import com.edusphere.user.Role;
import com.edusphere.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapTeacherRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapTeacherRunner.class);
    private final AppProperties props;
    private final UserRepository users;
    private final AuthService auth;

    public BootstrapTeacherRunner(AppProperties props, UserRepository users, AuthService auth) {
        this.props = props; this.users = users; this.auth = auth;
    }

    @Override
    public void run(ApplicationArguments args) {
        var b = props.bootstrapTeacher();
        if (b == null || b.email() == null || b.email().isBlank() || b.password() == null || b.password().isBlank()) return;
        if (users.existsByEmailIgnoreCase(b.email())) return;
        auth.create(b.name() == null || b.name().isBlank() ? "Teacher" : b.name(), b.email(), b.password(), Role.TEACHER);
        log.info("Bootstrap teacher account created for {}", b.email());
    }
}
