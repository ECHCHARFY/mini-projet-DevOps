# Use an OpenJDK base image
FROM openjdk:17-jdk-alpine

# Create an app directory
WORKDIR /app

# Copy the compiled JAR and JDBC driver into the container
COPY SimpleServer.jar /app/
COPY lib/postgresql-42.5.0.jar /app/lib/

# Set the CLASSPATH to include the JDBC driver
ENV CLASSPATH=/app/SimpleServer.jar:/app/lib/postgresql-42.5.0.jar

# Expose the port the app runs on
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-cp", "/app/SimpleServer.jar:/app/lib/postgresql-42.5.0.jar", "SimpleServer"]

