#!/bin/sh
while true
do
    echo "HTTP/1.1 200 OK\n\npong" | netcat -l 8080
done
