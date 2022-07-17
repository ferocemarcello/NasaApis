package org.example;

import org.ehcache.Cache;

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
    public static int cacheDatesSize;
    public static int cacheYearsSize;
    public static String NASA_API_KEY;
    public static Cache<String, String> cacheDates;
    public static Cache<String, String> cacheYears;

    public static void main(String[] args) {
        cacheDatesSize = Integer.parseInt(args[1]);
        cacheYearsSize = Integer.parseInt(args[2]);
        cacheDates = initCache(cacheDatesSize, "cacheDates");
        cacheYears = initCache(cacheDatesSize, "cacheYears");
        port(Integer.parseInt(args[0]));
        NASA_API_KEY = args[3];
        get("/asteroids/dates", (req, res) -> {
            String[] dates;
            try {
                dates = handleDatesParam(req.queryParams("fromDate"), req.queryParams("toDate"));
            } catch (Exception e) {
                res.status(400);
                res.body("Wrong Query Params");
                return res.body();
            }
            String[] newDates = getNoCacheDates(dates[0], dates[1], cacheDates);
            String[] datesInCache = getDatesInCache(dates[0], dates[1], cacheDates);
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
            Map<String, String> cacheMap = filterCacheToMap(datesInCache, cacheDates);
            Map<String, String> toReturnAsteroids = combineResponses(cacheMap, requestResponseMap);
            putMapInCache(requestResponseMap, cacheDates);
            res.status(200);
            res.body(editAsteroidResponse(toReturnAsteroids));
            res.type("application/json");
            return res.body();
        });
        get("/asteroids/largest", (req, res) -> {
            String year = req.queryParams("year");
            String yearResponse;
            if(cacheYears.containsKey(year)) {
                yearResponse = cacheYears.get(year);
            }
            else {
                URL largestUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                    put("api_key", NASA_API_KEY);
                    if (req.queryParams("year") != null) {
                        put("fromDate", year + "-01-01");
                        put("toDate", year + "-12-31");
                    }
                }});
                try {
                    yearResponse = asteroidDescription(editLargesAsteroidResponse(returnResponse(largestUrl)));
                    cacheYears.put(year, yearResponse);
                } catch (NasaException e) {
                    res.type(e.getContentType());
                    res.body(e.getMessage());
                    res.status(e.getResponseCode());
                    return res.body();
                }
            }
            res.type("application/json");
            yearResponse = prettyIndentJsonString(yearResponse);
            res.body(yearResponse);
            res.status(200);
            return res.body();
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