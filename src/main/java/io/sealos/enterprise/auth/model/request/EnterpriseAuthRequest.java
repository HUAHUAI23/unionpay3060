package io.sealos.enterprise.auth.model.request;

import lombok.Data;

@Data
public class EnterpriseAuthRequest {
    private String key; // 统一信用代码
    private String accountBank;
    private String accountProv;
    private String accountCity;
    private String subBank;

    private String keyName;
    private String usrName;
}