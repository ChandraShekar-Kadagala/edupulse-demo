# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Ensure mvnw has execute permissions
RUN chmod +x ./mvnw
# Download dependencies
RUN ./mvnw dependency:resolve
# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Create a minimal production image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# The PORT variable is injected by Render, application.yml maps it to server.port
ENV PORT=8080
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
