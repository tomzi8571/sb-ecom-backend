# Multi-stage Dockerfile for building a Spring Boot application
# Uses Maven to build and produces a minimized runtime image

# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
COPY src/ src/
COPY mvnw* ./
COPY images/ images/

# Use Maven wrapper if available, otherwise fallback to system mvn in CI
RUN ./mvnw -B -ntp -DskipTests package -DskipITs

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/*.jar
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

# Expose default port (Render uses PORT env var)
ENV PORT=8080
EXPOSE 8080

# Ensure the JVM picks up the PORT env var as server.port
ENTRYPOINT ["sh","-c","exec java -Dserver.port=${PORT} -jar /app/app.jar"]
