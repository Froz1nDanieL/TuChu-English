package com.mushan.msenbackend.service.impl;

import com.mushan.msenbackend.model.vo.EssayCorrectionResult;
import com.mushan.msenbackend.service.EssayCorrectionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 作文批改服务实现
 */
@Service
public class EssayCorrectionServiceImpl implements EssayCorrectionService {

    private final ChatClient essayCorrectorChat;
    private final BeanOutputConverter<EssayCorrectionResult> outputConverter;

    public EssayCorrectionServiceImpl(@Qualifier("essayCorrectorChat") ChatClient essayCorrectorChat) {
        this.essayCorrectorChat = essayCorrectorChat;
        this.outputConverter = new BeanOutputConverter<>(EssayCorrectionResult.class);
    }

    @Override
    public EssayCorrectionResult correctEssay(String essayContent, String topic) {
        String userPrompt = buildUserPrompt(essayContent, topic);
        
        String response = essayCorrectorChat.prompt()
                .user(userPrompt)
                .call()
                .content();
        
        return outputConverter.convert(response);
    }

    private String buildUserPrompt(String essayContent, String topic) {
        StringBuilder prompt = new StringBuilder();
        
        if (topic != null && !topic.isBlank()) {
            prompt.append("作文题目：").append(topic).append("\n\n");
        }
        
        prompt.append("作文内容：\n").append(essayContent);
        
        return prompt.toString();
    }
}
