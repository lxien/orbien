FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY main/etps/target/etps-*.jar /app/etps.jar
COPY main/etps/src/main/resources/etps.toml /app/
COPY main/etps/src/main/resources/application*.yml /app/config/
COPY main/etps/src/main/resources/logback-spring.xml /app/config/
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar etps.jar -c etps.toml"]
