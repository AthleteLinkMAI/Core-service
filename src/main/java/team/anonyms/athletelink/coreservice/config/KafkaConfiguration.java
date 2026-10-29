package team.anonyms.athletelink.coreservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
@EnableScheduling
public class KafkaConfiguration {
    @Bean
    public NewTopic userChangesTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.userChanges()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic gameSubmitRoundsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.gameSubmitRounds()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic gameCloseRequestsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.gameCloseRequests()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic gameRepliesTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.gameReplies()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic requestNotificationsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.requestNotifications()).partitions(3).replicas(1).build();
    }
}
