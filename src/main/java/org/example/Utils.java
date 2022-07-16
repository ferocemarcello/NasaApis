package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class Utils {
    public static URL buildUrl(String host, Map<String, String> queryParams) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(host);
        for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            uriBuilder.addParameter(queryParam.getKey(), queryParam.getValue());
        }
        return uriBuilder.build().toURL();
    }
    public static String prettyIndentJsonString(String jsonString) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(String.valueOf(jsonString), Object.class));
    }
}
