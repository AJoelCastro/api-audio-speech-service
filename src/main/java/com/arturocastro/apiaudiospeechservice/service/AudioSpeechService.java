package com.arturocastro.apiaudiospeechservice.service;

import com.arturocastro.apiaudiospeechservice.model.ASModel;
import com.openai.core.http.HttpResponse;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.speech.SpeechCreateParams;
import com.openai.models.audio.speech.SpeechModel;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;
import com.openai.models.audio.transcriptions.TranscriptionInclude;
import com.openai.models.realtime.*;
import com.openai.models.responses.ResponseComputerToolCall;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputAudio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class AudioSpeechService {

    private final OpenAIClient openAIClient;
    private final WebClient webClient;

    public AudioSpeechService(@Value("${openai.api.key}") String apiKey) {
        openAIClient = new OpenAIOkHttpClient.Builder()
                .apiKey(apiKey)
                .build();
        webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public HttpResponse speechService (ASModel asm){
        SpeechCreateParams speech = SpeechCreateParams.builder()
                .model(SpeechModel.GPT_4O_MINI_TTS)
                .input(asm.getInput())
                .instructions("Eres un buen orador, ten presente tambien el como expresarte de acuerdo al contexto")
                .voice(SpeechCreateParams.Voice.ALLOY)
                .build();
        return openAIClient.audio().speech().create(speech);
    }

    public TranscriptionCreateResponse textServiceWithRoute(ASModel asm){
        TranscriptionCreateParams transcriptionCreateParams = TranscriptionCreateParams.builder()
                .model(AudioModel.GPT_4O_TRANSCRIBE)
                .addInclude(
                        TranscriptionInclude.LOGPROBS
                )
                .file(
                        Path.of(asm.getPath())
                )
                .build();
        return openAIClient.audio().transcriptions().create(transcriptionCreateParams);
    }

    public TranscriptionCreateResponse textServiceWithBytes(){

        return openAIClient.audio().transcriptions().create();
    }

    public Mono<String> createSession(){
        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-realtime");
        body.put("voice", "verse");
        body.put("modalities", Arrays.asList("text", "audio"));
        body.put("instructions", "Eres un asistente peruano que ayuda en la pronunciaon en ingles a sus usuarios");
        return webClient
                .post()
                .uri("/realtime/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> Mono.just("Error: " + error.getMessage()));
    }


}
