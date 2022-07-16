package org.example;

import org.yaml.snakeyaml.Yaml;
import spark.ModelAndView;
import spark.Response;
import spark.template.velocity.VelocityTemplateEngine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.example.Utils.*;
import static org.example.WebUtils.asteroidDescription;
import static org.example.WebUtils.returnResponse;
import static spark.Spark.*;

public class Main {
    public static String NASA_API_KEY;
    public static String NASA_HOST = "https://api.nasa.gov/neo/rest/v1/feed";

    public static void main(String[] args) {
        try {
            NASA_API_KEY = (String) ((Map<String, Object>)
                    (new Yaml().load(new FileInputStream("keys.yml")))).get("api_key");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        port(8080);
        get("/hello", (req, res) -> "Hello");
        get("/index", (req, res) -> {
            req.session().attribute("apiKey", "shit");
            Map<String, Object> model = new HashMap<>();
            model.put("key", "");
            model.put("title", "NASA APIs");
            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.vm");
        }, new VelocityTemplateEngine());
        get("/show_all", (req, res) -> "show_all");
        get("/asteroids/dates", (req, res) -> {
            URL asteroidsUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                put("api_key", NASA_API_KEY);
                if (req.queryParams("fromDate") != null) put("fromDate", req.queryParams("fromDate"));
                if (req.queryParams("toDate") != null) put("toDate", req.queryParams("toDate"));
            }});
            try {
                return editAsteroidResponse(returnResponse(asteroidsUrl, res).body());

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
            return "Page not found. Go to /index";
        });
        internalServerError((req, res) -> {
            res.status(500);
            res.type("application/json");
            return "Internal Server error";
        });
    }

    private static Response handleNasaException(NasaException e, Response res) {
        res.type("text/html");
        res.body(e.getMessage());
        res.status(e.getResponseCode());
        return res;
    }
}