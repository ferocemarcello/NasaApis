package org.example;

import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Util {
    public static URL buildUrl(String host, Map<String,String> queryParams) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(host);
        for(Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            uriBuilder.addParameter(queryParam.getKey(),queryParam.getValue());
        }
        return uriBuilder.build().toURL();
    }
}
