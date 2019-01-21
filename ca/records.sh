export FABRIC_CA_CLIENT_HOME=/data/client/crypto/admin
export FABRIC_CA_CLIENT_TLS_CERTFILES=/data/client/tls/ca-cert.pem
fabric-ca-client enroll -d -u https://admin:adminpw@fabric-ca-server:7054


# order
export FABRIC_CA_CLIENT_ID_1AFFILIATION=com.cnabs
fabric-ca-client register -d --id.name orderer.cnabs.com --id.secret passwd --id.type orderer
# order admin
fabric-ca-client register -d --id.name Admin@cnabs.com --id.secret passwd --id.attrs "admin=true:ecert"


# org1
export FABRIC_CA_CLIENT_ID_AFFILIATION=com.cnabs.org1
fabric-ca-client register -d --id.name peer0.org1.cnabs.com --id.secret passwd --id.type peer
# org1 admin & user1
fabric-ca-client register -d --id.name Admin@org1.cnabs.com --id.secret passwd --id.attrs "hf.Registrar.Roles=client,hf.Registrar.Attributes=*,hf.Revoker=true,hf.GenCRL=true,admin=true:ecert,abac.init=true:ecert"
fabric-ca-client register -d --id.name User1@org1.cnabs.com --id.secret passwd


# org2 略


# getCACerts(orderer、org1、org2)
	## order
	ORG_MSP_DIR=/data/client/crypto/ordererOrganizations/cnabs.com/msp
	fabric-ca-client getcacert -d -u https://fabric-ca-server:7054 -M $ORG_MSP_DIR
	mkdir $ORG_MSP_DIR/tlscacerts
	cp $ORG_MSP_DIR/cacerts/* $ORG_MSP_DIR/tlscacerts
	
		### order admin
		ORG_ADMIN_HOME=/data/client/crypto/ordererOrganizations/cnabs.com/users/Admin@cnabs.com
		ORG_ADMIN_CERT=${ORG_MSP_DIR}/admincerts/cert.pem
		export FABRIC_CA_CLIENT_HOME=$ORG_ADMIN_HOME
		export FABRIC_CA_CLIENT_TLS_CERTFILES=/data/client/tls/ca-cert.pem
		fabric-ca-client enroll -d -u https://Admin@cnabs.com:passwd@fabric-ca-server:7054
		
		mkdir -p $(dirname "${ORG_ADMIN_CERT}")
		cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_CERT
		mkdir $ORG_ADMIN_HOME/msp/admincerts
		cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_HOME/msp/admincerts
	
	
	## org1
	ORG_MSP_DIR=/data/client/crypto/peerOrganizations/org1.cnabs.com/msp
	fabric-ca-client getcacert -d -u https://fabric-ca-server:7054 -M $ORG_MSP_DIR
	mkdir $ORG_MSP_DIR/tlscacerts
	cp $ORG_MSP_DIR/cacerts/* $ORG_MSP_DIR/tlscacerts
	
		### org1 admin
		ORG_ADMIN_HOME=/data/client/crypto/peerOrganizations/org1.cnabs.com/users/Admin@org1.cnabs.com
		ORG_ADMIN_CERT=${ORG_MSP_DIR}/admincerts/cert.pem
		export FABRIC_CA_CLIENT_HOME=$ORG_ADMIN_HOME
		export FABRIC_CA_CLIENT_TLS_CERTFILES=/data/client/tls/ca-cert.pem
		fabric-ca-client enroll -d -u https://Admin@org1.cnabs.com:passwd@fabric-ca-server:7054
		
		mkdir -p $(dirname "${ORG_ADMIN_CERT}")
		cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_CERT
		mkdir $ORG_ADMIN_HOME/msp/admincerts
		cp $ORG_ADMIN_HOME/msp/signcerts/* $ORG_ADMIN_HOME/msp/admincerts

# enroll部分

