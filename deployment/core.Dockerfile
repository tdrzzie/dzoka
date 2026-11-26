FROM eclipse-temurin@sha256:da0f0d242bec39e3fe5c1ac4c415840f03be9cef848c243d723166708e02148c
WORKDIR /app
COPY --chown=10001:10001 target/core-layer/BOOT-INF /app/BOOT-INF
COPY --chown=10001:10001 LICENSE dependencies.lock.json /app/
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-XX:+ExitOnOutOfMemoryError","-cp","/app/BOOT-INF/classes:/app/BOOT-INF/lib/*"]
CMD ["org.dzoka.CoreApplication"]
