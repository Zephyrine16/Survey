# Step 1: Build the application
FROM container-registry.oracle.com/java/openjdk:25 AS build
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package "-Dmaven.test.skip=true"

# Step 2: Run the application
FROM container-registry.oracle.com/java/openjdk:25
WORKDIR /app
COPY --from=build /app/target/Survey-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]