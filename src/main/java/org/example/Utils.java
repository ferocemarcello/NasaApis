package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class Utils {
    public static URL buildUrl(String host, Map<String, String> queryParams) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(host);
        for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            uriBuilder.addParameter(queryParam.getKey(), queryParam.getValue());
        }
        return uriBuilder.build().toURL();
    }

    public static String prettyIndentJsonString(String jsonString) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(String.valueOf(jsonString), Object.class));
    }

    public static String editAsteroidResponse(Map<String, String> json) throws JsonProcessingException {
        JsonArray asteroidArray = new JsonArray(10);
        for (Map.Entry<String, String> date : json.entrySet()) {
            if (asteroidArray.size() >= 10) break;
            for (JsonElement asteroid : new Gson().fromJson(date.getValue(), JsonArray.class)) {
                if (asteroidArray.size() >= 10) break;
                asteroid.getAsJsonObject().addProperty("date", date.getKey());
                asteroidArray.add(asteroid);
            }
        }
        return prettyIndentJsonString(String.valueOf(asteroidArray));
    }

    public static String editLargesAsteroidResponse(String jsonResponse) throws JsonProcessingException {
        JsonObject asteroids = (JsonObject) new Gson().fromJson(jsonResponse, JsonObject.class)
                .get("near_earth_objects");
        if (asteroids != null) {
            double maxDiameter = 0;
            JsonElement largestAsteroid = null;
            for (Map.Entry<String, JsonElement> date : asteroids.entrySet()) {
                for (JsonElement asteroid : date.getValue().getAsJsonArray()) {
                    JsonObject estimatedDiameter = asteroid.getAsJsonObject()
                            .get("estimated_diameter").getAsJsonObject().get("meters").getAsJsonObject();
                    double estimatedDiameterAverage = (estimatedDiameter.get("estimated_diameter_max")
                            .getAsDouble() - estimatedDiameter.get("estimated_diameter_min").getAsDouble()) / 2;
                    if (estimatedDiameterAverage > maxDiameter) {
                        maxDiameter = estimatedDiameterAverage;
                        largestAsteroid = asteroid;
                    }
                }
            }
            return prettyIndentJsonString(String.valueOf(largestAsteroid));
        }
        return jsonResponse;
    }

    public static Map<String, String> combineResponses(Map<String, String> mapOne, Map<String, String> mapTwo) {
        mapOne.putAll(mapTwo);
        return mapOne;
    }
}
