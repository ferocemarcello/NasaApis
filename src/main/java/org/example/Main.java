package org.example;

import org.ehcache.Cache;
import spark.Response;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.example.utils.CacheUtils.*;
import static org.example.utils.Utils.*;
import static org.example.utils.WebUtils.*;
import static spark.Spark.*;

public class Main {
    public final static String NASA_HOST = "https://api.nasa.gov/neo/rest/v1/feed";
    public final static String YEAR_TABLE = "YEARS";
    public static final String CACHE_DATES_NAME = "cacheDates";
    public static final String CACHE_YEARS_NAME = "cacheYears";
    private static final String DATES_TABLE = "DATES";
    private static final int CONNECTION_TIMEOUT = 60000;
    private static final int READ_TIMEOUT = 60000;
    public static int CACHE_DATES_SIZE;
    public static int CACHE_YEARS_SIZE;
    public static String NASA_API_KEY;
    public static Cache<String, String> CACHE_DATES;
    public static Cache<String, String> CACHE_YEARS;
    private static DAO DAO;

    public static void main(String[] args) {
        if (args.length >= 1) NASA_API_KEY = args[0];
        else NASA_API_KEY = null;
        if (args.length >= 2) port(Integer.parseInt(args[1]));
        else port(8080);
        if (args.length >= 3) CACHE_DATES_SIZE = Integer.parseInt(args[2]);
        else CACHE_DATES_SIZE = 30;
        if (args.length >= 4) CACHE_YEARS_SIZE = Integer.parseInt(args[3]);
        else CACHE_YEARS_SIZE = 20;
        if (args.length >= 5) DAO = new DAO(new DbConfig(args[4]));//path to dbConfigFile
        CACHE_DATES = initCache(CACHE_DATES_SIZE, CACHE_DATES_NAME);
        CACHE_YEARS = initCache(CACHE_YEARS_SIZE, CACHE_YEARS_NAME);
        get("/asteroids/dates", (req, res) -> {
            String[] dates;
            try {
                dates = handleDatesParam(req.queryParams("fromDate"), req.queryParams("toDate"));
            } catch (Exception e) {
                editResponse(res, 400, "text/html", "Wrong Query Params");
                return res.body();
            }
            Pair<String[], String[]> datesInCacheAndDb = getDatesInCacheDb(dates[0], dates[1],
                    CACHE_DATES, DAO, DATES_TABLE);
            String[] datesInCache = datesInCacheAndDb.getFirst();
            String[] datesInDb = datesInCacheAndDb.getSecond();
            String[] datesToRequest = getNoCacheDates(dates[0], dates[1], datesInCache, datesInDb);
            Map<String, String> requestResponseMap;
            if (datesToRequest != null) {
                URL asteroidsUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                    put("api_key", NASA_API_KEY);
                    put("start_date", datesToRequest[0]);
                    put("end_date", datesToRequest[1]);
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
            if (DAO != null) DAO.putManyInDb(requestResponseMap.entrySet());
            editResponse(res, 200, "application/json",
                    prettyIndentJsonString(editAsteroidResponse(toReturnAsteroids)));
            return res.body();
        });
        get("/asteroids/largest", (req, res) -> {
            String year = req.queryParams("year");
            String yearResponse;
            if (CACHE_YEARS.containsKey(year)) {
                yearResponse = CACHE_YEARS.get(year);
            } else {
                if (DAO != null && DAO.contains(YEAR_TABLE, year)) yearResponse = DAO.get(YEAR_TABLE, year);
                else {
                    List<URL> yearUrls = getNasaUrlsFromYear(year, NASA_HOST, NASA_API_KEY);
                    String largestAsteroid = null;
                    for (URL partYearUrl : yearUrls) {
                        try {
                            largestAsteroid = getLargerAsteroid(largestAsteroid, getLargestAsteroid(
                                    sendGetRequestAndRead(partYearUrl, CONNECTION_TIMEOUT, READ_TIMEOUT),
                                    "near_earth_objects"));
                        } catch (NasaException e) {
                            editResponse(res, e.getResponseCode(), e.getContentType(), e.getMessage());
                            return res.body();
                        }
                    }
                    try {
                        yearResponse = asteroidDescription(largestAsteroid);
                    } catch (NasaException e) {
                        editResponse(res, e.getResponseCode(), e.getContentType(), e.getMessage());
                        return res.body();
                    }
                    CACHE_YEARS.put(year, yearResponse);
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