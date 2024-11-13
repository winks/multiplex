#!/usr/bin/bash
exec docker run --rm -it \
-v "${PWD}:/app" \
-v "./tmp/.m2:/root/.m2" \
clojure:temurin-21-lein-bookworm-slim bash

# run `lein uberjar` in /app
