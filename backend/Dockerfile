# Estágio 1: Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copia apenas o necessário para baixar as dependências (melhora o cache)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copia o código e gera o JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Estágio 2: Runtime (Gera uma imagem muito menor e mais segura)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia apenas o JAR gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Comando otimizado para rodar o JAR
ENTRYPOINT ["java", "-jar", "app.jar"]