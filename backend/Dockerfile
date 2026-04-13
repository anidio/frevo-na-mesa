# Estágio 1: Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# AJUSTE: Copia os arquivos de dentro da pasta backend para a raiz /app do container
COPY backend/mvnw .
COPY backend/.mvn .mvn
COPY backend/pom.xml .

# Dá permissão e baixa as dependências
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# AJUSTE: Copia a pasta src que está dentro de backend
COPY backend/src ./src

# Gera o JAR
RUN ./mvnw clean package -DskipTests

# Estágio 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# O JAR é gerado na pasta target do estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]