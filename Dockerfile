FROM eclipse-temurin:17-jdk-jammy AS base

# Build Stage
FROM base AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# Run Stage
FROM base AS runner
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]