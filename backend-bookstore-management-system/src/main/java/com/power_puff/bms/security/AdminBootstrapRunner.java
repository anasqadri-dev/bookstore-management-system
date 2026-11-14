package com.power_puff.bms.security;

import com.power_puff.bms.common.exception.DuplicateResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Optional operator-assisted bootstrap retained for development and recovery.
 * It uses the same atomic provisioning service as the owner-facing setup flow.
 */
@Component
@ConditionalOnProperty(prefix = "bms.bootstrap-admin", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminProvisioningService adminProvisioningService;

    @Value("${bms.bootstrap-admin.username:}")
    private String username;

    @Value("${bms.bootstrap-admin.password:}")
    private String password;

    @Value("${bms.bootstrap-admin.full-name:Store Administrator}")
    private String fullName;

    public AdminBootstrapRunner(AdminProvisioningService adminProvisioningService) {
        this.adminProvisioningService = adminProvisioningService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            adminProvisioningService.provisionInitialAdmin(username, password, fullName);
            log.info("Initial Admin account created by the explicit operator bootstrap.");
        } catch (DuplicateResourceException alreadyInitialized) {
            log.info("Operator bootstrap skipped because initial setup is already complete.");
        }
    }
}
