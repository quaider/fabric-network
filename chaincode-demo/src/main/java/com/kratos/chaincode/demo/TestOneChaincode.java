package com.kratos.chaincode.demo;

import com.google.gson.*;
import io.netty.handler.ssl.OpenSsl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;

import java.util.Iterator;

public class TestOneChaincode extends ChaincodeBase {

    private static Log logger = LogFactory.getLog(TestOneChaincode.class);

    private final String PLAYER_HP = "player_%s_hp";

    @Override
    public Response init(ChaincodeStub stub) {
        logger.info("chaincode 初始化方法执行...");
        return newSuccessResponse("初始化成功...");
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        String fun = stub.getFunction();
        switch (fun) {
            case "query":
                return doQuery(stub);
            case "invoke":
                return doInvoke(stub);
        }

        return newSuccessResponse("invoke没有被处理...");
    }

    private Response doQuery(ChaincodeStub stub) {
        Response response = Guard.checkArguments(stub, 2);
        logger.info("query 参数检查结果：" + response.getStatus().name());
        if (response.getStatus() != Response.Status.SUCCESS)
            return response;

        String user = stub.getParameters().get(0);

        String compositeKey = String.format(PLAYER_HP, user);
        // System.out.println("---compositeKey: " + compositeKey);

        String hpStr = stub.getStringState(compositeKey);
        System.out.println("current hp " + hpStr);

        if (hpStr == null || hpStr.length() <= 0) {
            hpStr = "100";
        }

        String hp = "hp is" + hpStr;
        System.out.println(hp);

        return newSuccessResponse(hp);
    }

    private Response doInvoke(ChaincodeStub stub) {

        Response response = Guard.checkLessArguments(stub, 2);
        if (response.getStatus() != Response.Status.SUCCESS)
            return response;

        String opType = stub.getParameters().get(0);
        switch (opType) {
            case "add":
                return addHp(stub, true);
            case "minus":
                return addHp(stub, false);
            case "history":
                return fetchHistory(stub);
        }

        return newSuccessResponse();
    }

    private Response addHp(ChaincodeStub stub, boolean doAdd) {
        Response response = Guard.checkLessArguments(stub, 4);
        if (response.getStatus() != Response.Status.SUCCESS)
            return response;

        int delta = 0;
        try {
            delta = Integer.parseInt(stub.getParameters().get(2));
            if (delta > 100 || delta < 0) throw new NumberFormatException("hp must between 0 and 100");
        } catch (NumberFormatException e) {
            return newErrorResponse("your hp input is incorrect");
        }

        String user = stub.getParameters().get(1);
        String compositeKey = String.format(PLAYER_HP, user);
        int currentHp = 0;
        try {
            currentHp = Integer.parseInt(stub.getStringState(compositeKey));
        } catch (NumberFormatException ex) {
            currentHp = 100;
        }

        Integer newVal = doAdd ? (currentHp + delta) : (currentHp - delta);
        if (newVal < 0) newVal = 0;
        if (newVal > 100) newVal = 100;

        stub.putStringState(compositeKey, newVal.toString());

        return newSuccessResponse(newVal.toString());
    }

    private Response fetchHistory(ChaincodeStub stub) {
        Response response = Guard.checkLessArguments(stub, 3);
        if (response.getStatus() != Response.Status.SUCCESS)
            return response;

        String user = stub.getParameters().get(1);
        String compositeKey = String.format(PLAYER_HP, user);

        Iterator<KeyModification> modifications = stub.getHistoryForKey(compositeKey).iterator();


        JsonArray jsonArray = new JsonArray();
        while (modifications.hasNext()) {
            KeyModification modification = modifications.next();
            JsonObject o = new JsonObject();
            o.addProperty("txid", modification.getTxId());
            o.addProperty("value", modification.getStringValue());
            o.addProperty("timestamp", modification.getTimestamp().getEpochSecond());
            o.addProperty("deleted", modification.isDeleted());
            jsonArray.add(o);
        }

        com.google.gson.Gson gson = new Gson();
        String histryJson = gson.toJson(jsonArray);

        return newSuccessResponse(histryJson);
    }

    static class Guard {
        static Response checkArguments(ChaincodeStub stub, int n) {
            int size = stub.getArgs().size();
            if (size != n)
                return newErrorResponse(String.format("参数个数不正确，期望是 %d", n));
            return newSuccessResponse();
        }

        static Response checkLessArguments(ChaincodeStub stub, int n) {
            int size = stub.getArgs().size();
            if (size < n)
                return newErrorResponse(String.format("参数个数不正确，期望是至少 %d", n));
            return newSuccessResponse();
        }
    }

    public static void main(String[] args) {
        System.out.println("OpenSSL avaliable: " + OpenSsl.isAvailable());
        new TestOneChaincode().start(args);
    }
}
