FROM ghcr.io/graalvm/native-image-community:25 AS build

COPY --from=maven:3.9.13-eclipse-temurin-25 /usr/share/maven /usr/share/maven
RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

WORKDIR /app
COPY pom.xml ./
COPY src/main ./src/main
RUN mvn -B -ntp -Pnative -DskipTests native:compile

FROM debian:bookworm-slim
WORKDIR /app
RUN mkdir -p /logs && chown 10001:10001 /logs
COPY --from=build /app/target/core-service /app/target/*.so /app/
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/core-service"]
