#!/bin/sh

if [[ -z "${JAVA_HOME}" ]]; then
    echo "Please set JAVA_HOME environment variable"
    exit 1
fi

if [[ -z "${JAVA_LINK}" ]]; then
    echo "Please set JAVA_LINK environment variable - point it to jlink executable"
    exit 1
fi

./gradlew clean build

RED='\033[0;31;47m'
GREEN='\033[0;32m'
NC='\033[0m'

if [ -d "${BUILDPAL_DEPS}" ]; then
    echo "${GREEN}Starting buildpal server node on localhost...${NC}\n"

    java --module-path $BUILDPAL_DEPS:auth/build/libs/:core/build/libs/:db/build/libs/:node/build/libs/:oci/build/libs/:workspace/build/libs/:$JAVA_HOME/jmods \
         -m io.buildpal.node/io.buildpal.node.NodeLauncher run --conf $BUILDPAL_DEPS/localhost/config.json io.buildpal.node.Node

else
    echo "${RED}Make sure you point 'BUILDPAL_DEPS' to the folder containing buildpal dependencies (jars).${NC}\n"
fi
