package com.arturocastro.apiaudiospeechservice.controller;

import com.arturocastro.apiaudiospeechservice.service.AudioSpeechService;
import com.openai.core.http.HttpResponse;
import com.openai.models.responses.ResponseOutputItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.http.HttpClient;

@RestController
@RequestMapping("/api/openai/audio-speech")
public class AudioSpeechController {

    private final AudioSpeechService audioSpeechService;

    private AudioSpeechController(AudioSpeechService audioSpeechService) {
        this.audioSpeechService = audioSpeechService;
    }

    @PostMapping("/session")
    public ResponseEntity<HttpResponse> createSession(@RequestBody String clientSdp) throws Exception {
        return ResponseEntity.ok(audioSpeechService.callService(clientSdp));
    }

}
