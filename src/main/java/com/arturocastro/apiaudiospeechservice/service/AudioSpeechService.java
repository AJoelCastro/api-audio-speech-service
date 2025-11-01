package com.arturocastro.apiaudiospeechservice.service;

import com.arturocastro.apiaudiospeechservice.model.ASModel;
import com.openai.core.http.HttpResponse;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.audio.speech.SpeechCreateParams;
import com.openai.models.audio.speech.SpeechModel;
import com.openai.models.beta.realtime.ResponseAudioTranscriptDoneEvent;
import com.openai.models.realtime.RealtimeResponse;
import com.openai.models.realtime.RealtimeResponseCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputAudio;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

@Service
public class AudioSpeechService {

    private final OpenAIClient openAIClient;
    private final PeerConnectionFactory factory;
    public AudioSpeechService(@Value("${openai.api.key}") String apiKey) {
        openAIClient = new OpenAIOkHttpClient.Builder()
                .apiKey(apiKey)
                .build();
        factory = new PeerConnectionFactory();

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

}
