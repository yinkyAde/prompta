package com.example.prompta.controller;

import com.example.prompta.model.ChatRequest;
import com.example.prompta.service.LLMService;
import com.example.prompta.service.SearchService;
import com.example.prompta.service.SortSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SearchService searchService;
    private final SortSourceService sortSourceService;
    private final LLMService llmService;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest body) {
        var searchResults = searchService.webSearch(body.getQuery());
        var sorted = sortSourceService.sortSources(body.getQuery(), searchResults);
        var response = llmService.generateResponse(body.getQuery(), sorted);
        return ResponseEntity.ok(response);
    }
}
