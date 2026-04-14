# Multi-stage build for the Spring Boot backend.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests \
    && mv target/photovault-backend-*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ffmpeg
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseG1GC"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
