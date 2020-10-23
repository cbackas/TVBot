FROM maven:3.6.3-adoptopenjdk-14 AS MAVEN_TOOL_CHAIN
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn package

FROM adoptopenjdk/openjdk13:alpine
COPY --from=MAVEN_TOOL_CHAIN /tmp/target/TVBot-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app/TVBot.jar
WORKDIR /app
CMD ["java", "-jar", "TVBot.jar"]