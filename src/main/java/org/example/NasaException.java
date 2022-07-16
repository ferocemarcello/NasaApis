package org.example;

public class NasaException extends Throwable {
    private final String nasaResponse;
    private final String responseMessage;
    private final int responseCode;
    public NasaException(String response, String responseMessage, int responseCode) {
        this.nasaResponse = response;
        this.responseMessage = responseMessage;
        this.responseCode = responseCode;
    }

    @Override
    public String getMessage() {
        return this.nasaResponse;
    }
    public String getResponseMessage() {
        return this.responseMessage;
    }
    public int getResponseCode() {
        return this.responseCode;
    }
}