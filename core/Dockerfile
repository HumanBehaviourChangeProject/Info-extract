# Stage 1: Maven JDK 8 image to build the project
FROM maven:3.6.3-openjdk-8 AS maven-builder
ARG ENTITIES_FOR_PREDICTION="src/main/resources/data/jsons/All_annotations_512papers_05March20.json"
ENV ENTITIES_FOR_PREDICTION=${ENTITIES_FOR_PREDICTION}
# Copy whole folder
COPY . /tmp/
# Copy the annotation JSON file for prediction in the resources
COPY ./${ENTITIES_FOR_PREDICTION} /tmp/src/main/resources/data/jsons/
WORKDIR /tmp/
ENV MAVEN_OPTS -Xms512m -Xmx8g
RUN mvn clean install
# generate embeddings and training data for prediction
WORKDIR /tmp/src/main/resources/scripts/
RUN chmod +x ./predictionresources4api.sh
RUN ./predictionresources4api.sh 100 10 0.1 0.9

# Stage 2: OpenJDK JRE for Java 8, with Tomcat 9.0.30 server to run the springboot API
FROM tomcat:9.0.30-jdk8-openjdk
# Tomcat server port
EXPOSE 8080
# places the app in the Tomcat app folder (at the root of the server)
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=maven-builder /tmp/target/hbcp-core*.war /usr/local/tomcat/webapps/ROOT.war
RUN rm -f /usr/local/tomcat/conf/context.xml
COPY context.xml /usr/local/tomcat/conf/
# copy the prediction resources
COPY --from=maven-builder /tmp/resources /prediction/resources
# set the max size of the JVM heap
ENV CATALINA_OPTS -Xms512m -Xmx8g

# Finalise
# run catalina (Tomcat's servlet container)
CMD ["catalina.sh", "run"]