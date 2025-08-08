package com.example.prompta.service;

import com.example.prompta.config.PropsReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LLMService {

    private final PropsReader propsReader;
    private final RestTemplate restTemplate = new RestTemplate();

    //GEMINI
//    public List<String> generateResponse(String query, List<Map<String, Object>> searchResults) {
//        StringBuilder context = new StringBuilder();
//
//        for (int i = 0; i < searchResults.size(); i++) {
//            Map<String, Object> result = searchResults.get(i);
//            context.append("Source ").append(i + 1)
//                    .append(" (").append(result.get("url")).append("):\n")
//                    .append(result.get("content")).append("\n\n");
//        }
//
//        String prompt = """
//            Please provide a comprehensive, detailed, well-cited accurate response using the below context.
//            Think and reason deeply. Ensure it answers the query the user is asking. Do not use your knowledge until it is absolutely necessary.
//
//            Context from web search:
//            """ + context + "\nQuery: " + query;
//
//        // Build the JSON payload
//        Map<String, Object> requestBody = Map.of(
//                "contents", List.of(
//                        Map.of("parts", List.of(Map.of("text", prompt)))
//                )
//        );
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(propsReader.getGeminiApiKey());
//
//        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.exchange(
//                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
//                    HttpMethod.POST,
//                    requestEntity,
//                    Map.class
//            );
//
//            Map responseBody = response.getBody();
//            if (responseBody == null) return List.of("No response");
//
//            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
//            if (candidates == null || candidates.isEmpty()) return List.of("No content candidates");
//
//            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
//            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
//
//            return parts.stream().map(p -> p.get("text")).collect(Collectors.toList());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return List.of("Error contacting Gemini API");
//        }
//    }

    //CHAT GPT
//    public List<String> generateResponse(String query, List<Map<String, Object>> searchResults) {
//        StringBuilder context = new StringBuilder();
//
//        for (int i = 0; i < searchResults.size(); i++) {
//            Map<String, Object> result = searchResults.get(i);
//            context.append("Source ").append(i + 1)
//                    .append(" (").append(result.get("url")).append("):\n")
//                    .append(result.get("content")).append("\n\n");
//        }
//
//        String prompt = """
//        Please provide a comprehensive, detailed, well-cited accurate response using the below context.
//        Think and reason deeply. Ensure it answers the query the user is asking. Do not use your knowledge until it is absolutely necessary.
//
//        Context from web search:
//        """ + context + "\nQuery: " + query;
//
//        // Build the payload according to /v1/responses format
//        Map<String, Object> requestBody = Map.of(
//                "model", "o4-mini",
//                "reasoning", Map.of("effort", "medium"),
//                "input", List.of(
//                        Map.of(
//                                "role", "user",
//                                "content", prompt
//                        )
//                )
//        );
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setBearerAuth(propsReader.getOpenAiApiKey());
//
//        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.exchange(
//                    "https://api.openai.com/v1/responses",
//                    HttpMethod.POST,
//                    requestEntity,
//                    Map.class
//            );
//
//            Map<String, Object> responseBody = response.getBody();
//            if (responseBody == null) return List.of("No response from OpenAI.");
//
//            // Extract output
//            List<Map<String, Object>> outputList = (List<Map<String, Object>>) responseBody.get("output");
//            if (outputList == null || outputList.isEmpty()) return List.of("No content in output.");
//
//            return outputList.stream()
//                    .map(entry -> (String) entry.get("content"))
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return List.of("Error contacting OpenAI /v1/responses API");
//        }
//    }


    // HUGGING FACE
    public List<String> generateResponse(String query, List<Map<String, Object>> searchResults) {
        ObjectMapper mapper = new ObjectMapper();

        // Combine the search context
        StringBuilder context = new StringBuilder();
        searchResults.forEach(res -> {
            String content = Objects.toString(res.get("content"), "").trim();
            if (!content.isEmpty()) {
                context.append(content).append("\n\n");
            }
        });

        // Build the messages array like OpenAI chat format
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "You are a knowledgeable assistant. Use the provided context to answer clearly, with citations."),
                Map.of("role", "user", "content", "Context:\n" + context + "\nQuestion: " + query)
        );

        Map<String, Object> requestBody = Map.of(
                "model", "Qwen/Qwen3-4B-Instruct-2507:nscale",
                "messages", messages,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(propsReader.getHuggingFaceApiKey()); // Your HF token

        try {
            String json = mapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            String url = "https://router.huggingface.co/v1/chat/completions";
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = mapper.readTree(resp.getBody());
            if (root.has("choices") && root.get("choices").isArray()) {
                JsonNode choice = root.get("choices").get(0);
                String answer = choice.path("message").path("content").asText();
                return List.of(answer);
            } else {
                return List.of("Unexpected response format: " + root.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of("Error during inference: " + e.getMessage());
        }
    }

}
