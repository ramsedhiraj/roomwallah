# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root system user and group
RUN addgroup -S spring && adduser -S spring -G spring

# Copy jar from build stage and assign owner permissions
COPY --from=build --chown=spring:spring /app/target/backend-1.0.0.jar app.jar

# Expose server port
EXPOSE 8080

# Configure wget-based actuator healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Drop root privileges and switch user context
USER spring:spring

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
