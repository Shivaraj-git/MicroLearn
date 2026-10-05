package com.lms.identity.admin;

import com.lms.identity.common.ApiException;
import com.lms.identity.user.Role;
import com.lms.identity.user.User;
import com.lms.identity.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    public record UserView(Long id, String name, String email, Role role, boolean enabled) {
        static UserView of(User u) {
            return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isEnabled());
        }
    }

    public record UserPage(List<UserView> items, int page, int size, long totalItems, int totalPages) {}

    private final UserRepository users;

    public AdminUserController(UserRepository users) { this.users = users; }

    /** GET /api/admin/users?q=shiva&page=0&size=20   (all parameters optional) */
    @GetMapping
    public UserPage list(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));

        Page<User> result = (q == null || q.isBlank())
                ? users.findAll(pageRequest)
                : users.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(q.trim(), q.trim(), pageRequest);

        return new UserPage(result.getContent().stream().map(UserView::of).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/{id}/disable")
    @Transactional
    public UserView disable(@PathVariable Long id, Authentication auth) {
        if (String.valueOf(id).equals(auth.getName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
        }
        User u = find(id);
        u.disable();
        return UserView.of(u);
    }

    @PostMapping("/{id}/enable")
    @Transactional
    public UserView enable(@PathVariable Long id) {
        User u = find(id);
        u.enable();
        return UserView.of(u);
    }

    private User find(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
