#!/usr/bin/env bash
sbt clean universal:packageBin

cat target/universal/version.properties

docker ps -a | awk '{ print $1,$2,$12 }' | grep tamediadigital/dgraphtest | awk '{print $1 }' | xargs -I {} docker stop {} | xargs -I {} docker rm {}
docker images -qf "dangling=true" | xargs -I {} docker rmi {}

docker build -t tamediadigital/dgraphtest .
docker run --env-file ./env.list  -d --name tamediadigital-dgraphtest -p 9090:9090 tamediadigital/dgraphtest
# docker exec -it tamediadigital-dgraphtest bash
docker logs -f tamediadigital-dgraphtest
