package com.power_puff.bms.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks authenticated HTTP sessions created by this application instance.
 * A successful database restore invalidates every tracked session so that
 * principals from the previous database snapshot cannot remain authenticated.
 */
@Component
public class ActiveSessionRegistry {

    private final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

    public void register(HttpSession session) {
        if (session != null) {
            sessions.put(session.getId(), session);
        }
    }

    public void unregister(HttpSession session) {
        if (session != null) {
            sessions.remove(session.getId());
        }
    }

    public int invalidateAll() {
        int invalidated = 0;
        for (Map.Entry<String, HttpSession> entry : sessions.entrySet()) {
            if (!sessions.remove(entry.getKey(), entry.getValue())) {
                continue;
            }
            try {
                entry.getValue().invalidate();
                invalidated++;
            } catch (IllegalStateException ignored) {
                // Expired sessions are already safe and need no further action.
            }
        }
        return invalidated;
    }
}
