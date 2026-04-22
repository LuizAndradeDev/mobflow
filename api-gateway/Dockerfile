FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./
RUN apk add --no-cache curl
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B
RUN mkdir -p target/dependency && cd target/dependency && jar -xf ../*.jar

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S mobflow && adduser -S mobflow -G mobflow

WORKDIR /app

COPY --from=build /build/target/dependency /app
COPY --from=build /build/target/*.jar /app/app.jar

USER mobflow:mobflow

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
