#!/bin/bash

ENROLLMENT_URL=https://orderer.cnabs.com:passwd@fabric-ca-server:7054
ORDERER_HOST=orderer.cnabs.com
ORDERER_HOME=/data/client/crypto/ordererOrganizations/cnabs.com/orderers/orderer.cnabs.com
TLSDIR=$ORDERER_HOME/tls
ORG_ADMIN_CERT=/data/client/crypto/ordererOrganizations/cnabs.com/msp/admincerts/cert.pem

# Enroll to get orderer's TLS cert (using the "tls" profile)
fabric-ca-client enroll -d --enrollment.profile tls -u $ENROLLMENT_URL -M /tmp/tls --csr.hosts $ORDERER_HOST

mkdir -p $TLSDIR

cp /tmp/tls/keystore/* $ORDERER_GENERAL_TLS_PRIVATEKEY
cp /tmp/tls/signcerts/* $ORDERER_GENERAL_TLS_CERTIFICATE 
rm -rf /tmp/tls

# Enroll again to get the orderer's enrollment certificate (default profile)
fabric-ca-client enroll -d -u $ENROLLMENT_URL -M $ORDERER_GENERAL_LOCALMSPDIR

mkdir $ORDERER_GENERAL_LOCALMSPDIR/tlscacerts
cp $ORDERER_GENERAL_LOCALMSPDIR/cacerts/*  $ORDERER_GENERAL_LOCALMSPDIR/tlscacerts

dstDir=$ORDERER_GENERAL_LOCALMSPDIR/admincerts
mkdir -p $dstDir
cp $ORG_ADMIN_CERT $dstDir

orderer