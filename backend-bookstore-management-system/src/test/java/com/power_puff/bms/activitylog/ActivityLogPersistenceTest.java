package com.power_puff.bms.activitylog;

import com.power_puff.bms.activitylog.entity.ActivityLog;
import com.power_puff.bms.activitylog.repository.ActivityLogRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import com.power_puff.bms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ActivityLogPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Test
    void persistsActorActionAndCreationTimestamp() {
        User user = userRepository.saveAndFlush(
                new User("audit-tester", "already-encoded", "Audit Tester", UserRole.STAFF));

        ActivityLog saved = activityLogRepository.saveAndFlush(
                new ActivityLog(user, "LOGIN", "Successful test login"));

        ActivityLog reloaded = activityLogRepository.findById(saved.getId()).orElseThrow();

        assertEquals(user.getId(), reloaded.getUser().getId());
        assertEquals("LOGIN", reloaded.getAction());
        assertNotNull(reloaded.getCreatedAt());
    }
}
