#!/usr/bin/env bash
git checkout deploy
git pull
docker-compose stop main
sbt docker
docker-compose up -d main