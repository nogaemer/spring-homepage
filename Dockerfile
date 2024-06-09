# Use the Eclipse Temurin image for the build stage
FROM eclipse-temurin:20-jdk AS build

# Copy the project files to the container
COPY . .

# Build the application
RUN ./gradlew bootJar --no-daemon

# Use the same Eclipse Temurin image for the final stage
FROM eclipse-temurin:20-jdk

# Expose the port the application runs on
EXPOSE 8080

# Copy the built JAR file from the build stage
COPY --from=build /build/libs/spring-homepage-0.0.1-SNAPSHOT.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]