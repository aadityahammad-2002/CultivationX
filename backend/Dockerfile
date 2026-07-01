# ============================================
# STAGE 1: Build
# ============================================
FROM eclipse-temurin:24-jdk-alpine AS builder

WORKDIR /app

# Maven install karo (mvnw ke bina)
RUN apk add --no-cache maven

# Source code copy karo
COPY pom.xml .
COPY src ./src

# Dependencies download karo
RUN mvn dependency:go-offline -B

# Build karo (skip tests)
RUN mvn clean package -DskipTests -B

# ============================================
# STAGE 2: Runtime
# ============================================
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
