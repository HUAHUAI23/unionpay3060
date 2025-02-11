package io.sealos.enterprise.auth.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnterpriseAuthRequest {
    @NotBlank(message = "统一信用代码不能为空")
    @Size(min = 5, max = 20, message = "统一信用代码长度必须在5-20位之间")
    private String key;

    private String accountBank;

    private String accountProv;

    private String accountCity;

    @Size(min = 12, max = 12, message = "电子联行号必须是12位")
    private String subBank;

    @NotBlank(message = "企业名称不能为空")
    private String keyName;

    @NotBlank(message = "法人姓名不能为空")
    private String usrName;

    @NotBlank(message = "银行账号不能为空")
    @Size(min = 1, max = 32, message = "银行账号长度必须在1-32位之间")
    private String accountNo;
}