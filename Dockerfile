FROM eclipse-temurin:25-jre
WORKDIR /app
COPY main/orbien-server-app/target/orbien-server.jar /app/orbien-server.jar
COPY main/orbien-server-app/src/main/resources/orbien-server.toml /app/
COPY main/orbien-server-app/src/main/resources/application*.yml /app/config/
COPY main/orbien-server-app/src/main/resources/logback-spring.xml /app/config/
ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar orbien-server.jar -c orbien-server.toml"]
