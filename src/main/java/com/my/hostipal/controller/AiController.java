package com.my.hostipal.controller;

import com.my.hostipal.pojo.entity.Result;
import com.my.hostipal.service.AiService;
import com.my.hostipal.utils.CurrentHolder;
import org.apache.ibatis.annotations.Delete;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private AiService aiService;

    // 聊天接口
    @GetMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> chat(String prompt,String conversationId) {
        // 从当前线程中获取用户id
        Integer userId = CurrentHolder.getCurrentId();
        String finalConversationId = userId + ":" + conversationId;
        Flux<String> content = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalConversationId))
                .user(prompt)
                .stream()
                .content();
        return content;
    }

    //保存会话记录id
    @GetMapping("/history/save")
    public void save(String conversationId) {
        aiService.save(conversationId);
    }

    //从数据库中获取会话id列表
    @GetMapping("/history/list")
    public Result list() {
        List<String> conversationIds = aiService.list();
        return Result.success(conversationIds);
    }

    //通过会话id获取会话记录列表
    @GetMapping("/history/messages")
    public Result Messages(String conversationId){
        // 从当前线程中获取用户id
        Integer userId = CurrentHolder.getCurrentId();
        String finalConversationId = userId + ":" + conversationId;
        List<Message> messages = aiService.getMessages(finalConversationId);
        return Result.success(messages);
    }

    @Delete("/history/delete")
    public void delete(String conversationId){
        aiService.delete(conversationId);
    }
}
