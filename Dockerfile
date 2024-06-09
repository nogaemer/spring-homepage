FROM ubuntu:latest AS build

# Update package list and install dependencies
RUN apt-get update && apt-get install -y openjdk-20-jdk --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Copy the project files to the container
COPY . .

# Build the application
RUN ./gradlew bootJar --no-daemon

# Use a lightweight image for the final stage
FROM openjdk:20-jdk-slim

# Expose the port the application runs on
EXPOSE 8080

# Copy the built JAR file from the build stage
COPY --from=build /build/libs/spring-homepage-0.0.1-SNAPSHOT.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
