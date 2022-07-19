package org.nasaapis;

import org.ehcache.Cache;
import org.nasaapis.utils.CacheUtils;
import spark.Response;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import static org.nasaapis.utils.CacheUtils.*;
import static org.nasaapis.utils.Utils.*;
import static org.nasaapis.utils.WebUtils.*;
import static spark.Spark.*;

public class Main {
    public final static String NASA_HOST = "https://api.nasa.gov/neo/rest/v1/feed";
    public final static String YEAR_TABLE = "YEARS";
    public static final String CACHE_DATES_NAME = "cacheDates";
    public static final String CACHE_YEARS_NAME = "cacheYears";
    public static final String DATES_TABLE = "DATES";
    private static final int CONNECTION_TIMEOUT = 60000;
    private static final int READ_TIMEOUT = 60000;
    public static final Pair<String, String> DATES_TABLE_COLUMNS = new Pair<>("date","asteroids");
    public static final Pair<String, String> YEARS_TABLE_COLUMNS = new Pair<>("year","description");
    public static int CACHE_DATES_SIZE;
    public static int CACHE_YEARS_SIZE;
    public static String NASA_API_KEY;
    public static Cache<String, String> CACHE_DATES;
    public static Cache<String, String> CACHE_YEARS;
    private static DAO DAO;
    private static final int defaultSparkPort = 8080;

    public static void main(String[] args) {
        if (args.length >= 1) NASA_API_KEY = args[0];
        else NASA_API_KEY = null;
        int port;
        if (args.length >= 2) port = getHerokuAssignedPort(Integer.parseInt(args[1]));
        else port = getHerokuAssignedPort(defaultSparkPort);
        if (args.length >= 3) CACHE_DATES_SIZE = Integer.parseInt(args[2]);
        else CACHE_DATES_SIZE = 30;
        if (args.length >= 4) CACHE_YEARS_SIZE = Integer.parseInt(args[3]);
        else CACHE_YEARS_SIZE = 20;
        if (args.length >= 5) {
            try {
                DAO = new DAO(new DbConfig(args[4]));//path to dbConfigFile
            } catch (FileNotFoundException e) {
                DAO = null;
            }
        }
        else {
            try {
                URI dbUri = new URI(System.getenv("DATABASE_URL"));
                int dbport = dbUri.getPort();
                String host = dbUri.getHost();
                String db = dbUri.getPath().substring(1);
                String username = (dbUri.getUserInfo() == null) ? null : dbUri.getUserInfo().split(":")[0];
                String password = (dbUri.getUserInfo() == null) ? null : dbUri.getUserInfo().split(":")[1];
                DAO = new DAO(new DbConfig(host,String.valueOf(dbport),username,password, db));
            } catch (URISyntaxException ex) {
                DAO = null;
            }
        }
        port(port);
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
            String[] datesToRequest = getNoCacheDbDates(dates[0], dates[1], datesInCache, datesInDb);
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
            Map<String, String> dbMap = new HashMap<>();
            if(DAO!=null) dbMap = DAO.filterDbToMap(DATES_TABLE, datesInDb, DATES_TABLE_COLUMNS.getFirst(),
                    DATES_TABLE_COLUMNS.getSecond());
            Map<String, String> finalDbMap = dbMap;
            Map<String, String> dbAndRequestMap = combineResponses(new ArrayList<>(){{add(requestResponseMap);
                add(finalDbMap);}});
            Map<String, String> toReturnAsteroids = combineResponses(new ArrayList<>()
            {{add(dbAndRequestMap);add(cacheMap);}});
            putMapInCache(dbAndRequestMap, CACHE_DATES);
            if (DAO != null) DAO.putManyInDb(requestResponseMap.entrySet(), new Pair<>(DATES_TABLE,
                    DATES_TABLE_COLUMNS));
            editResponse(res, 200, "application/json",
                    prettyIndentJsonString(editAsteroidResponse(toReturnAsteroids)));
            return res.body();
        });
        get("/asteroids/largest", (req, res) -> {
            String year = req.queryParams("year");
            String yearResponse;
            boolean yearInCache = CACHE_YEARS.containsKey(year);
            List<String> yearInDbResult = new ArrayList<>();
            if(DAO!=null) yearInDbResult = DAO.getFromKey(YEAR_TABLE, new Pair<>(YEARS_TABLE_COLUMNS.getFirst(), year),
                    YEARS_TABLE_COLUMNS.getSecond());
            boolean yearInDb = yearInDbResult.size()>0;
            if (yearInCache || yearInDb) {
                if(yearInCache) yearResponse = CACHE_YEARS.get(year);
                else yearResponse = yearInDbResult.get(0);
            } else {
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
                    HashSet<Map.Entry<String,String>> yearSetInDb = new HashSet<>();
                    yearSetInDb.add(new AbstractMap.SimpleEntry<>(year,yearResponse));
                    if(DAO!=null) DAO.putManyInDb(yearSetInDb, new Pair<>(YEAR_TABLE, YEARS_TABLE_COLUMNS));
                    CACHE_YEARS.put(year, yearResponse);
                }
            editResponse(res, 200, "application/json", prettyIndentJsonString(yearResponse));
            return res.body();
        });
        get("/dates/closeCache", (req, res) -> {
            long previousSize = getCacheSize(CACHE_DATES_NAME);
            CacheUtils.stopCache(CACHE_DATES);
            return "Cache for years has been stopped. Size of the cache is now 0. Previously it was +"+ previousSize;
        });
        get("/years/closeCache", (req, res) -> {
            long previousSize = getCacheSize(CACHE_YEARS_NAME);
            CacheUtils.stopCache(CACHE_YEARS);
            return "Cache for years has been stopped. Size of the cache is now 0. Previously it was +"+ previousSize;
        });
        get("/dates/cacheSize", (req, res) -> {
            long size = getCacheSize(CACHE_DATES_NAME);
            return "Size of the cache is now "+ size;
        });
        get("/years/cacheSize", (req, res) -> {
            long size = getCacheSize(CACHE_YEARS_NAME);
            return "Size of the cache is now "+ size;
        });
        get("/dates/dbEntries", (req, res) -> {
            if(DAO==null) return "There is no Database configured";
            res.type("text/html");
            return DAO.getTableInfoAsHtml(DATES_TABLE, DATES_TABLE_COLUMNS);
        });
        get("/years/dbEntries", (req, res) -> {
            if(DAO==null) return "There is no Database configured";
            res.type("text/html");
            return DAO.getTableInfoAsHtml(YEAR_TABLE, YEARS_TABLE_COLUMNS);
        });
        get("/connectToDb", (req, res) -> {
            if(DAO==null) return "There is no Database configured";
            if(DAO.isConnected()) return "Already connected";
            if(DAO.getDbConfig()==null) return "No dbconfig";
            DAO.connectToDb();
            return "Connected";
        });
        get("/disconnectDb", (req, res) -> {
            if(DAO==null) return "There is no Database configured";
            if(DAO.isConnected()) return "No connection to database. Use /connectToDb";
            DAO.disconnect();
            return "Disconnected";
        });
        get("/index", (req, res) -> {
            long sizeDates = getCacheSize(CACHE_DATES_NAME);
            long sizeYears = getCacheSize(CACHE_YEARS_NAME);
            res.type("text/html");
            res.status(200);
            String returnString = "Current size of the cache for dates is now " + sizeDates + "<br>" +
                    "Current size of the cache for dates is now " + sizeYears + "<br>" +
                    "The Max size of the cache for dates is now " + CACHE_DATES_SIZE + "<br>" +
                    "The Max size of the cache for years is now " + CACHE_YEARS_SIZE + "<br>";
            if(DAO!=null) {
                return returnString+"<br>DB config is:<br>"+DAO.getDbConfig().toString();
            }
            return returnString;
        });
        get("/", (req, res) -> {
            res.redirect("/index");
            return res;
        });
        notFound((req, res) -> {
            res.type("text/html");
            res.status(404);
            return "Page not found, go to /index";
        });
        internalServerError((req, res) -> {
            res.status(500);
            res.type("application/json");
            return "Internal Server error";
        });
    }

    private static int getHerokuAssignedPort(int port) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        if(port==0) return 8080;
        return port; //return default port if heroku-port isn't set (i.e. on localhost)
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