#!/bin/bash

fabric-ca-server init -b admin:adminpw

if [ ! -d /data/client/tls/ ]; then
  mkdir -p /data/client/tls/
fi

cp $FABRIC_CA_SERVER_HOME/ca-cert.pem /data/client/tls/ca-cert.pem

echo 'root ca server starting...'

fabric-ca-server start