package com.photovault.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String MEDIA_PROCESSING_QUEUE = "media.processing.queue";
    public static final String THUMBNAIL_GENERATION_QUEUE = "thumbnail.generation.queue";
    public static final String VIDEO_TRANSCODING_QUEUE = "video.transcoding.queue";
    public static final String MEDIA_EXCHANGE = "media.exchange";

    @Bean
    public Queue mediaProcessingQueue() {
        return QueueBuilder.durable(MEDIA_PROCESSING_QUEUE)
            .withArgument("x-message-ttl", 3600000) // 1 hour TTL
            .build();
    }

    @Bean
    public Queue thumbnailGenerationQueue() {
        return QueueBuilder.durable(THUMBNAIL_GENERATION_QUEUE)
            .withArgument("x-priority", 10) // High priority
            .build();
    }

    @Bean
    public Queue videoTranscodingQueue() {
        return QueueBuilder.durable(VIDEO_TRANSCODING_QUEUE)
            .withArgument("x-message-ttl", 7200000) // 2 hour TTL
            .build();
    }

    @Bean
    public TopicExchange mediaExchange() {
        return new TopicExchange(MEDIA_EXCHANGE);
    }

    @Bean
    public Binding mediaProcessingBinding() {
        return BindingBuilder
            .bind(mediaProcessingQueue())
            .to(mediaExchange())
            .with("media.uploaded");
    }

    @Bean
    public Binding thumbnailGenerationBinding() {
        return BindingBuilder
            .bind(thumbnailGenerationQueue())
            .to(mediaExchange())
            .with("media.thumbnail");
    }

    @Bean
    public Binding videoTranscodingBinding() {
        return BindingBuilder
            .bind(videoTranscodingQueue())
            .to(mediaExchange())
            .with("media.video");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
