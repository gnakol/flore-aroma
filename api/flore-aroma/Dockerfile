# Utilise une image OpenJDK 21 (compatible avec Spring Boot 3.5.7)
FROM openjdk:21-jdk-slim

# Définis le répertoire de travail dans le conteneur
WORKDIR /app

# Copie le JAR depuis target/
COPY target/flore-aroma-0.0.1-SNAPSHOT.jar app.jar

# Expose le port 9000
EXPOSE 9000

# Lance l'application Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
