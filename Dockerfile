FROM clojure:temurin-21-lein-bookworm-slim

RUN mkdir /app && \
	apt-get update && \
        apt-get install -y make curl jq && \
        apt-get clean && \
	apt-get autoclean

COPY target/uberjar/multiplex.jar /app

EXPOSE 3000
WORKDIR /app
CMD ["/opt/java/openjdk/bin/java","-Xms128m","-Xmx256m","-jar","multiplex.jar"]
