package com.power_puff.bms.security;

import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void normalizesUsernameAndRetainsRoleAndActiveState() {
        User user = new User("  StoreAdmin  ", "encoded-password",
                "Store Administrator", UserRole.ADMIN);

        assertEquals("storeadmin", user.getUsername());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.isActive());
        assertEquals("encoded-password", user.getPasswordHash());
    }
}
