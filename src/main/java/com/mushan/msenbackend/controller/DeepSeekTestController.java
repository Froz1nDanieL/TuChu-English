package com.mushan.msenbackend.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 官方文档示例的测试Controller，验证连通性
 */
@RestController
public class DeepSeekTestController {

    // 方式1：官方推荐的高阶ChatClient，作文批改首选
    private final ChatClient chatClient;

    // 方式2：底层DeepSeekChatModel，细粒度控制用
    private final DeepSeekChatModel deepSeekChatModel;

    // 自动注入，Spring AI官方自动配置已帮我们创建好Bean
    @Autowired
    public DeepSeekTestController(ChatClient.Builder chatClientBuilder, DeepSeekChatModel deepSeekChatModel) {
        this.chatClient = chatClientBuilder.build();
        this.deepSeekChatModel = deepSeekChatModel;
    }

    // 最简测试接口：浏览器访问 http://localhost:8080/ai/test?message=hello
    @GetMapping("/ai/test")
    public Map<String, String> testChat(@RequestParam(defaultValue = "用一句话介绍你自己") String message) {
        String result = chatClient.prompt()
                .user(message)
                .call()
                .content();
        return Map.of("result", result);
    }

    // 底层模型测试接口
    @GetMapping("/ai/test/model")
    public Map<String, String> testModel(@RequestParam(defaultValue = "你好") String message) {
        String result = deepSeekChatModel.call(message);
        return Map.of("result", result);
    }
}