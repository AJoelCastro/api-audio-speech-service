package com.arturocastro.apiaudiospeechservice.model;

public class ASModel {

    private String input;
    private byte[] audioBytes;
    private String path;

    public ASModel() {
    }

    public String getInput() {
        return input;
    }
    public byte[] getAudioBytes() {
        return audioBytes;
    }
    public String getPath() {
        return path;
    }

    public void setInput(String input) {
        this.input = input;
    }
    public void setAudioBytes(byte[] audioBytes) {
        this.audioBytes = audioBytes;
    }
    public void setPath(String path) {
        this.path = path;
    }
}
