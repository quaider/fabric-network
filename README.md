# fabric-network

## Generate files

### Generate certificates
```shell
cryptogen generate --config=./crypto-config.yaml
```

### Generating Orderer Genesis block
```shell
# 这里不能 加 -channelID cnabs 被日志上的警告 坑了很久很久...
configtxgen -profile OneOrgOrdererGenesis -outputBlock ./channel-artifacts/genesis.block
```

###  Generating channel configuration transaction 'channel.tx'

``` shell
configtxgen -profile OneOrgChannel -outputCreateChannelTx ./channel-artifacts/channel.tx -channelID cnabs
```

### Generating anchor peer update for Org1MSP
``` shell
configtxgen -profile OneOrgChannel -outputAnchorPeersUpdate ./channel-artifacts/Org1MSPanchors.tx -channelID cnabs -asOrg Org1MSP
```

### Generating anchor peer update for Org2MSP
``` shell
configtxgen -profile OneOrgChannel -outputAnchorPeersUpdate ./channel-artifacts/Org2MSPanchors.tx -channelID cnabs -asOrg Org2MSP
```

## Channel

### Create channel

是否一定是Admin用户的签名？，应该是跟policy有关

```shell
export CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/msp/users/Admin@org1.cnabs.com/msp
echo $CORE_PEER_MSPCONFIGPATH

peer channel create -o orderer.cnabs.com:7050 -c cnabs -f /etc/hyperledger/configtx/channel.tx
```

### Peer join channel
```shell
peer channel join -b cnabs.block
```

### Update Anchor peer node
```shell
peer channel update -o orderer.cnabs.com:7050 -c cnabs -f /etc/hyperledger/configtx/Org1MSPanchors.tx --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/cnabs.com/orderers/orderer.cnabs.com/msp/tlscacerts/tlsca.cnabs.com-cert.pem
```

## Chaincode

### Install chaincode
``` shell
export CC_SRC_PATH=/opt/gopath/src/github.com/go
peer chaincode install -n mycc -v 1.0.0 -l java -p ${CC_SRC_PATH}
peer chaincode install -n mycc -v 1.0.0 -p ${CC_SRC_PATH}
```

### Instantiate chaincode
实例化时需要指定背书策略 -P, 不指定则使用默认策略
``` shell
peer chaincode instantiate -o orderer.cnabs.com:7050 -C cnabs -n mycc -l golang -v 1.0.0 -c '{"Args":["init","a","100","b","200"]}' -P "AND ('Org1MSP.peer')"

peer chaincode instantiate -o orderer.example.com:7050 -C $CHANNEL_NAME -n mycc -l ${LANGUAGE} -v ${VERSION} -c '{"Args":["init","a","100","b","200"]}' -P "AND ('Org1MSP.peer','Org2MSP.peer')" >&log.txt
```


## 安装过程的错误
* 链码安装报错如下
  ```
  Error: error getting chaincode code mycc: <go, [env]>: failed with error: "exec: not started"
  ```
  原因是安装chaincode时偷懒了，在peer容器中进行了，应该在CLI的容器中进行安装操作
* 生成区块的时候不要加 -channelID，尽管警告需要加，但加上后背书策略始终无法通过，去掉-channelID就可以了

