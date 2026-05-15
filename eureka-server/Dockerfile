FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY eureka-server/pom.xml .
COPY eureka-server/src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8761} -jar /app/app.jar"]
