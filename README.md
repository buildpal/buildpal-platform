# Buildpal - docker native continuous integration server
## Introduction
Builpal is an open source continuous integration server with first class support for pipelines. Built on top of the excellent reactive toolkit vertx, Buildpal runs your pipelines on docker containers. Buildpal is a good alternative for folks who prefer code over various configuration files and formats. You define pipelines in JavaScript (Java support is on its way). You can run multiple phases of a pipeline in parallel and cut down on your build times. Your pipeline can also build a docker image as the final outcome of a successful build.  

## Project status
This project is currently in experimental stage. We are trying to experiment with the new Java Platform Module System (JPMS) and Vert.x to design and develop a modern, responsive, resilient and message driven continuous integration (CI) system. Git and Perforce are the two currently supported source control systems.

## Getting started
Buildpal runs on docker and co-ordinates builds by spinning additional containers. Make sure you have a version of docker that supports creation of named data volumes (version 1.13 or higher). 

Assuming you have docker installed, run the following command for a quick start:
```bash
docker volume create buildpal-data && \
docker run -d --name buildpal -v /var/run/docker.sock:/var/run/docker.sock -v buildpal-data:/buildpal/data -p 8080:8080 -p 55555:55555 buildpal/buildpal
```
The initial admin password gets printed to the console. Look at the log by running the following command:
```bash
docker logs buildpal
```

### Creating a pipeline
* For the default installation, you can access the app at http://localhost:8080
* Login using your admin credentials. User name is "admin" (without the quotes)
* Navigate to the repositories tab to define your repository
* Once you create a repository, navigate to the pipelines tab
* Now use the javascript editor to define your pipeline that runs combination of parallel or sequential phases.

For setup and usage refer to our user guide.

## Development
Builpal platform is developed on top of [vert.x](http://vertx.io/) using JDK 9 and Java Platform Module System.

To start contributing, do the following:
* Install the latest version of JDK 9.
* Use git to clone the source code from https://github.com/buildpal/buildpal-platform.git
* Buildal uses gradle. You can compile the code by running the gradle wrapper "gradlew.[sh|bat] clean build"
