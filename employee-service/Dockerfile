# ---- Stage 1: build the jar with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Stage 2: small runtime image (JRE only, no Maven, no source code) ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Fixed numeric UID so Kubernetes can verify the container does not run as root
RUN addgroup -S -g 1001 app && adduser -S -u 1001 -G app app \
    && mkdir -p /app/data /app/uploads && chown -R 1001:1001 /app
COPY --from=build --chown=1001:1001 /app/target/*.jar app.jar
USER 1001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
