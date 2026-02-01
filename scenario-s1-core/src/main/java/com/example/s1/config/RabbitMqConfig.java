package com.example.s1.config;

import com.example.s1.messaging.OrderEventConsumer;
import com.example.s1.messaging.OrderEventPublisher;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for order messaging.
 */
@Configuration
public class RabbitMqConfig {

    /**
     * Creates the order exchange.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(OrderEventPublisher.EXCHANGE);
    }

    /**
     * Creates the order created queue.
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(OrderEventConsumer.QUEUE).build();
    }

    /**
     * Binds the queue to the exchange.
     */
    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder
            .bind(orderCreatedQueue)
            .to(orderExchange)
            .with(OrderEventPublisher.ROUTING_KEY_CREATED);
    }

    /**
     * JSON message converter for RabbitMQ.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures RabbitTemplate with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
