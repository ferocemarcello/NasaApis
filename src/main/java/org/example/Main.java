package org.example;

import org.ehcache.Cache;
import spark.Response;

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
    public static int CACHE_DATES_SIZE;
    public static int CACHE_YEARS_SIZE;
    public static String NASA_API_KEY;
    public static Cache<String, String> CACHE_DATES;
    public static final String CACHE_DATES_NAME = "cacheDates";
    public static Cache<String, String> CACHE_YEARS;
    public static final String CACHE_YEARS_NAME = "cacheYears";
    private static final int CONNECTION_TIMEOUT = 60000;
    private static final int READ_TIMEOUT = 60000;

    public static void main(String[] args) {
        CACHE_DATES_SIZE = Integer.parseInt(args[1]);
        CACHE_YEARS_SIZE = Integer.parseInt(args[2]);
        CACHE_DATES = initCache(CACHE_DATES_SIZE, CACHE_DATES_NAME);
        CACHE_YEARS = initCache(CACHE_YEARS_SIZE, CACHE_YEARS_NAME);
        port(Integer.parseInt(args[0]));
        NASA_API_KEY = args[3];
        get("/asteroids/dates", (req, res) -> {
            String[] dates;
            try {
                dates = handleDatesParam(req.queryParams("fromDate"), req.queryParams("toDate"));
            } catch (Exception e) {
                editResponse(res, 400, "text/html", "Wrong Query Params");
                return res.body();
            }
            String[] newDates = getNoCacheDates(dates[0], dates[1], CACHE_DATES);
            String[] datesInCache = getDatesInCache(dates[0], dates[1], CACHE_DATES);
            Map<String, String> requestResponseMap;
            if (newDates != null) {
                URL asteroidsUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                    put("api_key", NASA_API_KEY);
                    put("start_date", newDates[0]);
                    put("end_date", newDates[1]);
                }});
                try {
                    requestResponseMap = returnResponseMap(
                            asteroidsUrl, CONNECTION_TIMEOUT, READ_TIMEOUT);
                } catch (NasaException e) {
                    editResponse(res, e.getResponseCode(), e.getContentType(), e.getMessage());
                    return res.body();
                }
            } else requestResponseMap = new HashMap<>();
            Map<String, String> cacheMap = filterCacheToMap(datesInCache, CACHE_DATES);
            Map<String, String> toReturnAsteroids = combineResponses(cacheMap, requestResponseMap);
            putMapInCache(requestResponseMap, CACHE_DATES);
            editResponse(res, 200,"application/json",
                    prettyIndentJsonString(editAsteroidResponse(toReturnAsteroids)));
            return res.body();
        });
        get("/asteroids/largest", (req, res) -> {
            String year = req.queryParams("year");
            String yearResponse;
            if(CACHE_YEARS.containsKey(year)) {
                yearResponse = CACHE_YEARS.get(year);
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
                    yearResponse = asteroidDescription(editLargesAsteroidResponse(
                            sendGetRequestAndRead(largestUrl, CONNECTION_TIMEOUT, READ_TIMEOUT),
                            "near_earth_objects"));
                    CACHE_YEARS.put(year, yearResponse);
                } catch (NasaException e) {
                    editResponse(res, e.getResponseCode(), e.getContentType(), e.getMessage());
                    return res.body();
                }
            }
            editResponse(res, 200, "application/json", prettyIndentJsonString(yearResponse));
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

    private static void editResponse(Response response, int status, String type, String body) {
        response.status(status);
        response.body(body);
        response.type(type);
    }

    private static String[] handleDatesParam(String fromDate, String toDate) {
        LocalDate.parse(fromDate);
        if (toDate == null) toDate = LocalDate.parse(fromDate).plusWeeks(1).toString();
        LocalDate.parse(toDate);
        return new String[]{fromDate, toDate};
    }
}