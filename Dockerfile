# STAGE 1: Build the application
FROM gradle:8.5-jdk21-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew bootJar --no-daemon

# STAGE 2: Run the application
FROM eclipse-temurin:21-jdk-alpine
EXPOSE 9000
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
