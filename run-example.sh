#!/bin/sh

export PORT=3000
export NREPL_PORT=7000
export DATABASE_URL="postgresql://HOST/PORT?user=USER&password=PASSWORD"
export MULTIPLEX__SITE_TITLE="multiplex"
export MULTIPLEX__SITE_SCHEME=":https"
export MULTIPLEX__SITE_URL="multiplex.example.org"
export MULTIPLEX__SITE_PORT=443
export MULTIPLEX__SITE_THEME="default"
export MULTIPLEX__ASSETS_URL="https://static.multiplex.example.org"
export MULTIPLEX__CONTENT_REL_PATH="/dump"
export MULTIPLEX__CONTENT_ABS_PATH="/srv/www/multiplex/dump"

exec java -jar target/uberjar/multiplex.jar

