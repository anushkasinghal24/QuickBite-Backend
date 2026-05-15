FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY order-service/pom.xml .
COPY order-service/src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8085} -jar /app/app.jar"]
