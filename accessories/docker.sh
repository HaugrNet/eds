#!/bin/bash

readonly tmp_proxy=${http_proxy//http:\/\//}
readonly bare_proxy=${tmp_proxy//\//}

if [[ "${bare_proxy}" != "" ]]; then
    echo "Acquire::http::Proxy \"http://${bare_proxy}/\";" > docker/apt.conf
    echo "use_proxy = on" > docker/wgetrc
    echo "http_proxy = ${http_proxy}" >> docker/wgetrc
    echo "https_proxy = ${http_proxy}" >> docker/wgetrc
else
    echo "" > docker/apt.conf
    echo "use_proxy = off" > docker/wgetrc
fi

docker build --build-arg HTTP_PROXY="${bare_proxy}" --build-arg HTTPS_PROXY="${bare_proxy}" -t cws1 -f docker/Dockerfile .
