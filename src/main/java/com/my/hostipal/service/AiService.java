package com.my.hostipal.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface AiService {
    void save(String conversationId);

    List<String> list();

    List<Message> getMessages(String conversationId);
}
