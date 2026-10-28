# Receita de build usada SOMENTE pelo Render para publicar a API (não é preciso usar Docker no computador).
# Etapa 1: compila o projeto com Maven.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Etapa 2: imagem leve só com o Java para rodar o .jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/tapetes-kalke-api-1.0.0.jar app.jar
# O plano grátis do Render tem 512 MB de memória; limita o Java para caber.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
