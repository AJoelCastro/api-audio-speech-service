package com.arturocastro.apiaudiospeechservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.http.Headers;
import com.openai.models.beta.realtime.sessions.Session;
import com.openai.models.beta.realtime.sessions.SessionCreateParams;
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

                SessionCreateParams sessionCreateParams = SessionCreateParams.builder()
                        .model(SessionCreateParams.Model.GPT_4O_MINI_REALTIME_PREVIEW)
                        .inputAudioFormat(SessionCreateParams.InputAudioFormat.PCM16)
                        .outputAudioFormat(SessionCreateParams.OutputAudioFormat.PCM16)
                        .additionalHeaders(
                                Headers.builder()
                                        .put("OpenAI-Beta", "realtime=v1")
                                        .put("Authorization", "Bearer " + apiKey)
                                        .build()
                        )
                        .addModality(SessionCreateParams.Modality.AUDIO)
                        .inputAudioNoiseReduction(
                                SessionCreateParams.InputAudioNoiseReduction.builder()
                                        .type(SessionCreateParams.InputAudioNoiseReduction.Type.FAR_FIELD)
                                        .build()
                        )
                        .clientSecret(SessionCreateParams.ClientSecret.builder()
                                .expiresAfter(SessionCreateParams.ClientSecret.ExpiresAfter.builder()
                                        .anchor(SessionCreateParams.ClientSecret.ExpiresAfter.Anchor.CREATED_AT)
                                        .seconds(20)
                                        .build()
                                )
                                .additionalProperties(new ConcurrentHashMap<>())
                                .build()
                        )
                        .build();
                System.out.println("sessionCreateParams: " + sessionCreateParams.clientSecret().get());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(sessionCreateParams)));
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