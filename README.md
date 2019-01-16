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

通道创建后，peer节点会获取到一个channel的创世区块(由order发放)，该区块用于后续peer加入通道。

### Peer join channel
需要说明的是组织类的其他节点并没有从order获取channel的创世区块(如 cnabs.block)，此时是没有办法加入通道的，因此必须先获取区块（copy或从order Fetch）

``` shell
# 获取区块 peer channel fetch <newest|oldest|config|(number)> [outputfile] [flags]
peer channel fetch 0 -o orderer.cnabs.com:7050 -c cnabs
# 在本地生成了 cnabs_0.block，不太确定 fetch参数是否正确，但测试中节点还是加入了通道
```

将peer节点加入通道(前提是已经获取了区块)

```shell
peer channel join -b cnabs.block
```

### Update Anchor peer node

```shell
peer channel update -o orderer.cnabs.com:7050 -c cnabs -f /etc/hyperledger/configtx/Org1MSPanchors.tx 

#--tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/cnabs.com/orderers/orderer.cnabs.com/msp/tlscacerts/tlsca.cnabs.com-cert.pem
```

## Chaincode

下面的步骤需要在CLI容器中进行了

### Install chaincode

这里也是使用的Admin用户的msp，特别需要注意的是GO语言的链码，不需要/opt/gopath/src全路径，只需要到github.com/chaincode就行了。
非go语言的链码，则必须需要全路径，否则安装会报错（报错信息也没有指明是找不到链码，排错相当麻烦），如java需要的则需要全路径，到源码所在根目录

``` shell
# GO环境直接是 github.com/chaincode/go，不需要全路径
export CC_SRC_PATH=github.com/chaincode/
#for java: export CC_SRC_PATH=/opt/gopath/src/github.com/chaincode/
peer chaincode install -n mycc -v 1.0 -l java -p ${CC_SRC_PATH}
peer chaincode install -n mycc -v 1.0 -p ${CC_SRC_PATH}
```

### Instantiate chaincode
实例化时需要指定背书策略 -P, 不指定则使用默认策略
``` shell
peer chaincode instantiate -o orderer.cnabs.com:7050 -C cnabs -n mycc -v 1.0 -l java -c '{"Args":["init"]}' -P "OR ('Org1MSP.peer','Org1MSP.admin','Org1MSP.member')"
#--peerAddresses peer0.org1.cnabs.com:7051

peer chaincode instantiate -o orderer.example.com:7050 -C $CHANNEL_NAME -n mycc -l ${LANGUAGE} -v ${VERSION} -c '{"Args":["init","a","100","b","200"]}' -P "AND ('Org1MSP.peer','Org2MSP.peer')" >&log.txt
```

### Invoke chaincode

#### Query
``` shell
peer chaincode query -C cnabs -n mycc -c '{"Args":["query","user1"]}'
```

#### Invoke

``` shell
peer chaincode invoke -o orderer.cnabs.com:7050 -C cnabs -n mycc -c '{"Args":["invoke","user1","4"]}'
```

### Upgrade chaincode

升级链码之前需要先对其进行安装，不然是找不到待升级的链码的

``` shell
# 安装新版本的链码
peer chaincode install -n mycc -v 1.1 -l java -p ${CC_SRC_PATH}

# 将链码升级到新版本
# 改过程稍花时间，因为要启动新的链码容器
peer chaincode upgrade -o orderer.cnabs.com:7050 -C cnabs -n mycc -v 1.1 -c '{"Args":["init"]}' -P "OR ('Org1MSP.peer','Org1MSP.admin','Org1MSP.member')"
# --tls $CORE_PEER_TLS_ENABLED --cafile $ORDERER_CA
```

## 端口说明

- 7051：peer gRPC 服务监听端口
- 7052：一般用在peer与链码docker容器通信（cli）
- 7053：peer 事件服务端口，一般用来监听数据，用在HubEvent

## 调试模式

正常情况下，每次更改链码都需要将链码安装到peer节点，升级链码，调试时只能依靠打日志的方式进行问题的排查，严重影响
开发效率和开发体验。

fabric提供了一个开发模式的开关，给链码部署和调试提供了便利，虽然还是稍显繁琐，但问题有所改善。

开启链码调试功能的核心配置 是在 peer 节点的容器启动命令上 附加 --peer-chaincodedev=true，同时需暴露容器的
7052端口给外部环境（chaincodes server的端口默认为7052，映射到宿主机可用端口即可，这里仍然以7052示例）

``` yaml
peer0.org1.cnabs.com:
  container_name: peer0.org1.cnabs.com
  image: hyperledger/fabric-peer
  # ...
  # 这个就是开启调试模式的关键所在了，注意必须开启一个端口 7052，用于外部链接
  command: peer node start --peer-chaincodedev=true
  ports: 
    - 7051:7051
    - 7052:7052
    - 7053:7053
```

这样 fabric链码调试模式就准备就绪，由于我用的是java，所以需要在 idea 中设置2个关键环境变量，用于指定链码的标识和链码服务器，其中链码标识为 `name:version` 形式。

``` property
CORE_PEER_ADDRESS=192.168.8.131:7052
CORE_CHAINCODE_ID_NAME=mycc:1.0
```

然后在 idea 中正常运行或调试，当在cli中执行链码调用时，链码会切换到本地编译器环境中，这样便可以断点调试了

需要注意的是，在链码还未进行安装时，需要先手动安装、实例化链码（这一步是必须的，一般第一次才需要）

## 安装过程的错误
* 链码安装报错如下
  ```
  Error: error getting chaincode code mycc: <go, [env]>: failed with error: "exec: not started"
  ```
  原因是安装chaincode时偷懒了，在peer容器中进行了，应该在CLI的容器中进行安装操作
* 生成区块的时候不要加 -channelID，尽管警告需要加，但加上后背书策略始终无法通过，去掉-channelID就可以了

