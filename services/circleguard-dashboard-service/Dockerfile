FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY services/circleguard-dashboard-service services/circleguard-dashboard-service
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew :services:circleguard-dashboard-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/services/circleguard-dashboard-service/build/libs/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
# trigger deploy