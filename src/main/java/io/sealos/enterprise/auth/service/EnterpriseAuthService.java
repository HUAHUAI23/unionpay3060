package io.sealos.enterprise.auth.service;

import com.chinapay.secss.SecssUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.sealos.enterprise.auth.config.EnvConfig;
import io.sealos.enterprise.auth.model.dto.UserDTO;
import io.sealos.enterprise.auth.model.request.EnterpriseAuthRequest;
import io.sealos.enterprise.auth.model.response.SensitiveData;
import io.sealos.enterprise.auth.model.response.Unionpay3060ApiEnterpriseAuthResponse;
import io.sealos.enterprise.auth.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EnterpriseAuthService {
    private static final Logger logger = LoggerFactory.getLogger(EnterpriseAuthService.class);
    private static final String Unionpay3060Api = EnvConfig.getUnionpay3060Api();
    private static final String MERCHANT_NO = EnvConfig.getMerchantNo();
    private static final String BUSI_TYPE = "3060";
    private static final String KEY_TYPE = "1"; // 1: 统一信用代码

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EnterpriseAuthService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClient.newHttpClient();
    }

    public Unionpay3060ApiEnterpriseAuthResponse processEnterpriseAuth(EnterpriseAuthRequest request, UserDTO userDTO)
            throws Exception {
        // init unionpay secssUtil
        SecssUtil secssUtil = new SecssUtil();

        if (EnvConfig.getConfigPath() == null || EnvConfig.getConfigPath().isEmpty()) {
            throw new RuntimeException("secss.configPath is not set");
        }

        boolean initResult = secssUtil.init(EnvConfig.getConfigPath());
        if (!initResult) {
            logger.error("SecssUtil initialization failed");
            throw new RuntimeException("SecssUtil initialization failed");
        }

        // Create request data
        Map<String, String> requestData = createRequestData(request, userDTO);

        // Process sensitive data
        String encryptedSensData = encryptSensitiveData(request, secssUtil);
        requestData.put("sensData", encryptedSensData);

        // Prepare final request
        Map<String, Object> finalRequest = prepareFinalRequest(requestData, secssUtil);

        logger.info("User: {},RegionUid: {},orderId: {}", userDTO.getUserId(),
                userDTO.getRegionUid(),
                requestData.get("orderId"));

        // Send request and get response
        String responseBody = sendRequest(finalRequest);

        // Process response
        return processResponse(responseBody, secssUtil);
    }

    private Map<String, String> createRequestData(EnterpriseAuthRequest request, UserDTO userDTO) {
        Map<String, String> data = new HashMap<>();
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

        // 使用纳秒级时间戳作为订单号的一部分
        String timestamp = String.format("%014d", System.nanoTime() % 100000000000000L);
        // 可以加入随机数进一步降低冲突概率
        String random = String.format("%04d", (int) (Math.random() * 10000));
        String orderId = currentDate + timestamp + random;

        data.put("merNo", MERCHANT_NO);
        data.put("busiType", BUSI_TYPE);
        data.put("keyType", KEY_TYPE);
        data.put("orderDate", currentDate);
        data.put("orderId", orderId);

        // Add request data
        data.put("key", request.getKey());
        data.put("accountBank", request.getAccountBank());
        // data.put("accountProv", request.getAccountProv());
        // data.put("accountCity", request.getAccountCity());
        // data.put("subBank", request.getSubBank());

        return data;
    }

    private String encryptSensitiveData(EnterpriseAuthRequest request, SecssUtil secssUtil)
            throws Exception {
        Map<String, String> sensData = new HashMap<>();
        sensData.put("accountNo", request.getAccountNo());
        sensData.put("keyName", request.getKeyName());
        sensData.put("usrName", request.getUsrName());

        String sensDataJsonString = objectMapper.writeValueAsString(sensData);
        secssUtil.encryptData(sensDataJsonString);

        if (!"00".equals(secssUtil.getErrCode())) {
            throw new RuntimeException("Encryption failed: " + secssUtil.getErrMsg());
        }

        return secssUtil.getEncValue();
    }

    private Map<String, Object> prepareFinalRequest(Map<String, String> requestData, SecssUtil secssUtil)
            throws Exception {
        String reqDataJsonString = objectMapper.writeValueAsString(requestData);
        String reqDataBase64 = Base64.getEncoder().encodeToString(reqDataJsonString.getBytes(StandardCharsets.UTF_8));

        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] hash = digest.digest(reqDataBase64.getBytes(StandardCharsets.UTF_8));
        String reqDataHash = StringUtils.bytesToHex(hash);

        Map<String, Object> signMap = new HashMap<>();
        signMap.put("reqData", reqDataHash);
        secssUtil.sign(signMap);

        Map<String, Object> finalRequest = new HashMap<>();
        finalRequest.put("reqData", reqDataBase64);
        finalRequest.put("merNo", MERCHANT_NO);
        finalRequest.put("signature", secssUtil.getSign());

        return finalRequest;
    }

    private String sendRequest(Map<String, Object> finalRequest) throws Exception {
        String requestParams = StringUtils.mapToUrlParams(finalRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Unionpay3060Api))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .header("Accept-Charset", "UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestParams, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP request failed with status code: " + response.statusCode());
        }

        return response.body();
    }

    private Unionpay3060ApiEnterpriseAuthResponse processResponse(String responseBody, SecssUtil secssUtil)
            throws Exception {
        Map<String, String> resultMap = StringUtils.parseUrlParamsToMap(responseBody);
        // Verify signature
        verifySignature(resultMap, secssUtil);

        String respDataStr = resultMap.get("respData");
        String respDataDecoded = new String(Base64.getDecoder().decode(respDataStr), StandardCharsets.UTF_8);
        Map<String, Object> jsonRes = objectMapper.readValue(respDataDecoded,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

        // 创建响应对象并手动赋值
        Unionpay3060ApiEnterpriseAuthResponse response = new Unionpay3060ApiEnterpriseAuthResponse();
        response.setAccountCity((String) jsonRes.get("accountCity"));
        response.setSubBank((String) jsonRes.get("subBank"));
        response.setOrderId((String) jsonRes.get("orderId"));
        response.setAccountProv((String) jsonRes.get("accountProv"));
        response.setOrderStatus((String) jsonRes.get("orderStatus"));
        response.setRandomNum((String) jsonRes.get("randomNum"));
        response.setAccountBank((String) jsonRes.get("accountBank"));
        response.setMerNo((String) jsonRes.get("merNo"));
        response.setTransAmt((String) jsonRes.get("transAmt"));
        response.setRespMsg((String) jsonRes.get("respMsg"));
        response.setBusiType((String) jsonRes.get("busiType"));
        response.setKeyType((String) jsonRes.get("keyType"));
        response.setOrderDate((String) jsonRes.get("orderDate"));
        response.setKey((String) jsonRes.get("key"));
        response.setRespCode((String) jsonRes.get("respCode"));

        // 处理敏感数据
        String sensDataStr = (String) jsonRes.get("sensData");
        if (sensDataStr != null) {
            secssUtil.decryptData(sensDataStr);
            if ("00".equals(secssUtil.getErrCode())) {
                String decryptedSensData = secssUtil.getDecValue();
                SensitiveData sensitiveData = objectMapper.readValue(decryptedSensData, SensitiveData.class);
                response.setSensData(sensitiveData);
            } else {
                logger.error("Failed to decrypt sensitive data: {}", secssUtil.getErrMsg());
                throw new RuntimeException("Failed to decrypt sensitive data: " + secssUtil.getErrMsg());
            }
        }

        return response;
    }

    private void verifySignature(Map<String, String> resultMap, SecssUtil secssUtil) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        String respDataStr = resultMap.get("respData");
        byte[] hash = digest.digest(respDataStr.getBytes(StandardCharsets.UTF_8));
        String respDataHash = StringUtils.bytesToHex(hash);

        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put("respData", respDataHash);
        verifyMap.put("signature", resultMap.get("signature"));
        secssUtil.verify(verifyMap);

        if (!"00".equals(secssUtil.getErrCode())) {
            throw new RuntimeException("Signature verification failed");
        }
    }
}