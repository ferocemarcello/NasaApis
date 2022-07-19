package org.nasaapis.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.nasaapis.NasaException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static org.nasaapis.utils.Utils.splitJsonStringToMap;

public class WebUtils {

    public static HttpURLConnection sendGetRequest(URL url, int connectionTimeout, int readTimeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        connection.disconnect();
        return connection;
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

    public static String sendGetRequestAndRead(URL asteroidsUrl, int connectionTimeout, int readTimeout) throws IOException, NasaException {
        HttpURLConnection con = sendGetRequest(asteroidsUrl, connectionTimeout, readTimeout);

        if (con.getResponseCode() > 299) {
            throw new NasaException(readResponseFromConnection(new InputStreamReader(con.getErrorStream())),
                    con.getResponseCode(), con.getContentType());
        } else {
            return readResponseFromConnection(new InputStreamReader(con.getInputStream()));
        }
    }

    public static Map<String, String> returnResponseMap(URL asteroidsUrl, int connectionTimeout, int readTimeout) throws IOException, NasaException {
        String response = sendGetRequestAndRead(asteroidsUrl, connectionTimeout, readTimeout);
        return splitJsonStringToMap(response, "near_earth_objects");
    }

    public static String asteroidDescription(String singleAsteroid) throws IOException, NasaException {
        String selfLink = new Gson().fromJson(singleAsteroid, JsonObject.class).get("links").getAsJsonObject()
                .get("self").getAsString();
        return sendGetRequestAndRead(new URL(selfLink), 0, 0);
    }
}
