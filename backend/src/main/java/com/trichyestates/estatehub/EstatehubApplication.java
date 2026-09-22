package com.trichyestates.estatehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EstatehubApplication {
    public static void main(String[] args) {
        SpringApplication.run(EstatehubApplication.class, args);
    }
}
