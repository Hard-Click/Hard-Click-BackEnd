package com.wanted.backend.domain.chat.infrastructure.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ChatBroadcastListenerConfig {

    @Bean
    public RedisMessageListenerContainer chatBroadcastListenerContainer(RedisConnectionFactory connectionFactory,
                                                                          RedisChatBroadcastSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(RedisChatBroadcastPublisher.CHANNEL));
        return container;
    }
}
