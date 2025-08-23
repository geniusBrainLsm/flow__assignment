# Build stage
FROM openjdk:17-jdk-slim AS build

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /app/uploads /app/logs

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
