# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml ./
COPY src/main ./src/main
RUN mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 10001 infra && adduser -D -H -u 10001 -G infra infra
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/backend-infra-ti-0.0.1-SNAPSHOT.jar ./app.jar
ENV SERVER_PORT=8080 JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/datacenters || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
