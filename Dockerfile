# ===============================
# Build Stage
# ===============================
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copia i file Gradle per sfruttare la cache delle dipendenze
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Scarica le dipendenze
RUN gradle dependencies --no-daemon || true

# Copia il codice sorgente
COPY src ./src

# Build del JAR (escludendo i test per velocità)
RUN gradle bootJar --no-daemon -x test

# ===============================
# Runtime Stage
# ===============================
FROM eclipse-temurin:17-jdk-jammy

# Installiamo solo curl, necessario per il Healthcheck di Docker
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia il JAR generato nello stage precedente
COPY --from=build /app/build/libs/*.jar app.jar

# Creazione utente non-root per sicurezza
RUN useradd -r -u 1001 appuser && \
    chown -R appuser:appuser /app
USER appuser

# Espone la porta del backend
EXPOSE 8080

# Health check (Ora funzionerà perché abbiamo installato curl sopra)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Avvio dell'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]