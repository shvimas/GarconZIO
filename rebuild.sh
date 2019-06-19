#!/usr/bin/env bash
git checkout aws-current
git pull
docker-compose stop main
sbt docker
docker-compose up --build -d main