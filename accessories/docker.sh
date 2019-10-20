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

echo "Start the Docker container first time as follows:"
echo "$ docker run --name cws-1.1 -d -p <PORT>:8080 cws"
echo "replace the <PORT> with what is needed."
echo "Once started first time, it can always be stopped and started"
echo "with the following Docker commands:"
echo "$ docker start cws-1.1"
echo "$ docker stop cws-1.1"
echo
echo "Simple check to see if your Docker image works:"
echo "$ curl --silent --header \"Content-Type: application/json\" --request POST \"http://localhost:<PORT>/cws/api/version\""
