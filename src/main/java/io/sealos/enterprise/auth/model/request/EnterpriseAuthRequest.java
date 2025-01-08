package io.sealos.enterprise.auth.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnterpriseAuthRequest {
    private String key; // 统一信用代码
    private String accountBank; // 开户行
    private String accountProv; // 开户行省份
    private String accountCity; // 开户行城市
    private String subBank; // 电子联行号

    private String keyName; // 企业名称
    private String usrName; // 法人姓名
    private String accountNo; // 银行账号
}