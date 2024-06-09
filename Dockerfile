FROM ubuntu:latest AS build

RUN apt-get update
RUN apt-get install openjdk-20-jdk -y
COPY . .

RUN ./gradlew bootJar --no-daemon

FROM openjdk:20-jdk-slim

EXPOSE 8080

COPY --from=build /build/libs/spring-homepage-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]