package com.quickbite.payment.notification.config;

import com.quickbite.payment.notification.constants.AppConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@EnableRabbit
@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(AppConstants.RABBIT_NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable(AppConstants.RABBIT_NOTIFICATION_EMAIL_QUEUE).build();
    }

    @Bean
    public Queue smsNotificationQueue() {
        return QueueBuilder.durable(AppConstants.RABBIT_NOTIFICATION_SMS_QUEUE).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(AppConstants.RABBIT_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(AppConstants.RABBIT_NOTIFICATION_KEY);
    }

    @Bean
    public Binding emailNotificationBinding(Queue emailNotificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(emailNotificationQueue)
                .to(notificationExchange)
                .with(AppConstants.RABBIT_NOTIFICATION_EMAIL_KEY);
    }

    @Bean
    public Binding smsNotificationBinding(Queue smsNotificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(smsNotificationQueue)
                .to(notificationExchange)
                .with(AppConstants.RABBIT_NOTIFICATION_SMS_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }
}
