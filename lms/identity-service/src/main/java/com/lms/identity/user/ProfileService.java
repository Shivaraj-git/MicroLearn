package com.lms.identity.user;

import com.lms.common.event.UserRenamedEvent;
import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import com.lms.identity.common.ApiException;
import com.lms.identity.outbox.OutboxWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final OutboxWriter outbox;

    public ProfileService(UserRepository users, PasswordEncoder encoder, OutboxWriter outbox) {
        this.users = users;
        this.encoder = encoder;
        this.outbox = outbox;
    }

    /** Saves the new name and, in the same transaction, the "user.renamed" message. */
    @Transactional
    public User rename(Long userId, String name) {
        User u = find(userId);
        u.rename(name);
        outbox.add(Exchanges.EVENTS, RoutingKeys.USER_RENAMED, new UserRenamedEvent(u.getId(), u.getEmail(), name));
        return u;
    }

    @Transactional
    public void changePassword(Long userId, String current, String next) {
        User u = find(userId);
        if (!encoder.matches(current, u.getPasswordHash())) {
            // 400, not 401: a 401 would sign the person out in the frontend
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        u.setPasswordHash(encoder.encode(next));
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
