## 1、创建docker-compose.yaml 文件

## 2、安装并启动CA
    - 注册 orderer和peer到ca
    - getcacerts
    - 创建configtx.yaml
    - 生成创世区块、通道交易文件、锚节点更新交易文件

## 3、启动orderer
    - enroll 获取 tls证书
    - 复制证书到目标目录
    - enroll 获取 msp证书的
    - 复制msp证书和admin证书到目标目录
    - 启动 orderer

## 4、启动peer
    - enroll 获取 peer的 tls证书
    - 复制证书到目标目录
    - enroll 获取peer 客户端 tls证书和密钥对(暂时不理解)
    - enroll 获取 peer cli的 tls 证书？(暂时不理解)
    - 复制msp和admin证书到目标目录
    - 启动 peer节点，peer node start

## 5、运行测试
    - 常规的 创建通道、加入通道、更新锚节点、查询、调用操作


# 环境搭建步骤

## 启动 ca server
本实例先暂时采用单ca的方式，通过如下方式启动一个 ca server

`fabric-ca-server start -b admin:adminpw`

## 启动 orderer

在启动orderer之前，必须先从ca获取足够的证书，如 tls、cacerts、msp等

### 登记ca引导身份
``` shell
# 假设 FABRIC_CA_CLIENT_HOME 的目录设定如下
export FABRIC_CA_CLIENT_HOME=/etc/hyperledger/fabric-ca-client
```
登记引导身份会在本地的 `$FABRIC_CA_CLIENT_HOME/admin`目录下 生成 如下结构的文件

<pre style="font-size:10px">
├── admin
│   ├── fabric-ca-client-config.yaml
│   └── msp
│       ├── cacerts
│       │   └── localhost-7054.pem
│       ├── keystore
│       │   └── 7006d01e3b70c41b93b09a41dab0cf37911b15270467906cf5757ca693d2bae7_sk
│       └── signcerts
│           └── cert.pem
└── org1
    └── peer0
        ├── fabric-ca-client-config.yaml
        └── msp
            ├── cacerts
            │   └── localhost-7054.pem
            ├── keystore
            │   └── e78fc37161ee917aaff659b8081e77902447e07bb65bba80c5d92b0682feb4f7_sk
            └── signcerts
                └── cert.pem
</pre>

在 setup(fabric-ca-tool)容器中进行

``` shell
export FABRIC_CA_CLIENT_ID_AFFILIATION=com.cnabs
export FABRIC_CA_CLIENT_HOME=/data/client/crypto/admin
# ca-server生成的ca-cert.pem 用于客户端的 FABRIC_CA_CLIENT_TLS_CERTFILES
export FABRIC_CA_CLIENT_TLS_CERTFILES=/data/client/tls/ca-cert.pem
fabric-ca-client enroll -d -u https://admin:adminpw@fabric-ca-server:7054

# 查看联盟信息
fabric-ca-client affiliation list
```

### 注册 orderer 节点身份

``` shell
# 使用引导身份注册orderer节点
# 可以通过环境变量指定联盟信息 或者 --id.affiliation 
export FABRIC_CA_CLIENT_ID_AFFILIATION=com.cnabs
fabric-ca-client register -d --id.name orderer --id.secret passwd --id.type orderer

# 使用引导身份注册orderer组织管理员用户 Admin@orderer.cnabs.com
fabric-ca-client register -d --id.name Admin@orderer.cnabs.com --id.secret passwd --id.attrs "admin=true:ecert"

# 查看注册的身份信息
fabric-ca-client identity list
```

### 注册 org1 的 peer 节点身份
``` shell
export FABRIC_CA_CLIENT_ID_AFFILIATION=com.cnabs.org1
# 使用引导身份注册peer节点
export FABRIC_CA_CLIENT_HOME=/data/client/crypto
export FABRIC_CA_CLIENT_TLS_CERTFILES=/data/client/tls/ca-cert.pem

fabric-ca-client register -d --id.name peer0@org1.cnabs.com --id.secret passwd --id.type peer

# 使用引导身份注册org1组织管理员用户 Admin@org1.cnabs.com
fabric-ca-client register -d --id.name Admin@org1.cnabs.com --id.secret passwd --id.attrs "hf.Registrar.Roles=client,hf.Registrar.Attributes=*,hf.Revoker=true,hf.GenCRL=true,admin=true:ecert,abac.init=true:ecert"

# 使用引导身份注册org1组织普通用户 user@org1.cnabs.com
fabric-ca-client register -d --id.name user@org1.cnabs.com --id.secret passwd
```

### 获取CA证书链

通常，msp目录的cacerts目录必须包含CA颁发的证书链，代表节点的所有可信根证书。fabric-ca-client getcacerts 命令即是用于从其他Fabric CA 服务器实例获取证书链。

**以org1为例**

``` shell
export FABRIC_CA_CLIENT_TLS_CERTFILES=/data/client/tls/ca-cert.pem

fabric-ca-client getcacert -d -u https://fabric-ca-server:7054 -M /data/client/crypto/orgs/org1/msp

# 创建 MSP的 tls目录，如果其不存在的话，tlscacerts就是 cacerts？ 这个不理解
# mkdir -p /data/client/crypto/orgs/org1/msp/tlscacerts
# cp /data/client/crypto/orgs/org1/msp/cacerts/* /data/client/crypto/orgs/org1/msp/tlscacerts

# 将admin用户copy到 admincerts   /data/client/crypto/orgs/org1/admin
mkdir -p /data/client/crypto/orgs/org1/admin
export FABRIC_CA_CLIENT_HOME=/data/client/crypto/orgs/org1/admin
export FABRIC_CA_CLIENT_TLS_CERTFILES=/data/client/tls/ca-cert.pem
fabric-ca-client enroll -d -u https://Admin@org1.cnabs.com:passwd@fabric-ca-server:7054

# ORG_ADMIN_CERT=/data/client/crypto/orgs/org1/msp/admincerts/cert.pem
mkdir -p /data/client/crypto/orgs/org1/msp/admincerts
cp /data/client/crypto/orgs/org1/admin/msp/signcerts/* /data/client/crypto/orgs/org1/msp/admincerts/cert.pem
mkdir /data/client/crypto/orgs/org1/admin/msp/admincerts
cp /data/client/crypto/orgs/org1/admin/msp/signcerts/* /data/client/crypto/orgs/org1/admin/msp/admincerts
```




