package org.example;
import spark.ModelAndView;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        port(8080);
        get("/hello", (req, res) -> {
            return "Hello";
        });
        get("/index", (req, res) -> {
            req.session().attribute("apiKey","shit");
            Map<String, Object> model = new HashMap<>();
            model.put("key", "");
            model.put("title", "NASA APIs");
            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.vm");
        }, new VelocityTemplateEngine());
        get("/show_all", (req, res) -> {
            return "show_all";
        });
        notFound((req, res) -> {
            res.type("text/html");
            return "Page not found. Go to /index";
        });
    }
}