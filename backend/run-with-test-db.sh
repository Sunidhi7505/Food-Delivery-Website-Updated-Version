#!/usr/bin/env bash
# Runs the backend on http://localhost:8080 against a throw-away in-memory MongoDB test DB
# (localhost:27017, db "foodify_test"). Data is lost when the process stops.
# Real values come from env vars when set; otherwise empty test defaults are used.
# Without MAIL_USERNAME, mail is switched off and emails (e.g. reset links) are printed in the console.
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home}"
export MONGO_URI="${MONGO_URI:-mongodb://localhost:27017/foodify_test}"
export RAZORPAY_KEY_ID="${RAZORPAY_KEY_ID:-}" RAZORPAY_SECRET="${RAZORPAY_SECRET:-}"
export MAIL_FROM="${MAIL_FROM:-}" MAIL_USERNAME="${MAIL_USERNAME:-}" MAIL_PASSWORD="${MAIL_PASSWORD:-}"
MAIL_ARG=""
[ -z "$MAIL_USERNAME" ] && MAIL_ARG="--foody.mail.enabled=false"
CP_FILE="$(mktemp)"
./mvnw -q test-compile dependency:build-classpath -Dmdep.outputFile="$CP_FILE" -Dmdep.includeScope=test
exec "$JAVA_HOME/bin/java" -cp "target/test-classes:target/classes:$(cat "$CP_FILE")" com.foody.TestDbApplication \
  --foody.frontend-url=http://localhost:5500 $MAIL_ARG
