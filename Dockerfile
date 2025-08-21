# Usa uma imagem base oficial do Java 17
FROM openjdk:17-jdk-slim

# Define o diretório de trabalho dentro do contêiner
WORKDIR /app

# Dá permissão de execução para o script do Maven Wrapper
RUN chmod +x ./mvnw

# Baixa todas as dependências do projeto
RUN ./mvnw dependency:go-offline

# Copia o resto do código fonte do seu projeto
COPY src ./src

# Executa o build do Maven para gerar o arquivo .jar
RUN mvn clean package -DskipTests

# Expõe a porta 8080 para que o mundo exterior possa acessá-la
EXPOSE 8080

# O comando para iniciar a aplicação quando o contêiner rodar
ENTRYPOINT ["sh", "-c", "java -jar target/*.jar"]