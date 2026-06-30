# ============================================
# STAGE 1: Build
# ============================================
FROM eclipse-temurin:24-jdk-alpine AS builder

WORKDIR /app

# Maven wrapper copy karo
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dependencies download karo (cache layer)
RUN ./mvnw dependency:go-offline -B

# Source code copy karo
COPY src ./src

# Build karo (skip tests for faster build)
RUN ./mvnw clean package -DskipTests -B

# ============================================
# STAGE 2: Runtime
# ============================================
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# JAR copy karo builder se
COPY --from=builder /app/target/*.jar app.jar

# Port expose karo
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# Run karo
ENTRYPOINT ["java", "-jar", "app.jar"]