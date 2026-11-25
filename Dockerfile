FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre
RUN useradd --system --no-create-home gateway
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER gateway
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
