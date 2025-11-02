package com.arturocastro.apiaudiospeechservice.controller;

import com.arturocastro.apiaudiospeechservice.model.ASModel;
import com.arturocastro.apiaudiospeechservice.service.AudioSpeechService;
import com.openai.core.http.HttpResponse;
import com.openai.core.http.StreamResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/openai/audio-speech")
public class AudioSpeechController {

    private final AudioSpeechService audioSpeechService;

    private AudioSpeechController(AudioSpeechService audioSpeechService) {
        this.audioSpeechService = audioSpeechService;
    }

    @PostMapping("/text-to-audio")
    public ResponseEntity<byte[]> createSession(@RequestBody ASModel asm) throws Exception {
        byte[] audioBytes;
        try (HttpResponse response = audioSpeechService.speechService(asm)) {
            audioBytes = response.body().readAllBytes();
        }
        return ResponseEntity.ok()
                .header("Content-Type", "audio/mpeg")
                .body(audioBytes);
    }

}
