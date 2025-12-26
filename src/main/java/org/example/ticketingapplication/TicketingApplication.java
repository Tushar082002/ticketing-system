package org.example.ticketingapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot Application Class
 *
 * Starts the Distributed Ticketing Management System
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.example.ticketingapplication")
public class TicketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketingApplication.class, args);
    }

}
