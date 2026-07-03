# Stage 1: Build the Maven application
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the entire workspace (containing both backend and frontend)
COPY . .

# Run maven package inside backend
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
