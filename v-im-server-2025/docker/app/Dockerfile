# Build stage: compile the v-im-server-ry-plus module
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Pre-copy the Maven descriptor files to leverage caching for dependencies
COPY pom.xml ./
COPY v-im-server/pom.xml ./v-im-server/
COPY v-im-server-ry-plus/pom.xml ./v-im-server-ry-plus/

# Download dependencies required for the target module
RUN mvn -B -pl v-im-server-ry-plus -am dependency:go-offline

# Copy the remaining project sources and build the runnable jar
COPY . ./
RUN mvn -B -pl v-im-server-ry-plus -am package -DskipTests

# Runtime stage: slim JRE image for running the application
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai

# Copy the packaged jar produced in the build stage
COPY --from=builder /workspace/v-im-server-ry-plus/target/v-im-server-ry-plus.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
