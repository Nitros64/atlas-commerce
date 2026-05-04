FROM maven:3.9.13-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -g 10001 spring && \
    useradd -u 10001 -g spring -s /usr/sbin/nologin -M spring

COPY --from=builder /app/target/*.jar app.jar

RUN chown -R 10001:10001 /app

USER 10001:10001

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
#ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]