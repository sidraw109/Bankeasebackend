# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies resolve in their own layer so source edits don't re-download the world.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# The base Ubuntu image ships neither curl nor wget; the health check below needs one.
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --home /app --shell /usr/sbin/nologin bankease
COPY --from=build /build/target/bankease-monolith-1.0.0.jar app.jar
USER bankease

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    PORT="8080"

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
    CMD wget -qO- "http://127.0.0.1:${PORT}/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
