# Multi-stage build for Kotlin/Ktor application
FROM gradle:8-jdk21 AS build

WORKDIR /app
COPY . .

# Build the application
RUN ./gradlew build --no-daemon

# Runtime stage using distroless Java 21
FROM gcr.io/distroless/java21-debian12

USER nonroot

WORKDIR /app

# Copy the built JAR from build stage (Gradle creates JAR without version suffix)
COPY --from=build /app/build/libs/happyrow-core.jar happyrow-core.jar

# Copy application configuration
COPY --from=build /app/src/main/resources/application.conf /app/application.conf

ENTRYPOINT ["/usr/bin/java"]

CMD ["-jar", "happyrow-core.jar"]

EXPOSE 8080