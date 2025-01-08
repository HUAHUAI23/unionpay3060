package io.sealos.enterprise.auth.model.response;

import lombok.Data;

@Data
public class SensitiveData {
    private String accountNo;
    private String keyName;
    private String usrName;
}
