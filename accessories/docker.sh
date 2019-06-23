#!/bin/bash

readonly tmp_proxy=${http_proxy//http:\/\//}
readonly proxy=${tmp_proxy//\//}

if [[ "${proxy}" != "" ]]; then
    echo "Acquire::http::Proxy \"http://${proxy}/\";" > docker/apt.conf
    echo "use_proxy = on" > docker/wgetrc
    echo "http_proxy = http://${proxy}/" >> docker/wgetrc
    echo "https_proxy = http://${proxy}/" >> docker/wgetrc
else
    echo "" > docker/apt.conf
    echo "use_proxy = off" > docker/wgetrc
fi

cp ../cws-wildfly/target/cws.war docker
docker build --build-arg HTTP_PROXY="${proxy}" --build-arg HTTPS_PROXY="${proxy}" -t cws -f docker/Dockerfile .
rm docker/apt.conf docker/wgetrc docker/cws.war

# Run as follows:
# $ docker run -d -p 8080:8080 cws
