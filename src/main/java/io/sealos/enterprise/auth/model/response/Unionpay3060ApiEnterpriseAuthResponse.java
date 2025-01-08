package io.sealos.enterprise.auth.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Unionpay3060ApiEnterpriseAuthResponse {
    private String accountCity;
    private String subBank;
    private String orderId;
    private SensitiveData sensData;
    private String accountProv;
    private String orderStatus;
    private String randomNum;
    private String accountBank;
    private String merNo;
    private String transAmt;
    private String respMsg;
    private String busiType;
    private String keyType;
    private String orderDate;
    private String key;
    private String respCode;
}
