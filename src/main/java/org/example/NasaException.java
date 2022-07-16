package org.example;

public class NasaException extends Throwable {
    private final String nasaResponse;
    private final int responseCode;
    public NasaException(String response, int responseCode) {
        this.nasaResponse = response;
        this.responseCode = responseCode;
    }

    @Override
    public String getMessage() {
        return this.nasaResponse;
    }
    public int getResponseCode() {
        return this.responseCode;
    }
}