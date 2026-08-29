# ---- build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Dependencies resolve in their own layer so a source-only change does not re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- run ------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Never run the API as root.
RUN groupadd --system revexa && useradd --system --gid revexa --home /app revexa

COPY --from=build /workspace/target/*.jar app.jar
RUN chown -R revexa:revexa /app
USER revexa

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
  CMD ["sh", "-c", "wget -q --spider http://localhost:8080/actuator/health/readiness || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
