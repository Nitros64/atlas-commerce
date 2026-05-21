FROM maven:3.9.13-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN addgroup --system spring && adduser --system --uid 1001 spring --ingroup spring

USER 1001

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]