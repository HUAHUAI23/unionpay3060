package io.sealos.enterprise.auth.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class StringUtils {

    /**
     * 将 Map 转换为 URL 参数字符串，并正确处理编码
     */
    public static String mapToUrlParams(Map<String, Object> params) throws UnsupportedEncodingException {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                if (first) {
                    first = false;
                } else {
                    result.append("&");
                }

                // URL 编码 key 和 value
                String key = URLEncoder.encode(entry.getKey(), "UTF-8");
                String value = URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8");

                result.append(key).append("=").append(value);
            }
        }

        return result.toString();
    }

    /**
     * 解析 URL 参数字符串为 Map，并正确处理解码
     */
    public static Map<String, String> parseUrlParamsToMap(String params) {
        Map<String, String> result = new HashMap<>();

        if (params == null || params.trim().isEmpty()) {
            return result;
        }

        String[] pairs = params.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                // 直接使用拆分后的值，不进行 URL 解码
                result.put(keyValue[0], keyValue[1]);
            }
        }

        return result;
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}