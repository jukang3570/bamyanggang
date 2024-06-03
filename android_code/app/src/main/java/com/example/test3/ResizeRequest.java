package com.example.test3;

public class ResizeRequest {
    private int platform;
    private String filename;

    public ResizeRequest(int platform, String filename) {
        this.platform = platform;
        this.filename = filename;
    }

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
