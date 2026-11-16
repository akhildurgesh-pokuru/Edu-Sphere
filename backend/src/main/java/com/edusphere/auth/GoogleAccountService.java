package com.edusphere.auth;

import com.edusphere.common.ApiException;
import com.edusphere.domain.OAuthAccount;
import com.edusphere.repository.OAuthAccountRepository;
import com.edusphere.user.Role;
import com.edusphere.user.RoleRepository;
import com.edusphere.user.User;
import com.edusphere.user.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Separate bean so @Transactional applies. Google sign-in never creates a teacher. */
@Service
public class GoogleAccountService {
    private final OAuthAccountRepository accounts;
    private final UserRepository users;
    private final RoleRepository roles;

    public GoogleAccountService(OAuthAccountRepository accounts, UserRepository users, RoleRepository roles) {
        this.accounts = accounts; this.users = users; this.roles = roles;
    }

    @Transactional
    public User loginOrCreate(String subject, String email, boolean emailVerified, String name) {
        var existing = accounts.findByProviderAndProviderSubject("google", subject);
        if (existing.isPresent()) {
            return users.findById(existing.get().getUserId()).orElseThrow();
        }
        if (!emailVerified || email == null || email.isBlank()) {
            throw ApiException.forbidden("Your Google email is not verified");
        }
        String e = email.trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(e).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(e);
            user.setName(name == null || name.isBlank() ? e : name);
            user.getRoles().add(roles.findByName(Role.STUDENT).orElseThrow());
            user = users.save(user);
        } else if (user.getPasswordHash() != null) {
            // Local sign-up never verified the email: drop that password so it cannot be used to hijack the linked account.
            user.setPasswordHash(null);
            user.setUpdatedAt(Instant.now());
            users.save(user);
        }
        OAuthAccount link = new OAuthAccount();
        link.setUserId(user.getId());
        link.setProvider("google");
        link.setProviderSubject(subject);
        accounts.save(link);
        return user;
    }
}
