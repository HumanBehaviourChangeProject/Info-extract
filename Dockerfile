# OpenJDK JRE for Java 8, with Tomcat 9.0.30 server
FROM tomcat:9.0.30-jdk8-openjdk
# Tomcat server port
EXPOSE 8080
# places the app in the Tomcat app folder (at the root of the server)
RUN rm -rf /usr/local/tomcat/webapps/*
COPY ./target/hbcpIE-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
# set the max size of the JVM heap
ENV CATALINA_OPTS -Xms512m -Xmx8g
# run catalina (Tomcat's servlet container)
CMD ["catalina.sh", "run"]