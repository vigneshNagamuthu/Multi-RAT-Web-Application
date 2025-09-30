package com.example.demo.config;

import com.example.demo.websocket.VideoHandler;
import com.example.demo.websocket.SensorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final VideoHandler videoHandler = new VideoHandler();
    private final SensorHandler sensorHandler = new SensorHandler();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(videoHandler, "/video").setAllowedOrigins("http://localhost:3000");
        registry.addHandler(sensorHandler, "/sensor").setAllowedOrigins("http://localhost:3000");
    }
}
