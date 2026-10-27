FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN bash mvnw dependency:go-offline

COPY src src

RUN bash mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

# Usuario no-root: reutiliza el UID/GID 1000 si ya existe en la imagen base
# (p. ej. ubuntu en temurin reciente) para no romper el build.
RUN (getent group 1000 >/dev/null 2>&1 || groupadd -g 1000 appgroup) \
 && (id -u 1000 >/dev/null 2>&1 || useradd -u 1000 -g 1000 -r -s /usr/sbin/nologin appuser) \
 && chown -R 1000:1000 /app

COPY --from=builder /app/target/*.jar app.jar
RUN chown 1000:1000 app.jar

USER 1000

EXPOSE 3004

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:3004/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
