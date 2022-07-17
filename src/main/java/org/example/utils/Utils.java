package org.example.utils;

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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
        return String.valueOf(asteroidArray);
    }

    public static List<URL> getNasaUrlsFromYear(String year, String nasaHost, String nasaApiKey) throws MalformedURLException, URISyntaxException {
        LocalDate start_date = LocalDate.parse(year + "-01-01");
        LocalDate end_date = start_date.plusWeeks(1);
        List<URL> urlsFromYear = new LinkedList<>();
        while (end_date.getYear() == Integer.parseInt(year)) {
            LocalDate finalEnd_date = end_date;
            LocalDate finalStart_date = start_date;
            urlsFromYear.add(buildUrl(nasaHost, new HashMap<>() {{
                put("api_key", nasaApiKey);
                put("start_date", finalStart_date.toString());
                put("end_date", finalEnd_date.toString());
            }}));
            start_date = start_date.plusDays(8);
            end_date = start_date.plusWeeks(1);
        }
        if (start_date.getYear() == Integer.parseInt(year)) {
            LocalDate finalStart_date1 = start_date;
            urlsFromYear.add(buildUrl(nasaHost, new HashMap<>() {{
                put("api_key", nasaApiKey);
                put("start_date", finalStart_date1.toString());
                put("end_date", year + "-12-31");
            }}));
        }
        return urlsFromYear;
    }

    public static String getLargerAsteroid(String asteroidOne, String asteroidTwo) {
        if (asteroidOne == null) return asteroidTwo;
        if (asteroidTwo == null) return asteroidOne;
        JsonObject diameterOne = new Gson().fromJson(asteroidOne, JsonObject.class).get("estimated_diameter").getAsJsonObject().get("meters").getAsJsonObject();
        double averageOne = (diameterOne.get("estimated_diameter_max").getAsDouble()
                - diameterOne.get("estimated_diameter_min").getAsDouble()) / 2;

        JsonObject diameterTwo = new Gson().fromJson(asteroidTwo, JsonObject.class).get("estimated_diameter").getAsJsonObject().get("meters").getAsJsonObject();
        double averageTwo = (diameterTwo.get("estimated_diameter_max").getAsDouble()
                - diameterTwo.get("estimated_diameter_min").getAsDouble()) / 2;
        if (averageOne >= averageTwo) return asteroidOne;
        else return asteroidTwo;
    }

    public static String getLargestAsteroid(String jsonResponse, String arrayKey) throws JsonProcessingException {
        JsonObject asteroids = (JsonObject) new Gson().fromJson(jsonResponse, JsonObject.class)
                .get(arrayKey);
        if (asteroids != null) {
            double maxDiameter = 0;
            JsonElement largestAsteroid = null;
            for (Map.Entry<String, JsonElement> date : asteroids.entrySet()) {
                for (JsonElement asteroid : date.getValue().getAsJsonArray()) {
                    double estimatedDiameterAverage = -1;
                    if (asteroid.getAsJsonObject().has("estimated_diameter")) {
                        JsonObject estimatedDiameter = asteroid.getAsJsonObject()
                                .get("estimated_diameter").getAsJsonObject().get("meters").getAsJsonObject();
                        estimatedDiameterAverage = (estimatedDiameter.get("estimated_diameter_max")
                                .getAsDouble() - estimatedDiameter.get("estimated_diameter_min").getAsDouble()) / 2;
                    }
                    if (estimatedDiameterAverage > maxDiameter) {
                        maxDiameter = estimatedDiameterAverage;
                        largestAsteroid = asteroid;
                    }
                }
            }
            return String.valueOf(largestAsteroid);
        }
        return jsonResponse;
    }

    public static Map<String, String> combineResponses(Map<String, String> mapOne, Map<String, String> mapTwo) {
        mapOne.putAll(mapTwo);
        return mapOne;
    }

    public static Map<String, String> splitJsonStringToMap(String response, String arrayKey) {
        Map<String, String> map = new HashMap<>();
        JsonObject asteroids = new Gson().fromJson(response, JsonObject.class);
        if (asteroids.get(arrayKey) == null) return map;
        for (Map.Entry<String, JsonElement> date : (asteroids.get(arrayKey))
                .getAsJsonObject().entrySet()) {
            map.put(date.getKey(), date.getValue().toString());
        }
        return map;
    }
}
