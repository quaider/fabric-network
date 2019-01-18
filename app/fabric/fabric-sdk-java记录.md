
Framework app扩大异常捕捉范围，修复log4net日志中文乱码  1

1、获取配置

1.1、Config类：处理fabric sdk 配置

SDK配置文件：property: org.hyperledger.fabric.sdk.configuration | config.properties = config.properties

		  path: D:\Dev\open source\java\fabric-sdk-java\config.properties
		  
		  fabric 基础配置，如提案等待时间，msp签名算法等，示例中全注释即采用了默认配置
		  
getConfig()
	-> 缓存中有则直接返回，没有则走下述方法获取
	-> new Config
			- 加载SDK配置文件(上述路径)
			- 设置配置的默认值，包含log4j的日志级别设置(从sdk配置中加载的日志级别)
			
			
1.2、TestConfigHelper 类：对 fabric sdk 配置进行覆盖配置
	
	customizeConfig() 从名为 FABRICSDKOVERRIDES 的环境变量中加载配置项(各配置用,分隔)，然后将各项设置到系统属性中
	其实还是SDK本身的配置
	
1.3、TestConfig 类：处理示例中的组织、节点、msp等配置, 主要为了获得组织和组织中的节点的一些信息

	getIntegrationTestsSampleOrgs() 获取示例的组织

	test配置文件：property: org.hyperledger.fabric.sdktest.configuration | src/test/java/org/hyperledger/fabric/sdk/testutils.properties
	
	示例中是不存在这个配置文件的，所以代码层面做了默认设置
	
	$prefix = org.hyperledger.fabric.sdktest;
	$prefix.InvokeWaitTime
	$prefix.ProposalWaitTime
	
	$prefix = org.hyperledger.fabric.sdktest.integrationTests.org
	$prefix.peerOrg1.mspid = Org1MSP	
	$prefix.peerOrg1.domname = org1.example.com
	$prefix.peerOrg1.ca_location = http://LOCALHOST:7054
	$prefix.peerOrg1.caName = ca0
	$prefix.peerOrg1.peer_locations = peer0.org1.example.com@grpc://LOCALHOST:7051,peer1.org1.example.com@grpc://LOCALHOST:7056
	$prefix.peerOrg1.orderer_locations = orderer.example.com@grpc://LOCALHOST:7050
	$prefix.peerOrg1.eventhub_locations = peer0.org1.example.com@grpc://LOCALHOST:7053,peer1.org1.example.com@grpc://LOCALHOST:7058

	$prefix.peerOrg2.mspid = Org2MSP	
	$prefix.peerOrg2.domname = org2.example.com
	$prefix.peerOrg2.ca_location = http://LOCALHOST:8054
	$prefix.peerOrg2.peer_locations = peer0.org2.example.com@grpc://LOCALHOST:8051,peer1.org2.example.com@grpc://LOCALHOST:8056
	$prefix.peerOrg2.orderer_locations = orderer.example.com@grpc://LOCALHOST:7050
	$prefix.peerOrg2.eventhub_locations = peer0.org2.example.com@grpc://LOCALHOST:8053,peer1.org2.example.com@grpc://LOCALHOST:8058
	
	sampleOrgs 根据以上解析，该类主要职责就是获取下面的结构
	{
		"peerOrg1": {
			"mspid": "Org1MSP",
			"name": "peerOrg1",
			"peerLocations": [
				{ "name": "peer0.org1.example.com", "location": "grpc://LOCALHOST:7051" },
				{ "name": "peer1.org1.example.com", "location": "grpc://LOCALHOST:7056" }
			],
			"domainName": "org1.example.com",
			"ordererLocations": [
				{ "name": "orderer.example.com", "location": "grpc://LOCALHOST:7050" }
			],
			"eventHubLocations": [
				{ "name": "peer0.org1.example.com", "location": "grpc://LOCALHOST:7053" },
				{ "name": "peer1.org1.example.com", "location": "grpc://LOCALHOST:7058" }
			],
			"caLocation": "http://LOCALHOST:7054",
			"caName": "ca0"
		},
		"peerOrg2": {
			"mspid": "Org2MSP",
			"name": "peerOrg2s",
		}
	}
	
2、安装：C:\Users\kzhang\AppData\Local\Temp\HFCSampletest.properties
	建立一个本地的基于文件的KV存储  Samplestore
	针对各个组织去登记用户，如各个组织的admin，并存储在上述的KV存储中
	运行测试(包含创建channel、加入节点、链码操作)

	
	
	
	
	
	
	
	
	
	
	
	
	
	