package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import spark.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebUtils {
    private static final int connectionTimeout = 60000;
    private static final int readTimeout = 60000;

    public static HttpURLConnection sendGetRequest(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setConnectTimeout(connectionTimeout);
        con.setReadTimeout(readTimeout);
        con.disconnect();
        return con;
    }

    public static ResponseClass readResponseFromConnection(Reader streamReader, String message, int responseCode) throws IOException {
        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        String resNasa = content.toString();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return new ResponseClass(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(resNasa, Object.class)), message, responseCode);
        } catch (JsonProcessingException e) {
            return new ResponseClass(resNasa, message, responseCode);
        }
    }

    public static ResponseClass sendGetRequestAndRead(URL asteroidsUrl) throws IOException {
        HttpURLConnection con = sendGetRequest(asteroidsUrl);
        Reader streamReader;

        if (con.getResponseCode() > 299) {
            streamReader = new InputStreamReader(con.getErrorStream());
        } else {
            streamReader = new InputStreamReader(con.getInputStream());
        }
        return readResponseFromConnection(streamReader, con.getResponseMessage(), con.getResponseCode());
    }

    public static JsonObject retResponse(URL asteroidsUrl, Response res) throws IOException, NasaException {
        WebUtils.ResponseClass response = sendGetRequestAndRead(asteroidsUrl);
        res.status(response.getCode());
        if (200 >= res.status() && res.status() <= 299) {
            res.type("application/json");
            return new Gson().fromJson(response.getResponse(), JsonObject.class);
        }
        else throw new NasaException(response.getResponse(), response.getMessage(), response.getCode());
    }

    public static class ResponseClass {
        private final String response;
        private final String message;
        private final int code;

        public ResponseClass(String response, String message, int code) {
            this.response = response;
            this.message = message;
            this.code = code;
        }

        public String getResponse() {
            return response;
        }

        public String getMessage() {
            return message;
        }

        public int getCode() {
            return code;
        }
    }

}
