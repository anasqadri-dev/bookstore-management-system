package com.power_puff.bms.activitylog.service;

import com.power_puff.bms.activitylog.entity.ActivityLog;
import com.power_puff.bms.activitylog.repository.ActivityLogRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    public void record(User user, String action, String details) {
        if (user != null) {
            activityLogRepository.save(new ActivityLog(user, action, details));
        }
    }

    /**
     * Records an event for the authenticated local application user.
     * Sale services can call this method after a successful sale transaction.
     */
    public void recordCurrentUserAction(String action, String details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        userRepository.findByUsernameIgnoreCase(authentication.getName())
                .ifPresent(user -> record(user, action, details));
    }
}
