#!/bin/sh
rm -f tpid

nohup java -Xms1024m -Xmx2048m -jar app.jar > ./console.log 2>&1 &

echo $! > tpid
