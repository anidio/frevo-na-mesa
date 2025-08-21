# Usa uma imagem base oficial do Java 17
FROM openjdk:17-jdk-slim

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copia os arquivos necessários para o Maven Wrapper e o build
COPY ./src ./src
COPY ./mvnw ./mvnw
COPY ./pom.xml ./pom.xml
COPY ./.mvn ./.mvn

# Dá permissão de execução para o script do Maven Wrapper
RUN chmod +x ./mvnw

# Baixa todas as dependências do projeto
RUN ./mvnw dependency:go-offline

# Executa o build do Maven para gerar o arquivo .jar
RUN ./mvnw clean package -DskipTests

# Expõe a porta 8080 para que o mundo exterior possa acessá-la
EXPOSE 8080

# O comando para iniciar a aplicação quando o contêiner rodar
CMD ["java", "-jar", "target/*.jar"]