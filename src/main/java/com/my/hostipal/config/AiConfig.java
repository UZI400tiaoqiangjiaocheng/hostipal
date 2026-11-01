package com.my.hostipal.config;

import com.my.hostipal.aitools.DeparmentTools;
import com.my.hostipal.utils.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Autowired
    private DeparmentTools deparmentTools;

    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)        // 最多保留 20 条上下文
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("你是一个医院的智能助手,能够根据用户的病情,推荐合适的科室。\n" +
                        "1.推荐科室\n" +
                        "调用查询科室的工具并根据用户对病情的描述，在查询到的科室中推荐科室，如果没有相应的科室则对用户说抱歉暂时没有与你相符合的科室\n" +
                        "\n" +
                        "查询科室工具：DeparmentTools.queryDepartment()" )
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build()) //配置日志记录器
                .defaultTools(deparmentTools)
                .build();
    }
}
