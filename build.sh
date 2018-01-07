#!/bin/sh

cleanDockerContainers() {
    echo "${GREEN}Cleaning docker containers...${NC}\n"

    docker stop $(docker ps -a -q)
    docker rm $(docker ps -a -q)

    echo "${GREEN}Cleaned docker containers.${NC}\n"
}


cleanBuildpalImageAndVolume() {
    echo "${GREEN}Cleaning buildpal latest image and volumes...${NC}\n"

    docker rmi -f buildpal/buildpal:latest
    docker volume rm buildpal-data
    #docker volume rm buildpal-system

    echo "${GREEN}Cleaned buildpal image and volumes.${NC}\n"
}

buildDockerImage() {
    echo "${GREEN}Buidling buildpal docker image...${NC}\n"

    rm -rf ${BUILDPAL_SUPPORT}/web
    cp -R ${BUILDPAL_UI}/web ${BUILDPAL_SUPPORT}/web

    rm ${BUILDPAL_DEPS}/auth-${BUILDPAL_VERSION}.jar
    rm ${BUILDPAL_DEPS}/core-${BUILDPAL_VERSION}.jar
    rm ${BUILDPAL_DEPS}/db-${BUILDPAL_VERSION}.jar
    rm ${BUILDPAL_DEPS}/node-${BUILDPAL_VERSION}.jar
    rm ${BUILDPAL_DEPS}/oci-${BUILDPAL_VERSION}.jar
    rm ${BUILDPAL_DEPS}/workspace-${BUILDPAL_VERSION}.jar

    cp ${SCRIPT_DIR}/auth/build/libs/auth-${BUILDPAL_VERSION}.jar ${BUILDPAL_DEPS}/auth-${BUILDPAL_VERSION}.jar
    cp ${SCRIPT_DIR}/core/build/libs/core-${BUILDPAL_VERSION}.jar ${BUILDPAL_DEPS}/core-${BUILDPAL_VERSION}.jar
    cp ${SCRIPT_DIR}/db/build/libs/db-${BUILDPAL_VERSION}.jar ${BUILDPAL_DEPS}/db-${BUILDPAL_VERSION}.jar
    cp ${SCRIPT_DIR}/node/build/libs/node-${BUILDPAL_VERSION}.jar ${BUILDPAL_DEPS}/node-${BUILDPAL_VERSION}.jar
    cp ${SCRIPT_DIR}/oci/build/libs/oci-${BUILDPAL_VERSION}.jar ${BUILDPAL_DEPS}/oci-${BUILDPAL_VERSION}.jar
    cp ${SCRIPT_DIR}/workspace/build/libs/workspace-${BUILDPAL_VERSION}.jar ${BUILDPAL_DEPS}/workspace-${BUILDPAL_VERSION}.jar

    rm -rf ${BUILDPAL_SUPPORT}/libs
    cp -R ${BUILDPAL_DEPS} ${BUILDPAL_SUPPORT}/libs/

    cd ${BUILDPAL_SUPPORT}

    docker build -t buildpal/buildpal:latest .

    echo "\nStarting docker container for buildpal..."

    docker volume create buildpal-data
    #docker volume create buildpal-system

    docker run -d \
               --name buildpal \
               -v /var/run/docker.sock:/var/run/docker.sock \
               -v buildpal-system:/buildpal/system \
               -v buildpal-data:/buildpal/data \
               -p 8080:8080 -p 50001:50001 buildpal/buildpal

    echo "${GREEN}Started docker container for buildpal!${NC}\n\n"

    docker logs -f buildpal

}

buildWebUI() {
    echo "${GREEN}Compiling buildpal web UI...${NC}\n"
    cd ${BUILDPAL_UI}
    ojet build --release
    cd ${SCRIPT_DIR}
}

buildPlatform() {
    echo "${GREEN}Compiling buildpal platform...${NC}\n"
    cd ${SCRIPT_DIR}
    ./gradlew clean build
}

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ -z "${JAVA_HOME}" ]]; then
    echo "Please set JAVA_HOME environment variable"
    exit 1
fi

if [[ -z "${JAVA_LINK}" ]]; then
    echo "Please set JAVA_LINK environment variable - point it to jlink executable"
    exit 1
fi

if [[ -z "${BUILDPAL_DEPS}" ]]; then
    echo "Using default BUILDPAL_DEPS value"
    BUILDPAL_DEPS=../buildpal-deps
fi

if [[ -z "${BUILDPAL_SUPPORT}" ]]; then
    echo "Using default BUILDPAL_SUPPORT value"
    BUILDPAL_SUPPORT=../buildpal-support
fi

if [[ -z "${BUILDPAL_UI}" ]]; then
    echo "Using default BUILDPAL_UI value"
    BUILDPAL_UI=../../buildpal-ui
fi



RED='\033[0;31;47m'
GREEN='\033[0;32m'
NC='\033[0m'

BUILDPAL_VERSION="2.0-SNAPSHOT"

if [ -d "${BUILDPAL_DEPS}" ] && [ -d "${BUILDPAL_SUPPORT}" ]; then

    buildPlatform;
    buildWebUI;

    cleanDockerContainers;
    cleanBuildpalImageAndVolume;
    buildDockerImage;

else
    echo "${RED}Make sure you point 'BUILDPAL_DEPS' and 'BUILDPAL_SUPPORT' to valid folders.${NC}\n"
fi
