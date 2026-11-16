package com.edusphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        String backendUrl,
        String storagePath,
        boolean googleEnabled,
        boolean allowTeacherSignup,
        Mail mail,
        BootstrapTeacher bootstrapTeacher) {

    public record Mail(boolean enabled, String from, String fromName) {}

    public record BootstrapTeacher(String email, String password, String name) {}
}
