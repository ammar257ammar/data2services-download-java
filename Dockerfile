FROM maven:3-jdk-8-alpine as maven

# Only runs if pom.xml changes. To avoid downloading dependencies everytime.
COPY pom.xml .
RUN mvn verify clean --fail-never

COPY src/ ./src/
RUN mvn package 


# Final running image
FROM openjdk:8-jre-alpine

LABEL maintainer "Vincent Emonet <vincent.emonet@gmail.com>"

COPY --from=maven target/data2services-download-java-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app/data2services-download-java.jar

ENTRYPOINT ["java","-jar","/app/data2services-download-java.jar"]
CMD ["-h"]