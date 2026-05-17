FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn/ .mvn
COPY mvnw mvnw

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

COPY src ./src

RUN ./mvnw clean package -DskipTests --no-transfer-progress


FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="cart-service"
LABEL org.opencontainers.image.description="Atlas Commerce cart microservice"
LABEL org.opencontainers.image.version="1.0.0"

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring

EXPOSE 8085

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]