package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

    public static String readResponseFromConnection(Reader streamReader) throws IOException {
        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }

    public static String sendGetRequestAndRead(URL asteroidsUrl) throws IOException, NasaException {
        HttpURLConnection con = sendGetRequest(asteroidsUrl);
        Reader streamReader;

        if (con.getResponseCode() > 299) {
            streamReader = new InputStreamReader(con.getErrorStream());
            throw new NasaException(readResponseFromConnection(streamReader), con.getResponseCode(), con.getContentType());
        } else {
            streamReader = new InputStreamReader(con.getInputStream());
        }
        return readResponseFromConnection(streamReader);
    }

    public static String returnResponse(URL asteroidsUrl) throws IOException, NasaException {
        return sendGetRequestAndRead(asteroidsUrl);
    }

    public static Map<String, String> returnResponseMap(URL asteroidsUrl) throws IOException, NasaException {
        String response = sendGetRequestAndRead(asteroidsUrl);
        return splitJsonStringToMap(response);
    }

    private static Map<String, String> splitJsonStringToMap(String response) {
        Map<String, String> map = new HashMap<>();
        JsonObject asteroids = new Gson().fromJson(response, JsonObject.class);
        if (asteroids.get("near_earth_objects") == null) return map;
        for (Map.Entry<String, JsonElement> date : (asteroids.get("near_earth_objects"))
                .getAsJsonObject().entrySet()) {
            map.put(date.getKey(), date.getValue().toString());
        }
        return map;
    }

    public static String asteroidDescription(String singleAsteroid) throws IOException, NasaException {
        String selfLink = new Gson().fromJson(singleAsteroid, JsonObject.class).get("links").getAsJsonObject()
                .get("self").getAsString();
        return returnResponse(new URL(selfLink));
    }
}
