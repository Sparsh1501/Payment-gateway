# ---------- Frontend build stage ----------
FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend

COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---------- Backend build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Cache dependencies first
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --version

# Build the application, including the compiled dashboard.
COPY src src
COPY --from=frontend-build /workspace/frontend/dist src/main/resources/static
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
