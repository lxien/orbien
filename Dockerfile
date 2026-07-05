FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY main/orbiens/target/orbiens.jar /app/orbiens.jar
COPY main/orbiens/src/main/resources/orbiens.toml /app/
COPY main/orbiens/src/main/resources/application*.yml /app/config/
COPY main/orbiens/src/main/resources/logback-spring.xml /app/config/
ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar orbiens.jar -c orbiens.toml"]
