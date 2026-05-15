FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY restaurant-service/pom.xml .
COPY restaurant-service/src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8082} -jar /app/app.jar"]
