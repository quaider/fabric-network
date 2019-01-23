
# msp 规划

目标：利用 `fabric-ca` 生成的msp与使用 `cryptogen` 工具生成的 msp 在结构上保持一致。

## 组织结构
<pre>
com
  cnabs
    org1
    org2
</pre>
		
节点 分为 1 orderer, 2个组织下各2个peer节点, 节点本身的id及节点下辖的用户id如下

### 节点本身
<pre>
orderer.cnabs.com
peer0.org1.cnabs.com
peer1.org1.cnabs.com
peer0.org2.cnabs.com
peer1.org2.cnabs.com
</pre>

### 组织用户
```
cnabs.com
    Admin@cnabs.com
org1.cnabs.com
    Admin@org1.cnabs.com
    User1@org1.cnabs.com
org2.cnabs.com
    Admin@org2.cnabs.com
    User1@org2.cnabs.com
```
	
### 文件夹结构
<pre>
crypto
├── ordererOrganizations
│   └── cnabs.com
│       ├── ca
│       ├── msp
│       │   ├── admincerts
│       │   ├── cacerts
│       │   └── tlscacerts
│       ├── orderers
│       │   └── orderer.cnabs.com
│       │       ├── msp
│       │       └── tls
│       ├── tlsca
│       └── users
│           └── Admin@cnabs.com
│               ├── msp
│               └── tls
└── peerOrganizations
    ├── org1.cnabs.com
    │   ├── ca
    │   ├── msp
    │   │   ├── admincerts
    │   │   ├── cacerts
    │   │   └── tlscacerts
    │   ├── peers
    │   │   ├── peer0.org1.cnabs.com
    │   │   │   ├── msp
    │   │   │   └── tls
    │   │   └── peer1.org1.cnabs.com
    │   │       ├── msp
    │   │       └── tls
    │   ├── tlsca
    │   └── users
    │       ├── Admin@org1.cnabs.com
    │       │   ├── msp
    │       │   └── tls
    │       └── User1@org1.cnabs.com
    │           ├── msp
    │           └── tls
    └── org2.cnabs.com
</pre>


