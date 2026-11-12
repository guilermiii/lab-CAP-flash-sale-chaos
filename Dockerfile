# Estágio de Build do Backend Java 21
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw clean package -DskipTests

# Estágio de Execução
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
