package com.example.prompta.controller;

import com.example.prompta.service.LLMService;
import com.example.prompta.service.SearchService;
import com.example.prompta.service.SortSourceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SearchService searchService;
    private final SortSourceService sortSourceService;
    private final LLMService llmService;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> data = mapper.readValue(payload, new TypeReference<>() {});
        String query = data.get("query");

        List<Map<String, String>> searchResults = searchService.webSearch(query);
        List<Map<String, Object>> sortedResults = sortSourceService.sortSources(query, searchResults);

        session.sendMessage(new TextMessage(mapper.writeValueAsString(
                Map.of("type", "search_result", "data", sortedResults)
        )));

        for (String chunk : llmService.generateResponse(query, sortedResults)) {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(
                    Map.of("type", "content", "data", chunk)
            )));
            Thread.sleep(100); // Simulate delay
        }

        session.close();
    }
}
