package org.example;
import java.util.Arrays;
import java.util.List;

import static spark.Spark.*;

public class Main {
    private static final List<String> paths = Arrays.asList("show_all","/hello", "/index");
    public static void main(String[] args) {
        port(8080);
        get("/hello", (req, res) -> {
            return "Hello World";
        });
        get("/index", (req, res) -> {
            return "Index";
        });
        get("/show_all", (req, res) -> {
            return "show_all";
        });
        notFound((req, res) -> {
            res.type("text/html");
            return "Page not found. Go to /index";
        });
    }
}