#!/bin/sh
APP_HOME=$(cd "$(dirname "$0")" && pwd)
JAVA_HOME=/home/node/android-dev/jdk-17.0.2
exec "$JAVA_HOME/bin/java" \
    -Dorg.gradle.appname=gradlew \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
