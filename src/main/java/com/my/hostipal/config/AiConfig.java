package com.my.hostipal.config;

import com.openai.client.OpenAIClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个有帮助的助手。")
                .defaultAdvisors(new SimpleLoggerAdvisor()) //配置日志记录器
                .build();
    }
}
