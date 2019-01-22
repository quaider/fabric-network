# 利用CA生成MSP启动fabric网络

## 启动CA Server 

预先准备了一个 `fabric-ca-server-config.yaml` 配置文件，该配置文件制定了 affiliation 和 ca 的 host信息，
host名称为 `fabric-ca-server`, affiliation结构为：
<pre>
com
  cnabs
    org1
    org2
</pre>

为了操作简便，先启动一个CA Server容器，在docker容器中执行如下脚本(在 ca start之前)

``` shell
fabric-ca-server init -b admin:adminpw

# 将服务端生成的ca证书复制到客户端tls目录， client/tls/ca-cert.pem，后续用于与CA通讯的tls证书
if [ ! -d /data/client/tls/ ]; then
  mkdir -p /data/client/tls/
fi
cp $FABRIC_CA_SERVER_HOME/ca-cert.pem /data/client/tls/ca-cert.pem
fabric-ca-server start
```

## 登记各组织、组织节点、组织用户到CA

启动 ca server后，便可以将身份信息登记和注册到ca server中，在做登记之前，需要一个管理身份(ca引导用户)来操作，
因此需首先获得 ca的引导身份。

本次操纵 在一个 `setup` 容器中进行(采用 镜像 `hyperledger/fabric-ca-tools`)

本次操作的根路径为 /data/client, 引导用户的身份目录存储为 /data/client/crypto/admin，我们与CA的交互采用TLS，
TLS证书在之前启动CA时已经取得(/data/client/tls/ca-cert.pem)

定义几个全局性的变量：

``` shell
# 客户端操作根目录
CLI_DIR=/data/client
CLI_ROOT_MSP=$CLI_DIR/crypto
# /data/client/crypto/admin
CA_BOOTADMIN_DIR=$CLI_ROOT_MSP/admin
# /data/client/tls/ca-cert.pem
CLI_TLS_CERT=$CLI_DIR/tls/ca-cert.pem
```

### 登记CA引导身份

首先需要设置2个环境变量 `FABRIC_CA_CLIENT_HOME` 和 `FABRIC_CA_CLIENT_TLS_CERTFILES`

``` shell
# /data/client//crypto/admin
export FABRIC_CA_CLIENT_HOME=$CA_BOOTADMIN_DIR
export FABRIC_CA_CLIENT_TLS_CERTFILES=$CLI_TLS_CERT
# 登记引导用户，这将在/data/client/crypto/admin 生成 admin的msp(cacerts、keytore、signcerts)
fabric-ca-client enroll -d -u https://admin:adminpw@fabric-ca-server:7054
```

拿到引导身份后面就可以做后续的操作了。

### 注册orderer节点和管理员

orderer节点属于组织 com.cnabs，因此需要设置它的组织，可以修改 生成的 `fabric-ca-client-config.yaml`或通过环境变量的方式来设置一些属性，这里统一使用 环境变量的方式，方便阅读。

``` shell
export FABRIC_CA_CLIENT_ID_AFFILIATION=com.cnabs
# orderer节点本身
fabric-ca-client register -d --id.name orderer.cnabs.com --id.secret passwd --id.type orderer

# orderer所在组织(com.cnabs)的admin管理员用户
fabric-ca-client register -d --id.name Admin@cnabs.com --id.secret passwd --id.attrs "admin=true:ecert"
```

注册操作不会再本地生成文件，他ca server中存储了注册主体的身份， 可以通过 `fabric-ca-client identity list` 查看

### 注册 org1 节点和管理员

``` shell
# 联盟信息切换到 com.cnabs.org1
export FABRIC_CA_CLIENT_ID_AFFILIATION=com.cnabs.org1
# 注册org1的2个peer节点
fabric-ca-client register -d --id.name peer0.org1.cnabs.com --id.secret passwd --id.type peer
fabric-ca-client register -d --id.name peer1.org1.cnabs.com --id.secret passwd --id.type peer

# 注册 org1 的管理员和User1用户
fabric-ca-client register -d --id.name Admin@org1.cnabs.com --id.secret passwd --id.attrs "hf.Registrar.Roles=client,hf.Registrar.Attributes=*,hf.Revoker=true,hf.GenCRL=true,admin=true:ecert,abac.init=true:ecert"
fabric-ca-client register -d --id.name User1@org1.cnabs.com --id.secret passwd
```

### 注册 org2 节点和管理员

``` shell
# 联盟信息切换到 com.cnabs.org2
export FABRIC_CA_CLIENT_ID_AFFILIATION=com.cnabs.org2
# 注册org2的2个peer节点
fabric-ca-client register -d --id.name peer0.org2.cnabs.com --id.secret passwd --id.type peer
fabric-ca-client register -d --id.name peer1.org2.cnabs.com --id.secret passwd --id.type peer

# 注册 org1 的管理员和User1用户
fabric-ca-client register -d --id.name Admin@org2.cnabs.com --id.secret passwd --id.attrs "hf.Registrar.Roles=client,hf.Registrar.Attributes=*,hf.Revoker=true,hf.GenCRL=true,admin=true:ecert,abac.init=true:ecert"
fabric-ca-client register -d --id.name User1@org2.cnabs.com --id.secret passwd
```

### getCACerts

#### orderer 组织

获取 orderer、org1、org2的ca证书链(cacerts)，获取后将cacerts证书复制到 tlscacerts目录，从推测上来看，cacerts与tlscacerts，cacerts与tls，似乎都是一样的，他们都是从cacerts复制过去的。

下面的 脚本中 `ORG_MSP_DIR` 变量标识 组织的 msp 目录，

``` shell
ORG_MSP_DIR=$CLI_ROOT_MSP/ordererOrganizations/cnabs.com/msp
# getcacert 值拿到了ca证书链即cacerts
fabric-ca-client getcacert -d -u https://fabric-ca-server:7054 -M $ORG_MSP_DIR

# 准备组织的tlscacerts，将上面获取的cacerts复制到tlscerts目录中，用于tls安全传输
mkdir $ORG_MSP_DIR/tlscacerts
cp $ORG_MSP_DIR/cacerts/* $ORG_MSP_DIR/tlscacerts
```

##### 登记 orderer 管理员账户 Admin@cnabs.com

下面的 脚本中 `ORG_ADMIN_HOME` 变量标识 管理员的msp目录，`ORG_ADMIN_CERT` 变量 标识 组织的msp目录内的 admincerts 证书

``` shell
# 即 ORG_MSP_DIR 的上一级目录 + /users/Admin@cnabs.com
ORG_ADMIN_HOME=$(dirname "${ORG_MSP_DIR}")/users/Admin@cnabs.com
# 组织的admincerts ( 管理员用户必须将其admincerts复制到组织的admincerts才能视为有效管理员 )
ORG_ADMIN_CERT=${ORG_MSP_DIR}/admincerts/cert.pem

export FABRIC_CA_CLIENT_HOME=$ORG_ADMIN_HOME
export FABRIC_CA_CLIENT_TLS_CERTFILES=$CLI_TLS_CERT
# 登记cnabs.com组织的管理员，这会在 组织 msp内填充其msp证书，如 cacerts、keystore、signcerts
# 但是没有admincerts、tlscacerts证书信息，因此还需准备这2个目录的文件(采用复制)
fabric-ca-client enroll -d -u https://Admin@cnabs.com:passwd@fabric-ca-server:7054

# 准备组织和管理员的admincerts(来自组织的signcerts)、tlscacerts证书
mkdir -p $(dirname "${ORG_ADMIN_CERT}")   # 即组织内的admincerts目录
# 组织的 admincerts 来自于 管理员的 signcerts
cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_CERT
# 管理员的 admincerts 来自于 管理员的 signcerts
mkdir $ORG_ADMIN_HOME/msp/admincerts
cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_HOME/msp/admincerts
```

#### org1 组织

大体上和orderer所在组织类似，不再赘述。

``` shell
ORG_MSP_DIR=$CLI_ROOT_MSP/peerOrganizations/org1.cnabs.com/msp
fabric-ca-client getcacert -d -u https://fabric-ca-server:7054 -M $ORG_MSP_DIR
mkdir $ORG_MSP_DIR/tlscacerts
cp $ORG_MSP_DIR/cacerts/* $ORG_MSP_DIR/tlscacerts
```

##### 登记 org1 管理员账户 Admin@org1.cnabs.com

``` shell
ORG_ADMIN_HOME=$(dirname "${ORG_MSP_DIR}")/users/Admin@org1.cnabs.com
ORG_ADMIN_CERT=${ORG_MSP_DIR}/admincerts/cert.pem

export FABRIC_CA_CLIENT_HOME=$ORG_ADMIN_HOME
export FABRIC_CA_CLIENT_TLS_CERTFILES=$CLI_TLS_CERT
fabric-ca-client enroll -d -u https://Admin@org1.cnabs.com:passwd@fabric-ca-server:7054

# org1.cnabs.com/admincerts
mkdir -p $(dirname "${ORG_ADMIN_CERT}")
cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_CERT

# org1.cnabs.com/users/Admin@org1.cnabs.com/msp/admincerts
mkdir $ORG_ADMIN_HOME/msp/admincerts
cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_HOME/msp/admincerts

```

#### org2 组织

大体上和orderer所在组织类似，不再赘述。

``` shell
ORG_MSP_DIR=$CLI_ROOT_MSP/peerOrganizations/org2.cnabs.com/msp
fabric-ca-client getcacert -d -u https://fabric-ca-server:7054 -M $ORG_MSP_DIR
mkdir $ORG_MSP_DIR/tlscacerts
cp $ORG_MSP_DIR/cacerts/* $ORG_MSP_DIR/tlscacerts
```

##### 登记 org2 管理员账户 Admin@org2.cnabs.com

``` shell
ORG_ADMIN_HOME=$(dirname "${ORG_MSP_DIR}")/users/Admin@org2.cnabs.com
ORG_ADMIN_CERT=${ORG_MSP_DIR}/admincerts/cert.pem

export FABRIC_CA_CLIENT_HOME=$ORG_ADMIN_HOME
export FABRIC_CA_CLIENT_TLS_CERTFILES=$CLI_TLS_CERT
fabric-ca-client enroll -d -u https://Admin@org2.cnabs.com:passwd@fabric-ca-server:7054

# org1.cnabs.com/admincerts
mkdir -p $(dirname "${ORG_ADMIN_CERT}")
cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_CERT

# org1.cnabs.com/users/Admin@org1.cnabs.com/msp/admincerts
mkdir $ORG_ADMIN_HOME/msp/admincerts
cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_HOME/msp/admincerts

```

### 启动 orderer 节点

#### 登记的orderer节点本身的msp

这里主要准备了 orderer.cnabs.com 节点本身的 msp 和 tls 信息，因为上述步骤中并没有涉及到节点自身的 证书登记过程。
大致msp逻辑都类似 `cacerts -> tlscacerts, signcerts -> admincerts, admincerts(组织) -> admincerts(节点)`

``` shell
ENROLLMENT_URL=https://orderer.cnabs.com:passwd@fabric-ca-server:7054
ORDERER_HOST=orderer.cnabs.com

ORG_MSP_DIR=$CLI_ROOT_MSP/ordererOrganizations/cnabs.com/msp
ORG_ADMIN_CERT=${ORG_MSP_DIR}/admincerts/cert.pem
ORDERER_HOME=$(dirname "${ORG_MSP_DIR}")/orderers/orderer.cnabs.com
# tls 与 msp 目录平级
TLSDIR=$ORDERER_HOME/tls

# Enroll to get orderer's TLS cert(公私钥) (using the "tls" profile) into a temp dir `/tmp/tls`
fabric-ca-client enroll -d --enrollment.profile tls -u $ENROLLMENT_URL -M /tmp/tls --csr.hosts $ORDERER_HOST

# orderer节点 msp目录下的 tls 目录
mkdir -p $TLSDIR
cp /tmp/tls/keystore/* $TLSDIR/server.key
cp /tmp/tls/signcerts/* $TLSDIR/server.crt
rm -rf /tmp/tls

# Enroll again to get the orderer's enrollment certificate (default profile)
fabric-ca-client enroll -d -u $ENROLLMENT_URL -M $ORDERER_HOME/msp

mkdir $ORDERER_HOME/msp/tlscacerts
cp $ORDERER_HOME/msp/cacerts/*  $ORDERER_HOME/msp/tlscacerts

dstDir=$ORDERER_HOME/msp/admincerts
mkdir -p $dstDir
cp $ORG_ADMIN_CERT $dstDir
```

orderer 节点 msp 准备完毕，接下来生成交易文件和启动 orderer 的 docker 容器。

#### 生成 创世区块等交易文件

现在差不多可以尝试启动 orderer 节点了，但是在此之前，还缺少 orderer 节点启动的必需文件、如 创世区块、通道配置交易文件、组织锚节点更新交易文件。

准备 本示例中的 `configtx.yaml` 文件，确保里面的 msp路径和mspid正确即可，然后生成这些缺失的文件。

`configtx.yaml` 文件放置在 /data 目录， 生成的交易文件 放置在 /data/channel-artifacts 目录中。

``` shell
# 指定含有 configtx.yaml 配置文件的目录
export FABRIC_CFG_PATH=/data
ARTIFACTS_DIR=/data/channel-artifacts
# mkdir /data/channel-artifacts

configtxgen -profile TwoOrgsOrdererGenesis -outputBlock $ARTIFACTS_DIR/genesis.block
configtxgen -profile TwoOrgsChannel -outputCreateChannelTx $ARTIFACTS_DIR/channel.tx -channelID cnabs
configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate $ARTIFACTS_DIR/Org1MSPanchors.tx -channelID cnabs -asOrg Org1MSP
configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate $ARTIFACTS_DIR/Org2MSPanchors.tx -channelID cnabs -asOrg Org2MSP

```

#### 启动 orderer

使用示例中的 `docker-orderer.yaml` 文件启动 orderer即可，该文件只是启用了TLS功能的一个最简单的文件

``` shell
docker-compose -f docker-orderer.yaml -up -d
```

------

### 启动peer节点

**一共有4个peer节点，这一步需要操作4次**，限于篇幅，需自行复制本段 4次，然后修改涉及到的peer和org信息即可。

#### 登记的peer节点本身的msp

##### 生成服务端tls证书和私钥

该过程 基本等同于 [登记的orderer节点本身的msp](#登记的orderer节点本身的msp)，不再赘述。

``` shell
ENROLLMENT_URL=https://peer0.org1.cnabs.com:passwd@fabric-ca-server:7054
PEER_HOST=peer0.org1.cnabs.com

ORG_MSP_DIR=$CLI_ROOT_MSP/peerOrganizations/org1.cnabs.com/msp
ORG_ADMIN_CERT=${ORG_MSP_DIR}/admincerts/cert.pem
PEER_HOME=$(dirname "${ORG_MSP_DIR}")/peers/peer0.org1.cnabs.com
TLSDIR=$PEER_HOME/tls

# 生成服务端 tls 证书和私钥
fabric-ca-client enroll -d --enrollment.profile tls -u $ENROLLMENT_URL -M /tmp/tls --csr.hosts $PEER_HOST

# peer节点 msp目录下的 tls 目录
mkdir -p $TLSDIR
cp /tmp/tls/keystore/* $TLSDIR/server.key
cp /tmp/tls/signcerts/* $TLSDIR/server.crt
rm -rf /tmp/tls
```

与orderer节点不同的是，peer节点tls传输是双向的，它既可以是tls客户端(连接到order或其他peer)，也可以是tls服务端(被其他peer连接)。
因此需要生成客户端tls证书，尽管服务端和客户端证书可以是一样的，这里我们生成各自独立的tls证书。

##### 生成客户端证书和私钥

这里为了方便，统一存入 `$TLSDIR/tls` 目录

``` shell
fabric-ca-client enroll -d --enrollment.profile tls -u $ENROLLMENT_URL -M /tmp/tls --csr.hosts $PEER_HOST

cp /tmp/tls/keystore/*  $TLSDIR/peer0-org1-client.key
cp /tmp/tls/signcerts/* $TLSDIR/peer0-org1-client.crt
rm -rf /tmp/tls
```

##### 生成客户端-CLI证书和私钥

这里为了方便，统一存入 `$TLSDIR/tls` 目录

``` shell
fabric-ca-client enroll -d --enrollment.profile tls -u $ENROLLMENT_URL -M /tmp/tls --csr.hosts $PEER_HOST

cp /tmp/tls/keystore/*  $TLSDIR/peer0-org1-cli-client.key
cp /tmp/tls/signcerts/* $TLSDIR/peer0-org1-cli-client.crt
rm -rf /tmp/tls
```

##### 获取peer节点的tlscacerts和admincerts

``` shell
# 获取 peer0.org1.cnabs.com节点的msp信息
fabric-ca-client enroll -d -u $ENROLLMENT_URL -M $PEER_HOME/msp
mkdir $PEER_HOME/msp/tlscacerts
cp $PEER_HOME/msp/cacerts/*  $PEER_HOME/msp/tlscacerts

# 获取节点自身的msp的admincerts，从组织的admincerts复制即可
dstDir=$PEER_HOME/msp/admincerts
mkdir -p $dstDir
cp $ORG_ADMIN_CERT $dstDir
```

#### 启动一个peer

[登记的peer节点本身的msp](#登记的peer节点本身的msp) 根据不同组织不同节点执行4遍之后，所有peer节点的msp和tls材料已就绪，下面开始启动peer节点，可分开启动，也可以一次性全部启动，这里将其写在一个 `docker-peers.yaml` 中，以做简化。

``` shell
docker-compose -f docker-peers.yaml up -d
```

### 运行测试

测试单独启动了一个容器(hyperledger/fabric-ca-tools)

创建通道、加入通道、更新锚节点、部署链码、链码查询、链码调用

新容器，基础变量需重新定义

``` shell
# 客户端操作根目录
CLI_DIR=/data/client
CLI_ROOT_MSP=$CLI_DIR/crypto
CA_BOOTADMIN_DIR=$CLI_ROOT_MSP/admin
CLI_TLS_CERT=$CLI_DIR/tls/ca-cert.pem
```

设置 CORE_PEER_MSPCONFIGPATH

``` shell

ORDERER_HOST=orderer.cnabs.com
# 参数
export ORDERER_PORT_ARGS="-o $ORDERER_HOST:7050 --tls --cafile $CLI_TLS_CERT --clientauth"

# 创建通道
## 切换到org1的管理员
ORG_MSP_DIR=$CLI_ROOT_MSP/peerOrganizations/org1.cnabs.com/msp
ORG_ADMIN_HOME=$(dirname "${ORG_MSP_DIR}")/users/Admin@org1.cnabs.com
PEER_HOME=$(dirname "${ORG_MSP_DIR}")/peers/peer0.org1.cnabs.com
TLSDIR=$PEER_HOME/tls

# 关键环境变量
export CORE_PEER_LOCALMSPID=Org1MSP
export CORE_PEER_ADDRESS=peer0.org1.cnabs.com:7051
export CORE_PEER_MSPCONFIGPATH=$ORG_ADMIN_HOME/msp

# peer tlscerts
CORE_PEER_TLS_CLIENTKEY_FILE=$TLSDIR/peer0-org1-client.key
CORE_PEER_TLS_CLIENTCERT_FILE=$TLSDIR/peer0-org1-client.crt

ORDERER_CONN_ARGS="$ORDERER_PORT_ARGS --keyfile $CORE_PEER_TLS_CLIENTKEY_FILE --certfile $CORE_PEER_TLS_CLIENTCERT_FILE"
CHANNEL_TX_FILE=/data/channel-artifacts/channel.tx

peer channel create --logging-level=DEBUG -c cnabs -f $CHANNEL_TX_FILE $ORDERER_CONN_ARGS

# peer channel update -o orderer.cnabs.com:7050 -c cnabs -f /data/channel-artifacts/Org1MSPanchors.tx
```