package com.my.hostipal.utils;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.util.*;

@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "ai:chat:memory:";
    private static final Duration TTL = Duration.ofHours(24);
    private static final int MAX_MESSAGES = 50;

    private final StringRedisTemplate redisTemplate;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length()))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> values = redisTemplate.opsForList().range(key, 0, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::parseMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        redisTemplate.delete(key);

        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<String> values = messages.stream()
                .map(m -> m.getMessageType().name() + "||" + m.getText())
                .toList();

        redisTemplate.opsForList().rightPushAll(key, values);
        redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    private Message parseMessage(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|\\|", 2);
        if (parts.length < 2) {
            return null;
        }
        MessageType type;
        try {
            type = MessageType.valueOf(parts[0]);
        } catch (Exception e) {
            return null;
        }
        String content = parts[1];
        return switch (type) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            default -> new UserMessage(content);
        };
    }
}
