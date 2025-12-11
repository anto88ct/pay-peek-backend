# ===============================
# Build Stage
# ===============================
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copy Gradle files first for better caching
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon -x test

# ===============================
# Runtime Stage
# ===============================
FROM eclipse-temurin:17-jdk-jammy

# Install Tesseract OCR with Italian and English language packs
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-ita \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create non-root user for security
RUN useradd -r -u 1001 appuser && \
    chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
