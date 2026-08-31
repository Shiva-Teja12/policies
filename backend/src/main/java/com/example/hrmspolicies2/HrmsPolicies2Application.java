package com.example.hrmspolicies2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HrmsPolicies2Application {

    public static void main(
            String[] args
    ) {
        SpringApplication.run(
                HrmsPolicies2Application.class,
                args
        );
    }
}