package org.example;

import org.ehcache.Cache;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.example.CacheUtils.*;
import static org.example.Utils.*;
import static org.example.WebUtils.*;
import static spark.Spark.*;

public class Main {
    public final static String NASA_HOST = "https://api.nasa.gov/neo/rest/v1/feed";
    public final static int cacheSize = 30;
    public static String NASA_API_KEY;
    public static Cache<String, String> cache;

    public static void main(String[] args) {
        cache = initCache(cacheSize);

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
            } catch (Exception e) {
                res.status(400);
                res.body("Wrong Query Params");
                return res.body();
            }
            String[] newDates = getNoCacheDates(dates[0], dates[1], cache);
            String[] cacheDates = getCacheDates(dates[0], dates[1], cache);
            Map<String, String> requestResponseMap;
            if (newDates != null) {
                URL asteroidsUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                    put("api_key", NASA_API_KEY);
                    put("start_date", newDates[0]);
                    put("end_date", newDates[1]);
                }});
                try {
                    requestResponseMap = returnResponseMap(asteroidsUrl);
                } catch (NasaException e) {
                    res.type(e.getContentType());
                    res.body(e.getMessage());
                    res.status(e.getResponseCode());
                    return res.body();
                }
            } else requestResponseMap = new HashMap<>();
            Map<String, String> cacheMap = filterCacheToMap(cacheDates, cache);
            Map<String, String> toReturnAsteroids = combineResponses(cacheMap, requestResponseMap);
            putMapInCache(requestResponseMap, cache);
            res.status(200);
            res.body(editAsteroidResponse(toReturnAsteroids));
            res.type("application/json");
            return res.body();
        });
        get("/asteroids/largest", (req, res) -> {
            URL largestUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                put("api_key", NASA_API_KEY);
                if (req.queryParams("year") != null) {
                    put("fromDate", req.queryParams("year") + "-01-01");
                    put("toDate", req.queryParams("year") + "-12-31");
                }
            }});
            try {
                return asteroidDescription(editLargesAsteroidResponse(returnResponse(largestUrl)));
            } catch (NasaException e) {
                res.type(e.getContentType());
                res.body(e.getMessage());
                res.status(e.getResponseCode());
                return res.body();
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
        if (toDate == null) toDate = LocalDate.parse(fromDate).plusWeeks(1).toString();
        LocalDate.parse(toDate);
        return new String[]{fromDate, toDate};
    }
}