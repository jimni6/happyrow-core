# ===== BUILD STAGE =====
# Use an ARM-compatible Gradle + JDK image
FROM eclipse-temurin:21-jdk-jammy AS build

# Install Gradle manually (optional: you can also use gradle:8-jdk21-alpine if it supports arm64)
RUN apt-get update && apt-get install -y wget unzip && \
    wget https://services.gradle.org/distributions/gradle-8.7-bin.zip -O gradle.zip && \
    unzip gradle.zip -d /opt && \
    ln -s /opt/gradle-8.7/bin/gradle /usr/bin/gradle

WORKDIR /app
COPY . .

# Build the application
RUN ./gradlew clean build --no-daemon --stacktrace

# ===== RUNTIME STAGE =====
# Use a small, ARM-compatible JRE base image
FROM eclipse-temurin:21-jre-jammy

USER 1000:1000
WORKDIR /app

# Copy built jar
COPY --from=build /app/build/libs/*.jar /app/happyrow-core.jar

# Copy config if needed
COPY --from=build /app/src/main/resources/application.conf /app/application.conf

EXPOSE 8080

# JVM tuned for Raspberry Pi memory
ENTRYPOINT ["java"]
CMD ["-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/happyrow-core.jar"]