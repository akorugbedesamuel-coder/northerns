FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
COPY usa ./usa
RUN mvn clean package -DskipTests -B

FROM openjdk:17.0.1-jdk-slim
COPY --from=build /app/target/project.northerntrust-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
