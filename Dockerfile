# Use the official gradle/Java 15 image to create a build artifact.
# https://hub.docker.com/_/gradle
FROM gradle:6.8.1-jdk15 AS build-env

# Set the working directory to /app
WORKDIR /app
# Copy the build.gradle file to download dependencies
COPY build.gradle ./
# Copy local code to the container image.
COPY src ./src

# Download dependencies and build a release artifact.
RUN gradle assemble

# Use AdoptOpenJDK for base image.
# It's important to use OpenJDK 8u191 or above that has container support enabled.
# https://hub.docker.com/r/adoptopenjdk/openjdk15
# https://docs.docker.com/develop/develop-images/multistage-build/#use-multi-stage-builds
FROM adoptopenjdk/openjdk15:alpine-slim

# Copy the jar to the production image from the builder stage.
COPY --from=build-env /app/build/libs/*.jar /api-sales.jar

# Run the web service on container startup.
CMD ["java","-jar","/api-sales.jar"]