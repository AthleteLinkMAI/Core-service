package team.anonyms.athletelink.requestservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.kafka.topics")
public record KafkaTopicProperties(
        String userChanges,
        String gameJoinRequests,
        String activityRequestsChanges,
        String gameReplies,
        String requestNotifications
) {
}
