package com.power_puff.bms;

import com.power_puff.bms.database.DatabaseLocationInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendBookstoreManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication application =
                new SpringApplication(BackendBookstoreManagementSystemApplication.class);
        application.addInitializers(new DatabaseLocationInitializer());
        application.run(args);
    }
}
