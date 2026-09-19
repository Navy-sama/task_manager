# syntax=docker/dockerfile:1

## ---- Build stage -----------------------------------------------------------
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# mvnw's distributionType=only-script: it downloads the Maven binary itself with curl/wget,
# neither of which ships in the base image.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Cache dependencies in their own layer: only re-resolved when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests, Spotless check and the JaCoCo gate already ran in CI (`verify`); the image build only packages.
RUN ./mvnw -B -q -DskipTests -Dspotless.apply.skip=true package \
    && java -Djarmode=tools -jar target/*.jar extract --layers --destination extracted

## ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build --chown=spring:spring /workspace/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/application/ ./

USER spring:spring

# application.properties already reads server.port=${PORT:8080}; Cloud Run injects PORT itself
# (defaulting to 8080), so no extra JAVA_OPTS/SERVER_PORT mapping is needed here.
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
