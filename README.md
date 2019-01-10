# fabric-network

## Generate files

### Generate certificates
```shell
cryptogen generate --config=./crypto-config.yaml
```

### Generating Orderer Genesis block
```shell
configtxgen -profile OneOrgsOrdererGenesis -outputBlock ./channel-artifacts/genesis.block -channelID cnabs
```

###  Generating channel configuration transaction 'channel.tx'

``` shell
configtxgen -profile OneOrgsChannel -outputCreateChannelTx ./channel-artifacts/channel.tx -channelID cnabs
```

### Generating anchor peer update for Org1MSP
``` shell
configtxgen -profile OneOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org1MSPanchors.tx -channelID cnabs -asOrg Org1MSP
```


### Generating anchor peer update for Org2MSP
``` shell
configtxgen -profile OneOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org2MSPanchors.tx -channelID cnabs -asOrg Org2MSP
```

## Channel

### Create channel
```shell
peer channel create -o orderer.cnabs.com:7050 -c cnabs -f ./channel-artifacts/channel.tx
```

### Peer join channel
```shell
peer channel join -b cnabs.block
```

### Update Anchor peer node
```shell
peer channel update -o orderer.cnabs.com:7050 -c cnabs -f ./channel-artifacts/Org1MSPanchors.tx --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/cnabs.com/orderers/orderer.cnabs.com/msp/tlscacerts/tlsca.cnabs.com-cert.pem
```