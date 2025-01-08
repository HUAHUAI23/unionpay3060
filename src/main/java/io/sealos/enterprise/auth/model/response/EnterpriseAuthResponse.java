package io.sealos.enterprise.auth.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnterpriseAuthResponse {

    private String respCode; // 3060 响应码
    private String respMsg; // 3060 响应信息
    private Boolean isTransactionSuccess; // 3060 是否交易成功

    private String key; // 统一信用代码
    private String accountBank; // 企业账户开户行
    private String accountProv; // 企业开户行所在省
    private String accountCity; // 企业开户行所在地区
    private String subBank; // 电子联行号

    private String enterpriseName; // 企业名称
    private String legalPersonName;// 法人姓名

    private String orderId; // 订单号
    private Boolean isCharged; // 是否收费

    private String transAmt; // 交易金额

}