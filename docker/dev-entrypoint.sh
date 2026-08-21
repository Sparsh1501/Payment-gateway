#!/bin/sh
set -eu

BUILD_DIR=/tmp/payment-gateway
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
cp -R /workspace/. "$BUILD_DIR/"
cd "$BUILD_DIR"

npm ci --prefix frontend
npm run build --prefix frontend
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -R frontend/dist/. src/main/resources/static/

./gradlew clean bootJar -x test --no-daemon
exec java -XX:MaxRAMPercentage=75.0 -jar build/libs/*.jar
