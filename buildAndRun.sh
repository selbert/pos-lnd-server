#!/bin/sh
mvn clean package
docker-compose up -d --build
docker logs -f lnd-pos-server_ln-pos-server_1
