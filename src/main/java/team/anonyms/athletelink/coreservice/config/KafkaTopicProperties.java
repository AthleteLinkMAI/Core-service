package team.anonyms.athletelink.coreservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka.topics")
public record KafkaTopicProperties(
        String userChanges,
        String gameSubmitRounds,
        String gameCloseRequests,
        String gameReplies,
        String requestNotifications
) {
}
