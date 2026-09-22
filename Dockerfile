FROM maven:3.9.13-eclipse-temurin-25 AS build
LABEL authors="Team anonyms"
WORKDIR /app
COPY . .

RUN mvn clean package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/athletelink.jar /app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]