package com.power_puff.bms.security;

import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Revalidates database-backed principals on every request. Sessions created
 * before account deactivation, role change, or password change are revoked.
 */
public final class UserSessionValidationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public UserSessionValidationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() instanceof BmsUserPrincipal principal
                && !isCurrentAccount(principal)) {
            revokeSession(request);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isCurrentAccount(BmsUserPrincipal principal) {
        try {
            return userRepository.findById(principal.getUserId())
                    .filter(User::isActive)
                    .filter(user -> user.getUsername().equals(principal.getUsername()))
                    .filter(user -> user.getRole().name().equals(principal.getRole()))
                    .filter(user -> principal.matchesCurrentPasswordHash(user.getPasswordHash()))
                    .isPresent();
        } catch (RuntimeException databaseFailure) {
            // Fail closed if the current account state cannot be verified.
            return false;
        }
    }

    private void revokeSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // The session may already have expired concurrently.
            }
        }
    }
}
