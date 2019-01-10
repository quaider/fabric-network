# fabric-network

## Generate files

### Generate certificates
```shell
cryptogen generate --config=./crypto-config.yaml
```

### Generating Orderer Genesis block
```shell
configtxgen -profile TwoOrgsOrdererGenesis -outputBlock ./channel-artifacts/genesis.block
```

###  Generating channel configuration transaction 'channel.tx'

``` shell
configtxgen -profile TwoOrgsChannel -outputCreateChannelTx ./channel-artifacts/channel.tx -channelID cnabs
```

### Generating anchor peer update for Org1MSP
``` shell
configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org1MSPanchors.tx -channelID cnabs -asOrg Org1MSP
```


### Generating anchor peer update for Org2MSP
``` shell
configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org2MSPanchors.tx -channelID cnabs -asOrg Org2MSP
```

## Channel

### Create channel
```shell
peer channel create -o orderer.example.com:7050 -c cnabs -f ./channel-artifacts/channel.tx --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem
```

### Peer join channel
```shell
peer channel join -b cnabs.block
```

### Update Anchor peer node
```shell
peer channel update -o orderer.example.com:7050 -c cnabs -f ./channel-artifacts/Org1MSPanchors.tx --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem
```