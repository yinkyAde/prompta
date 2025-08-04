package com.example.prompta.service;

import com.example.prompta.config.PropsReader;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PropsReader propsReader;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, String>> webSearch(String query) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String url = "https://api.tavily.com/search";

            // Build the request body as a Map
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("auto_parameters", true); // or false if you prefer
            requestBody.put("include_raw_content", true);
            requestBody.put("include_answer", true);
            requestBody.put("max_results", 5); // adjust as needed

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + propsReader.getTavilyApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("results")) {
                return results;
            }

            List<Map<String, Object>> searchResults = (List<Map<String, Object>>) responseBody.get("results");

            for (Map<String, Object> result : searchResults) {
                String content = (String) result.getOrDefault("content", "");
                Map<String, String> formatted = Map.of(
                        "title", (String) result.getOrDefault("title", ""),
                        "url", (String) result.getOrDefault("url", ""),
                        "content", content
                );
                results.add(formatted);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
