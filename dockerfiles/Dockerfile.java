# syntax=docker/dockerfile:1
# Voz do Bairro - API Java (Spring Boot 3.4 / Java 21)
# Build (na raiz do projeto voz-do-bairro-java):
#   docker build -f dockerfiles/Dockerfile.java -t voz-do-bairro-java .

# ---------- Etapa 1: build ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependencias: so baixa de novo se o pom.xml mudar.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sem privilegios de root.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/voz-do-bairro-java.jar app.jar
RUN chown app:app app.jar
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"
EXPOSE 8080

# Variaveis obrigatorias em tempo de execucao: DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET.
# Opcionais: RABBITMQ_URL, OPENAI_API_KEY, OPENAI_BASE_URL, OPENAI_MODEL, CORS_ALLOWED_ORIGINS, PORT.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
