package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.ehcache.Cache;
import org.yaml.snakeyaml.Yaml;
import spark.Response;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import static org.example.CacheUtils.getCacheDates;
import static org.example.CacheUtils.initCache;
import static org.example.Utils.*;
import static org.example.WebUtils.asteroidDescription;
import static org.example.WebUtils.returnResponse;
import static spark.Spark.*;

public class Main {
    public static String NASA_API_KEY;
    public final static String NASA_HOST = "https://api.nasa.gov/neo/rest/v1/feed";
    public final static int cacheSize = 30;

    public static void main(String[] args){
        Cache<String, String> cache = initCache(cacheSize);

        try {
            NASA_API_KEY = (String) ((Map<String, Object>)
                    (new Yaml().load(new FileInputStream("keys.yml")))).get("api_key");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        port(8080);
        get("/asteroids/dates", (req, res) -> {
            String[] dates;
            try {
                dates = handleDatesParam(req.queryParams("fromDate"), req.queryParams("toDate"));
            }
            catch (Exception e) {
                res.status(400);
                res.body("Wrong Query Params");
                return res.body();
            }
            String[] newDates = getCacheDates(dates[0], dates[1], cache);

            URL asteroidsUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                put("api_key", NASA_API_KEY);
                put("start_date", newDates[0]);
                put("end_date", newDates[1]);
            }});
            try {
                /*List<String> allResponses = new ArrayList<>(Arrays.asList(returnResponse(asteroidsUrl, res).body()
                        ,returnResponse(asteroidsUrltt, res).body()));*/
                //String asteroidString = combineAsteroidStrings(allResponses);
                String responseBody = returnResponse(asteroidsUrl, res).body();
                Object asteroids = new Gson().fromJson(responseBody, HashMap.class).get("near_earth_objects");
                /*if(asteroids!=null) {
                    /*for (JsonElement x: asteroids) {
                        System.out.println(x);
                    }/*
                }*/

                return editAsteroidResponse(asteroids.getAsJsonObject());
            } catch (NasaException e) {
                return handleNasaException(e, res).body();
            }
        });
        get("/asteroids/largest", (req, res) -> {
            URL asteroidsUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                put("api_key", NASA_API_KEY);
                if (req.queryParams("year") != null) {
                    put("fromDate", req.queryParams("year") + "-01-01");
                    put("toDate", req.queryParams("year") + "-12-31");
                }
            }});
            try {
                return asteroidDescription(editLargesAsteroidResponse(returnResponse(asteroidsUrl, res).body()), res);
            } catch (NasaException e) {
                return handleNasaException(e, res).body();
            }
        });
        notFound((req, res) -> {
            res.type("text/html");
            res.status(404);
            return "Page not found";
        });
        internalServerError((req, res) -> {
            res.status(500);
            res.type("application/json");
            return "Internal Server error";
        });
    }

    private static String[] handleDatesParam(String fromDate, String toDate) {
        LocalDate.parse(fromDate);
        try {
            LocalDate.parse(toDate);
        }
        catch (Exception e) {
            toDate = LocalDate.parse(fromDate).plusWeeks(1).toString();
        }
        return new String[]{fromDate, toDate};
    }

    private static Response handleNasaException(NasaException e, Response res) {
        res.type("text/html");
        res.body(e.getMessage());
        res.status(e.getResponseCode());
        return res;
    }
}