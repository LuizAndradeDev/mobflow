# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


# Runtime stage
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN groupadd --system mobflow \
    && useradd --system --gid mobflow --home-dir /app mobflow \
    && chown -R mobflow:mobflow /app

EXPOSE 8080

USER mobflow

ENTRYPOINT ["java", "-jar", "app.jar"]
