package com.mushan.msenbackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Spring AI 配置
 */
@Configuration
public class SpringAiConfig {

    @Value("classpath:/prompts/essay-corrector-system.md")
    private Resource systemPromptResource;

    /**
     * 创建 ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(systemPromptResource)
                .build();
    }
}