FROM openjdk:21-jdk-slim

ADD target/signservice-ca-*.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

# Main web port
EXPOSE 8080
# HTTPS web port
EXPOSE 8443
# AJP port
EXPOSE 8009
# Management port
EXPOSE 8008
# Internal admin UI port
EXPOSE 8006
