package com.arturocastro.apiaudiospeechservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> openAISessions;
    private final String OPENAI_REALTIME_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01";
    private final String apiKey;

    public RealtimeWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.openAISessions = new ConcurrentHashMap<>();
        this.apiKey = System.getenv("OPENAI_API_KEY");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession clientSession) throws Exception {
        System.out.println("Cliente conectado: " + clientSession.getId());

        // Conectar a OpenAI Realtime API
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketHandler openAIHandler = new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                System.out.println("Conectado a OpenAI Realtime");
                openAISessions.put(clientSession.getId(), session);

                // Enviar configuración inicial de la sesión
                String sessionConfig = """
                {
                    "type": "session.update",
                    "session": {
                        "modalities": ["text", "audio"],
                        "instructions": "Eres un asistente útil que habla español",
                        "voice": "alloy",
                        "input_audio_format": "pcm16",
                        "output_audio_format": "pcm16",
                        "input_audio_transcription": {
                            "model": "whisper-1"
                        },
                        "turn_detection": {
                            "type": "server_vad",
                            "threshold": 0.5,
                            "prefix_padding_ms": 300,
                            "silence_duration_ms": 500
                        }
                    }
                }
                """;
                session.sendMessage(new TextMessage(sessionConfig));
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                // Reenviar mensajes de OpenAI al cliente
                if (clientSession.isOpen()) {
                    clientSession.sendMessage(message);
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                System.err.println("Error en conexión con OpenAI: " + exception.getMessage());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                System.out.println("Desconectado de OpenAI");
                openAISessions.remove(clientSession.getId());
                if (clientSession.isOpen()) {
                    clientSession.close();
                }
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        };

        // Conectar con headers de autenticación
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + apiKey);
        headers.add("OpenAI-Beta", "realtime=v1");

        client.execute(openAIHandler, headers, URI.create(OPENAI_REALTIME_URL));
    }

    @Override
    public void handleMessage(WebSocketSession clientSession, WebSocketMessage<?> message) throws Exception {
        // Obtener la sesión de OpenAI correspondiente
        WebSocketSession openAISession = openAISessions.get(clientSession.getId());

        if (openAISession != null && openAISession.isOpen()) {
            // Reenviar el mensaje del cliente a OpenAI
            openAISession.sendMessage(message);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("Error en cliente: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession clientSession, CloseStatus closeStatus) throws Exception {
        System.out.println("Cliente desconectado: " + clientSession.getId());

        // Cerrar conexión con OpenAI
        WebSocketSession openAISession = openAISessions.remove(clientSession.getId());
        if (openAISession != null && openAISession.isOpen()) {
            openAISession.close();
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}