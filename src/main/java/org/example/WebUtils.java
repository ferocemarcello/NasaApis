package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.ehcache.Cache;
import spark.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.example.Utils.prettyIndentJsonString;

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

    public static void sendGetRequestAndRead(URL asteroidsUrl, Response response) throws IOException {
        HttpURLConnection con = sendGetRequest(asteroidsUrl);
        Reader streamReader;

        if (con.getResponseCode() > 299) {
            streamReader = new InputStreamReader(con.getErrorStream());
            response.type("text/html");
        } else {
            streamReader = new InputStreamReader(con.getInputStream());
            response.type("application/json");
        }
        response.status(con.getResponseCode());
        response.body(readResponseFromConnection(streamReader));
    }

    public static Response returnResponse(URL asteroidsUrl, Response response) throws IOException, NasaException {
        sendGetRequestAndRead(asteroidsUrl, response);
        if (response.status() <= 299 && 200 >= response.status()) {
            response.body(prettyIndentJsonString(response.body()));
        }
        else throw new NasaException(response.body(), response.status());
        return response;
    }
    public static String asteroidDescription(String singleAsteroid, Response response) throws IOException, NasaException {
        String selfLink = new Gson().fromJson(singleAsteroid, JsonObject.class).get("links").getAsJsonObject()
                .get("self").getAsString();
        return returnResponse(new URL(selfLink), response).body();
    }
}
