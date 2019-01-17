多组织分布式的部署需参考单机部署，目前的部署前提是CLI中含有所有节点的msp证书，锚节点配置更新交易文件，通道配置
交易文件、创世区块。

所有操作均在CLI容器中进行(除了msp生成和交易文件、创世区块等)

在进行操作时，请注意对应组织、节点环境变量的切换。这主要涉及到2个环境变量，它们分别是

`CORE_PEER_ADDRESS`、`CORE_PEER_LOCALMSPID`、`CORE_PEER_MSPCONFIGPATH`

------

## CLI 中进行 通道创建、加入通道、更新锚节点、链码安装、链码实例化等操作

## 通道创建

``` shell
# org1 msp环境(后面会切换)
export CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.cnabs.com/users/Admin@org1.cnabs.com/msp

peer channel create -o orderer.cnabs.com:7050 -c cnabs -f /etc/hyperledger/configtx/channel.tx
```

## cli 容器中的环境变量切换汇总

``` shell
export CORE_PEER_ADDRESS=peer{x}.org{y}.cnabs.com:{z}
export CORE_PEER_LOCALMSPID=Org{y}MSP

# export CORE_PEER_LOCALMSPID=Org1MSP
# export CORE_PEER_ADDRESS=peer0.org1.cnabs.com:7051
# export CORE_PEER_ADDRESS=peer1.org1.cnabs.com:7151

# export CORE_PEER_LOCALMSPID=Org2MSP
# export CORE_PEER_ADDRESS=peer0.org2.cnabs.com:8051
# export CORE_PEER_ADDRESS=peer1.org2.cnabs.com:8151

export CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.cnabs.com/users/Admin@org1.cnabs.com/msp

export CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.cnabs.com/users/Admin@org2.cnabs.com/msp

```

```shell
## 注意对应环境变量的切换
## 在所有 peer节点上执行(后期写成脚本时，循环加入)
peer channel join -b cnabs.block
```

### Update Anchor peer node of orgs

```shell
# 注意环境变量切换到org1
peer channel update -o orderer.cnabs.com:7050 -c cnabs -f /etc/hyperledger/configtx/Org1MSPanchors.tx

# 注意环境变量切换到org2
peer channel update -o orderer.cnabs.com:7050 -c cnabs -f /etc/hyperledger/configtx/Org2MSPanchors.tx
```

## 链码部分安装和实例化

### 链码安装

这里仅将链码安装在各组织的锚节点中，因此需要安装多次，安装时注意切换到对应节点和msp

``` shell
# 注意环境变量切换(peer0.org1,peer0.org2 安装链码，其他节点不安装)
export CC_SRC_PATH=/opt/gopath/src/github.com/chaincode/
peer chaincode install -n mycc -v 1.0 -l java -p ${CC_SRC_PATH}
```

### 链码实例化

``` shell
peer chaincode instantiate -o orderer.cnabs.com:7050 -C cnabs -n mycc -v 1.0 -l java -c '{"Args":["init"]}' -P "OR ('Org1MSP.peer','Org1MSP.admin','Org1MSP.member')"

# peer chaincode instantiate -o orderer.cnabs.com:7050 -C cnabs -n mycc -v 1.0 -l java -c '{"Args":["init"]}' -P "OR ('Org2MSP.peer',# 'Org2MSP.admin','Org2MSP.member')"
```

统一链码实例化只能执行一次，后面再执行必定报错，本例子使用peer0的证书去实例化了chaincode，因此启动了一个链码容器关联到了 peer0.org1.。。

### 查询

通过 在 2个host上 分别使用 `docker ps` 查看，只有一个链码容器被启动起来了，下面将使用peer0.org2的证书信息，进行一次查询操作，
用以启动peer0.org2节点上的链码容器

``` shell
# 环境变量切换到 peer0.org2
peer chaincode query -C cnabs -n mycc -c '{"Args":["query","user1"]}'
```
命令执行完成后(链码容器启动需要一定时间)，再次使用 `docker ps`查看，确实在另1个host2上启动了 peer0.org2 的链码容器

### 链码调用

需要注意的是，CLI容器中调用链码时，注意msp需在 实例化链码所指定的背书策略范围内，否则调用交易无法验证通过。

其他略，没什么特殊的，注意环境变量切换即可。
