package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.example.Util.buildUrl;
import static spark.Spark.*;

public class Main {
    public static String NASA_API_KEY;
    public static String NASA_HOST = "https://api.nasa.gov/neo/rest/v1/feed";
    private static final int connectionTimeout = 60000;
    private static final int readTimeout = 60000;

    public static void main(String[] args) {
        try {
            NASA_API_KEY = (String) ((Map<String, Object>) (new Yaml().load(new FileInputStream(new File("keys.yml"))))).get("api_key");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        port(8080);
        get("/hello", (req, res) -> {
            return "Hello";
        });
        get("/index", (req, res) -> {
            req.session().attribute("apiKey", "shit");
            Map<String, Object> model = new HashMap<>();
            model.put("key", "");
            model.put("title", "NASA APIs");
            // The vm files are located under the resources directory
            return new ModelAndView(model, "index.vm");
        }, new VelocityTemplateEngine());
        get("/show_all", (req, res) -> {
            return "show_all";
        });
        get("/asteroids", (req, res) -> {
            String fromDate = req.queryParams("fromDate");
            String toDate = req.queryParams("toDate");
            URL asteroidsUrl = buildUrl(NASA_HOST, new HashMap<>() {{
                put("api_key", NASA_API_KEY);
                put("fromDate", fromDate);
                put("toDate", toDate);
            }});
            ResponseClass response = sendGetRequestAndRead(asteroidsUrl);
            res.status(response.getCode());
            if (200 >= res.status() && res.status() > 299) res.type("application/json");
            else res.type("text/html");
            return "asteroidsResponse is :\n" + response.getResponse();
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

    private static HttpURLConnection sendGetRequest(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setConnectTimeout(connectionTimeout);
        con.setReadTimeout(readTimeout);
        con.disconnect();
        return con;
    }

    private static ResponseClass readResponseFromConnection(Reader streamReader, String message, int responseCode) throws IOException {
        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        String resNasa = content.toString();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return new ResponseClass(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(resNasa, Object.class)), message, responseCode);
        } catch (JsonProcessingException e) {
            return new ResponseClass(resNasa, message, responseCode);
        }
    }

    private static ResponseClass sendGetRequestAndRead(URL asteroidsUrl) throws IOException {
        HttpURLConnection con = sendGetRequest(asteroidsUrl);
        Reader streamReader;

        if (con.getResponseCode() > 299) {
            streamReader = new InputStreamReader(con.getErrorStream());
        } else {
            streamReader = new InputStreamReader(con.getInputStream());
        }
        return readResponseFromConnection(streamReader, con.getResponseMessage(), con.getResponseCode());
    }

    private static class ResponseClass {
        private final String response;
        private final String message;
        private final int code;

        public ResponseClass(String response, String message, int code) {
            this.response = response;
            this.message = message;
            this.code = code;
        }

        public String getResponse() {
            return response;
        }

        public String getMessage() {
            return message;
        }

        public int getCode() {
            return code;
        }
    }
}