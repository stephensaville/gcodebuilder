#!/bin/bash

set -euax

cd $(dirname $0)/..

BASE=$(pwd)
OUT=$BASE/out/production/gcodebuilder
DIST=$BASE/dist
DIST_LIB=$DIST/lib

PLATFORM=$(uname)
PLATFORM_DIR=$DIST/$PLATFORM

case $PLATFORM in
    Darwin)
        JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home
        ;;
    *)
        echo "Current platform not supported: $PLATFORM"
        exit 1
        ;;
esac

PACKR_JAR=$DIST/packr/*.jar
PACKR="$JAVA_HOME/bin/java -jar $PACKR_JAR"

export BASE OUT DIST DIST_LIB JAVA_HOME PACKR PLATFORM PLATFORM_DIR

mkdir -p $DIST_LIB

find $HOME/.m2/repository -name '*.jar' -exec cp '{}' $DIST_LIB ';'

$JAVA_HOME/bin/jar -cf $DIST_LIB/gcodebuilder.jar -C $OUT com -C $OUT log4j2.xml

exec $PLATFORM_DIR/distribute.sh
