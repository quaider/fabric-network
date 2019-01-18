package com.kratos;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.KeySpec;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AppTest {

    @Test
    public void shouldAnswerWithTrue() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        constructChannel();
    }

    private void constructChannel() throws Exception {
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        User admin = new DemoUser("Org1MSP", "Admin@org1.cnabs.com");
        ((DemoUser) admin).setAffiliation("com.cnabs.org1");


        client.setUserContext(admin);

        Orderer orderer = client.newOrderer("orderer.cnabs.com", "grpc://192.168.8.131:7050");
        Peer peer = client.newPeer("peer0.org1.cnabs.com", "grpc://192.168.8.131:7051");
        Channel channel = client.newChannel("cnabs");
        channel.addPeer(peer);
        channel.addOrderer(orderer);

        channel.initialize();
        BlockInfo blockInfo = channel.queryBlockByNumber(1);


        System.out.println(blockInfo.getBlockNumber());
        System.out.println(blockInfo.getEnvelopeCount());
        System.out.println(new String(blockInfo.getPreviousHash()));

        TransactionProposalRequest req = TransactionProposalRequest.newInstance(admin);
        req.setChaincodeLanguage(TransactionRequest.Type.JAVA);
//        req.setChaincodeName("mycc");
        ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName("mycc").build();
        req.setChaincodeID(chaincodeID);
        req.setArgs("invoke", "minus", "user1", "3");
        req.setChaincodeVersion("1.0");
        req.setFcn("invoke");
        req.setUserContext(admin);
        Collection<ProposalResponse> responses = channel.sendTransactionProposal(req);

        boolean ok = false;
        for (ProposalResponse response : responses) {
            ok = response.getStatus() == ChaincodeResponse.Status.SUCCESS;
            if (!ok) throw new RuntimeException("bad Proposal");
        }

        channel.sendTransaction(responses);

        Thread.sleep(2000);
//        Set<String> peers = client.queryChannels(peer);
//        List<Query.ChaincodeInfo> chaincodes = client.queryInstalledChaincodes(peer);
//        for (Query.ChaincodeInfo chaincodeInfo : chaincodes) {
//            System.out.println(chaincodeInfo.getId());
//            System.out.println(chaincodeInfo.getName() + ":" + chaincodeInfo.getPath());
//        }
    }
}

class DemoUser implements User {

    private String mspId;
    private String name;
    private String account;
    private String affiliation;
    private Enrollment enrollment;

    public DemoUser(String mspId, String name) {
        this.mspId = mspId;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<String> getRoles() {
        return null;
    }

    @Override
    public String getAccount() {
        return null;
    }

    @Override
    public String getAffiliation() {
        return null;
    }

    private PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException {
        final Reader pemReader = new StringReader(new String(data));
        final PrivateKeyInfo pemPair;

        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);

        return privateKey;
    }

    static File findFileSk(String directorys) {

        File directory = new File(directorys);

        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];

    }

    @Override
    public Enrollment getEnrollment() {
        String path = "src/test/crypto-config/peerOrganizations/org1.cnabs.com/users/Admin@org1.cnabs.com/msp/keystore";
        String certificateFilePath = "src/test/crypto-config/peerOrganizations/org1.cnabs.com/users/Admin@org1.cnabs.com/msp/signcerts/Admin@org1.cnabs.com-cert.pem";
        File privateKeyFile = findFileSk(path);

        PrivateKey privateKey = null;
        String certificate = null;

        try {
            certificate = new String(IOUtils.toByteArray(new FileInputStream(certificateFilePath)), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privateKeyFile)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SampleStoreEnrollement(privateKey, certificate);
    }

    @Override
    public String getMspId() {
        return this.mspId;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }
}

class SampleStoreEnrollement implements Enrollment, Serializable {

    private static final long serialVersionUID = -2784835212445309006L;
    private final PrivateKey privateKey;
    private final String certificate;

    SampleStoreEnrollement(PrivateKey privateKey, String certificate) {

        this.certificate = certificate;

        this.privateKey = privateKey;
    }

    @Override
    public PrivateKey getKey() {
        return privateKey;
    }

    @Override
    public String getCert() {
        return certificate;
    }
}
