FROM maven:3.9.13-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml ./
COPY src/main ./src/main

RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/athletelink.jar /app.jar
USER 10001:10001
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app.jar"]
