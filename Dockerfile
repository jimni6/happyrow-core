# Multi-stage build for Kotlin/Ktor application
FROM gradle:8-jdk21 AS build

WORKDIR /app
COPY . .

# Build the application with explicit tasks
RUN ./gradlew clean build --no-daemon --stacktrace

# Runtime stage using distroless Java 21
FROM gcr.io/distroless/java21-debian12

USER nonroot

WORKDIR /app

# Copy the built JAR from build stage (use wildcard to handle any JAR name)
COPY --from=build /app/build/libs/*.jar happyrow-core.jar

# Copy application configuration
COPY --from=build /app/src/main/resources/application.conf /app/application.conf

ENTRYPOINT ["/usr/bin/java"]

CMD ["-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-Djava.security.egd=file:/dev/./urandom", "-jar", "happyrow-core.jar"]

EXPOSE 8080