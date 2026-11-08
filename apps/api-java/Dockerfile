# Stage 1: Build and test with pinned toolchain versions.
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx768m -XX:ActiveProcessorCount=2"
# The suite belongs to CI, which runs it on every PR. A small production host can set
# RUN_TESTS=false to build the image from an already-validated commit without thrashing swap.
ARG RUN_TESTS=true
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src/ ./src/
RUN if [ "$RUN_TESTS" = "false" ]; then \
        mvn --batch-mode --no-transfer-progress clean package -DskipTests; \
    else \
        mvn --batch-mode --no-transfer-progress clean package; \
    fi

# Stage 2: minimal non-root runtime with curl for the actuator health check.
FROM eclipse-temurin:21.0.12.1_1-jre-noble AS runtime
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --no-create-home --shell /usr/sbin/nologin app \
    && mkdir -p /app \
    && chown app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /app/target/*.jar /app/app.jar
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
