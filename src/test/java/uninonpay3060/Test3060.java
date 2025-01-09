package uninonpay3060;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.chinapay.secss.SecssUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import io.sealos.enterprise.auth.config.EnvConfig;
import io.sealos.enterprise.auth.utils.StringUtils;

public class Test3060 {
    private static final Logger logger = LoggerFactory.getLogger(Test3060.class);
    private static final String TEST_URL = "https://vas-test.chinapay.com/VASAP/vasap/business.htm";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final SecssUtil secssUtil;

    static {
        secssUtil = new SecssUtil();
        boolean initResult = secssUtil.init(EnvConfig.getConfigPath());
        if (!initResult) {
            logger.error("SecssUtil initialization failed");
            throw new RuntimeException("SecssUtil initialization failed");
        }
    }

    public static void main(String[] args) {

        logger.info("Starting UnionPay 3060 Service...");

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
        }).start(2342);

        // 测试接口
        app.get("/test3060", ctx -> {
            try {
                // 测试数据
                Map<String, String> testData = new HashMap<>();
                testData.put("merNo", EnvConfig.getMerchantNo()); // Read from properties
                testData.put("busiType", "3060"); // 业务类型

                // 使用当前时间生成 orderDate 和 orderId
                String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
                String currentTime = new SimpleDateFormat("HHmmss").format(new Date());

                testData.put("orderDate", currentDate); // 订单日期
                testData.put("orderId", currentDate + currentTime); // 订单号

                testData.put("keyType", "1");
                testData.put("key", "91310000734572833M"); // 统一信用代码

                testData.put("accountBank", "中国工商银行"); // 开户行
                testData.put("accountProv", "北京"); // 开户行省份
                testData.put("accountCity", "北京"); // 开户行城市
                testData.put("subBank", "测试支行|123456789123"); // 开户行支行

                testData.put("accountNo", "6221501111111113900");
                testData.put("keyName", "银联商务股份有限公司");
                testData.put("usrName", "田林");

                // 敏感信息加密
                Map<String, String> sensData = new HashMap<>();
                sensData.put("accountNo", testData.get("accountNo"));
                sensData.put("keyName", testData.get("keyName"));
                sensData.put("usrName", testData.get("usrName"));

                String sensDataJson = new ObjectMapper().writeValueAsString(sensData);
                secssUtil.encryptData(sensDataJson);

                if (!"00".equals(secssUtil.getErrCode())) {
                    ctx.result("Encryption failed: " + secssUtil.getErrMsg());
                    return;
                }

                logger.info("加密后的敏感数据: {}", secssUtil.getEncValue());

                // 组装完整请求数据
                Map<String, String> reqData = new HashMap<>(testData);
                // 移除敏感信息
                reqData.remove("accountNo");
                reqData.remove("keyName");
                reqData.remove("usrName");
                // 添加加密后的敏感数据
                reqData.put("sensData", secssUtil.getEncValue());

                logger.info("组装完整请求数据reqData: {}", reqData);

                // Base64编码
                String reqDataBase64Str = Base64.getEncoder().encodeToString(
                        new ObjectMapper().writeValueAsString(reqData).getBytes(StandardCharsets.UTF_8));

                logger.info("Base64编码后的请求数据 reqDataBase64Str: {}", reqDataBase64Str);

                // SHA-512摘要
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                byte[] hash = digest.digest(reqDataBase64Str.getBytes(StandardCharsets.UTF_8));

                String reqDataBase64StrHash = StringUtils.bytesToHex(hash);

                logger.info("请求数据base64编码后sha512摘要 reqDataBase64StrHash: {}", reqDataBase64StrHash);

                // 签名
                Map<String, Object> signMap = new HashMap<>();
                signMap.put("reqData", reqDataBase64StrHash);
                secssUtil.sign(signMap);

                // 最终请求数据
                Map<String, Object> finalRequestData = new HashMap<>();
                finalRequestData.put("reqData", reqDataBase64Str);
                finalRequestData.put("merNo", testData.get("merNo"));
                finalRequestData.put("signature", secssUtil.getSign());

                logger.info("最后发送的请求数据 finalRequestData: {}", finalRequestData);

                // 构建请求参数
                String requestParams = StringUtils.mapToUrlParams(finalRequestData);
                logger.info("请求URL: {}", TEST_URL);
                logger.info("请求参数 requestParams: {}", requestParams);

                // 发送请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(TEST_URL))
                        .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                        .header("Accept-Charset", "UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(requestParams, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                logger.info("返回状态码: {}", response.statusCode());
                logger.info("返回结果: {}", response.body());

                // 解析响应
                Map<String, String> resultMap = StringUtils.parseUrlParamsToMap(response.body());

                logger.info("返回结果解析成 map,resultmap: {}", resultMap);

                logger.info("返回结果中的 respData: {}", resultMap.get("respData"));
                // Base64解码响应数据
                String respDataStrBase64Decode = new String(
                        Base64.getDecoder().decode(resultMap.get("respData")),
                        StandardCharsets.UTF_8);
                logger.info("返回结果中的 respData base64 解密后的数据: {}", respDataStrBase64Decode);

                // 验签
                Map<String, String> verifyMap = new HashMap<>();
                String respDataStr = resultMap.get("respData");
                digest.update(respDataStr.getBytes(StandardCharsets.UTF_8));
                hash = digest.digest();
                String respDataStrHash = StringUtils.bytesToHex(hash);

                logger.info("验签的signature: {}", resultMap.get("signature"));

                logger.info("验签的 respData: {}", respDataStr);
                logger.info("验签的 respData hash: {}", respDataStrHash);

                verifyMap.put("respData", respDataStrHash);
                verifyMap.put("signature", resultMap.get("signature"));
                secssUtil.verify(verifyMap);

                logger.info("验签结果: {}", secssUtil.getErrCode());

                if ("00".equals(secssUtil.getErrCode())) {
                    logger.info("恭喜，验签通过了。");
                } else {
                    logger.error("很遗憾，验签失败了。");
                }

                // 解析响应JSON
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> jsonRes = mapper.readValue(respDataStrBase64Decode,
                        mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

                // 处理响应数据
                Map<String, Object> respmsgMap = new HashMap<>();

                for (Map.Entry<String, Object> entry : jsonRes.entrySet()) {
                    String key = entry.getKey();
                    String value = String.valueOf(entry.getValue());

                    if ("sensData".equals(key)) {
                        secssUtil.decryptData(value);
                        if (!"00".equals(secssUtil.getErrCode())) {
                            logger.error("敏感信息解密失败: {}", secssUtil.getErrMsg());
                            continue;
                        }
                        // Parse the decrypted sensData JSON string into a Map
                        Map<String, Object> sensDataMap = mapper.readValue(secssUtil.getDecValue(),
                                mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

                        respmsgMap.put("sensData", sensDataMap);
                    } else {
                        respmsgMap.put(key, value);
                    }
                }

                ctx.json(respmsgMap);

            } catch (Exception e) {
                logger.error("Test failed", e);
                ctx.result("Test failed: " + e.getMessage());
            }
        });

        logger.info("Server started on port 7070");
    }

}