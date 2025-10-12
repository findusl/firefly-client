# syntax=docker/dockerfile:1
FROM eclipse-temurin:21

WORKDIR /app

COPY . /app

RUN chmod +x gradlew

COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
