package com.arturocastro.apiaudiospeechservice.service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AudioSpeechService {
    private final OpenAIClient openAIClient;

    public AudioSpeechService(@Value("${openai.api.key}") String apiKey) {
        openAIClient = new OpenAIOkHttpClient.Builder()
                .apiKey(apiKey)
                .build();
    }

    // method here to create live session call
    // public

}
