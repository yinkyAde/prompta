package com.example.prompta.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class PropsReader {

    @Value("${tavily.api.key}")
    private String tavilyApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${hugging.face.api.key}")
    private String huggingFaceApiKey;
}
