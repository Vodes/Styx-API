FROM vodes/styx-baseimage:latest

COPY ./app.jar .

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]