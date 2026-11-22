# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --quiet dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --quiet -DskipTests package

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 1001 barkito && useradd --system --uid 1001 --gid barkito barkito
WORKDIR /app
COPY --from=build --chown=barkito:barkito /workspace/target/barkito-backend-*.jar app.jar
USER barkito
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
