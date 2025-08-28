package io.sealos.enterprise.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sealos.enterprise.auth.exception.BusinessException;
import io.sealos.enterprise.auth.exception.ErrorCode;
import io.sealos.enterprise.auth.config.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BankService {
    private static final Logger logger = LoggerFactory.getLogger(BankService.class);
    private static final Path BANK_JSON_PATH = Paths.get(EnvConfig.getBankJsonPath());

    private final ObjectMapper objectMapper;

    // 简单本地缓存与热更新（按文件最后修改时间）
    private volatile Map<String, String> cachedBankMap = new ConcurrentHashMap<>();
    private volatile FileTime lastModified;

    public BankService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Map<String, String> getBankMap() {
        try {
            if (!Files.exists(BANK_JSON_PATH)) {
                logger.error("Bank json file not found: {}", BANK_JSON_PATH);
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "bank.json 未找到", 500);
            }

            FileTime fileTime = Files.getLastModifiedTime(BANK_JSON_PATH);
            if (cachedBankMap.isEmpty() || lastModified == null || fileTime.compareTo(lastModified) > 0) {
                synchronized (this) {
                    // 双重检查，避免并发重复加载
                    if (cachedBankMap.isEmpty() || lastModified == null || fileTime.compareTo(lastModified) > 0) {
                        byte[] jsonBytes = Files.readAllBytes(BANK_JSON_PATH);
                        Map<String, String> map = objectMapper.readValue(jsonBytes, new TypeReference<Map<String, String>>() {});
                        cachedBankMap = new ConcurrentHashMap<>(map);
                        lastModified = fileTime;
                        logger.info("Loaded bank map, entries: {} (from {})", cachedBankMap.size(), BANK_JSON_PATH);
                    }
                }
            }

            return cachedBankMap;
        } catch (IOException e) {
            logger.error("Failed to read bank.json: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "读取 bank.json 失败: " + e.getMessage(), 500);
        } catch (Exception e) {
            logger.error("Unexpected error reading bank.json: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "处理 bank.json 异常: " + e.getMessage(), 500);
        }
    }
}


