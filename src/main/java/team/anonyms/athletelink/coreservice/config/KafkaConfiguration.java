package team.anonyms.athletelink.coreservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaConfiguration {
    @Bean
    public NewTopic userChangesTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.userChanges()).partitions(3).replicas(1).build();
    }
}
