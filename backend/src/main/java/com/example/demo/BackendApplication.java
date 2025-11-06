package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.lang.NonNull;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.demo.controller",
    "com.example.demo.service",
    "com.example.demo.model",
    "com.example.demo.websocket"  // ✅ ADDED THIS - Required for WebSocket handlers
})
public class BackendApplication implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("\n🔍 Registered Spring Endpoints:");
        applicationContext.getBean(RequestMappingHandlerMapping.class)
            .getHandlerMethods()
            .forEach((key, value) -> System.out.println("👉 " + key));
        
        System.out.println("\n✅ WebSocket Endpoints Available:");
        System.out.println("   📹 ws://localhost:8080/ws/streaming (Port 6060 - LRTT)");
        System.out.println("   📡 ws://localhost:8080/ws/sensor (Port 5000 - Redundant)");
        System.out.println("\n🚀 Backend is ready!\n");
    }

    // ✅ Enable global CORS for frontend access
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")  // Changed from "/api/**" to allow WebSocket CORS
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173")  // Added Vite port
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}