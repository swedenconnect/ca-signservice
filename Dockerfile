FROM openjdk:11-jre

ADD target/signservice-ca-1.0.0-SNAPSHOT.jar /app.jar
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
