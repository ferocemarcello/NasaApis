package org.nasaapis;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.nasaapis.utils.Utils.prettyIndentJsonString;

public class NasaException extends Throwable {
    private final String nasaResponse;
    private final int responseCode;
    private final String contentType;

    public NasaException(String response, int responseCode, String contentType) throws JsonProcessingException {
        this.responseCode = responseCode;
        this.contentType = contentType;
        if (contentType.toLowerCase().contains("json")) this.nasaResponse = prettyIndentJsonString(response);
        else this.nasaResponse = response;
    }

    @Override
    public String getMessage() {
        return this.nasaResponse;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getContentType() {
        return this.contentType;
    }
}