FROM amazoncorretto:17-alpine

WORKDIR /app

# Create user
RUN addgroup -S spring && adduser -S spring -G spring

# Create data dir and give ownership to spring
RUN mkdir -p /app/data && chown -R spring:spring /app/data

# Copy the JAR and set ownership
COPY --chown=spring:spring target/*.jar app.jar

# Switch to the non-root user
USER spring:spring

# Expose the port
EXPOSE 8080

#Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
