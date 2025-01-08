package io.sealos.enterprise.auth.handler;

import io.javalin.http.Context;
import io.sealos.enterprise.auth.exception.BusinessException;
import io.sealos.enterprise.auth.exception.ErrorCode;
import io.sealos.enterprise.auth.model.dto.UserDTO;
import io.sealos.enterprise.auth.model.request.EnterpriseAuthRequest;
import io.sealos.enterprise.auth.model.response.ApiResponse;
import io.sealos.enterprise.auth.model.response.EnterpriseAuthResponse;
import io.sealos.enterprise.auth.model.response.Unionpay3060ApiEnterpriseAuthResponse;
import io.sealos.enterprise.auth.service.EnterpriseAuthService;

public class EnterpriseAuthHandler {
    private static final EnterpriseAuthService service = new EnterpriseAuthService();

    public static void handleEnterpriseAuth(Context ctx) throws Exception {
        // Parse request body
        EnterpriseAuthRequest request = ctx.bodyAsClass(EnterpriseAuthRequest.class);
        UserDTO userDTO = ctx.attribute("user");
        // Process request
        Unionpay3060ApiEnterpriseAuthResponse response = service.processEnterpriseAuth(request, userDTO);

        if (response == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "3060 api response is null", 500);
        }
        EnterpriseAuthResponse enterpriseAuthResponse = new EnterpriseAuthResponse();

        if ("00000000".equals(response.getRespCode())) {
            enterpriseAuthResponse.setRespCode(response.getRespCode());
            enterpriseAuthResponse.setRespMsg(response.getRespMsg());
            enterpriseAuthResponse.setIsTransactionSuccess(true);

            enterpriseAuthResponse.setKey(response.getKey());
            enterpriseAuthResponse.setAccountBank(response.getAccountBank());
            enterpriseAuthResponse.setAccountProv(response.getAccountProv());
            enterpriseAuthResponse.setAccountCity(response.getAccountCity());
            enterpriseAuthResponse.setSubBank(response.getSubBank());

            enterpriseAuthResponse.setEnterpriseName(response.getSensData().getKeyName());
            enterpriseAuthResponse.setLegalPersonName(response.getSensData().getUsrName());

            enterpriseAuthResponse.setOrderId(response.getOrderId());
            enterpriseAuthResponse.setIsCharged("0000".equals(response.getOrderStatus()));

            enterpriseAuthResponse.setTransAmt(response.getTransAmt());
            ctx.json(ApiResponse.success(enterpriseAuthResponse));
        } else {
            enterpriseAuthResponse.setRespCode(response.getRespCode());
            enterpriseAuthResponse.setRespMsg(response.getRespMsg());
            enterpriseAuthResponse.setIsTransactionSuccess(false);

            enterpriseAuthResponse.setKey(request.getKey());
            enterpriseAuthResponse.setAccountBank(request.getAccountBank());
            enterpriseAuthResponse.setAccountProv(request.getAccountProv());
            enterpriseAuthResponse.setAccountCity(request.getAccountCity());
            enterpriseAuthResponse.setSubBank(request.getSubBank());

            enterpriseAuthResponse.setEnterpriseName(request.getKeyName());
            enterpriseAuthResponse.setLegalPersonName(request.getUsrName());

            enterpriseAuthResponse.setOrderId(response.getOrderId());
            enterpriseAuthResponse.setIsCharged("0000".equals(response.getOrderStatus()));

            ctx.json(ApiResponse.success(enterpriseAuthResponse));
        }

    }
}