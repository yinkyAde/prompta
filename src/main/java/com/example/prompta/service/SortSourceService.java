package com.example.prompta.service;

import com.example.prompta.config.PropsReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SortSourceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final PropsReader propsReader;

    public List<Map<String, Object>> sortSources(String query, List<Map<String, String>> results) {
        return results.stream()
                .map(res -> {
                    double similarity = computeSimilarity(query, res.get("content"));

                    if (similarity > 0.3) {
                        Map<String, Object> mapped = new HashMap<>(res);
                        mapped.put("relevance_score", similarity);
                        return mapped;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(
                        (double) b.get("relevance_score"),
                        (double) a.get("relevance_score")
                ))
                .toList();
    }

    private double computeSimilarity(String query, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(propsReader.getHuggingFaceApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> inputs = Map.of(
                "inputs", Map.of(
                        "source_sentence", query,
                        "sentences", List.of(content)
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(inputs, headers);

        ResponseEntity<List> response = restTemplate.exchange(
                "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2",
                HttpMethod.POST,
                request,
                List.class
        );

        List<Double> similarities = response.getBody();
        return similarities.get(0);
    }

    private double computeSimilarity(List<Double> a, List<Double> b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

