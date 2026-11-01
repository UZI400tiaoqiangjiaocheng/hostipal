package com.my.hostipal.service.impl;

import com.my.hostipal.mapper.AiMapper;
import com.my.hostipal.pojo.entity.Conversation;
import com.my.hostipal.service.AiService;
import com.my.hostipal.utils.CurrentHolder;
import com.my.hostipal.utils.RedisChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiserviceImpl implements AiService {

    @Autowired
    private AiMapper aiMapper;
    @Autowired
    private RedisChatMemoryRepository redisChatMemoryRepository;

    // 保存会话记录id
    @Override
    public void save(String conversationId) {
        Conversation conversation = new Conversation();
        Long userId = CurrentHolder.getCurrentId().longValue();
        conversation.setUserId(userId);
        conversation.setConversationId(conversationId);
        aiMapper.insert(conversation);
    }

    //查询用户所有会话记录id
    @Override
    public List<String> list() {
        Long userId = CurrentHolder.getCurrentId().longValue();
        List<String> conversationIds = aiMapper.selectConversationId(userId);
        return conversationIds;
    }

    // 查询会话记录
    @Override
    public List<Message> getMessages(String conversationId) {
        //获取用户id
        List<Message> messages = redisChatMemoryRepository.findByConversationId(conversationId);
        return messages;
    }
}
