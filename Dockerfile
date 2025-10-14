# Multi-stage build for Kotlin/Ktor application
FROM gradle:8-jdk21 AS build

WORKDIR /app
COPY . .

# Build the application with explicit tasks
RUN ./gradlew clean build --no-daemon --stacktrace

# Runtime stage using eclipse-temurin (supports AMD64 + ARM)
FROM eclipse-temurin:21-jre-jammy

USER 1000:1000

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar happyrow-core.jar

# Copy application configuration
COPY --from=build /app/src/main/resources/application.conf /app/application.conf

EXPOSE 8080

# JVM tuned for Raspberry Pi memory (also works fine on Render)
ENTRYPOINT ["java"]
CMD ["-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-Djava.security.egd=file:/dev/./urandom", "-jar", "happyrow-core.jar"]