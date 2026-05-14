# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-23 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies (this caches them to speed up future builds)
RUN mvn dependency:go-offline -B
# Copy the actual source code and build the final executable jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight runtime image
FROM eclipse-temurin:23-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the port (Render will also pass this dynamically)
EXPOSE 8081

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
