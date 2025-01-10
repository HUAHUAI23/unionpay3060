package io.sealos.enterprise.auth.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.http.Context;
import io.sealos.enterprise.auth.exception.BusinessException;
import io.sealos.enterprise.auth.exception.ErrorCode;
import io.sealos.enterprise.auth.model.dto.UserDTO;
import io.sealos.enterprise.auth.model.request.EnterpriseAuthRequest;
import io.sealos.enterprise.auth.model.response.ApiResponse;
import io.sealos.enterprise.auth.model.response.EnterpriseAuthResponse;
import io.sealos.enterprise.auth.model.response.Unionpay3060ApiEnterpriseAuthResponse;
import io.sealos.enterprise.auth.service.EnterpriseAuthService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;

public class EnterpriseAuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnterpriseAuthHandler.class);
    private static final EnterpriseAuthService service = new EnterpriseAuthService();
    // Add static validator
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    public static void handleEnterpriseAuth(Context ctx) throws Exception {
        // Parse request body
        EnterpriseAuthRequest request = ctx.bodyAsClass(EnterpriseAuthRequest.class);

        // Use static validator
        Set<ConstraintViolation<EnterpriseAuthRequest>> violations = VALIDATOR.validate(request);

        if (!violations.isEmpty()) {
            // 将验证错误信息合并成一个字符串，保持与现有错误处理一致
            String errorMessage = violations.stream()
                    .map(violation -> {
                        String path = violation.getPropertyPath().toString();
                        String message = violation.getMessage();
                        return path + ": " + message;
                    })
                    .collect(Collectors.joining("; "));

            // 使用现有的 BusinessException 格式
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    errorMessage, // 格式化的错误信息
                    400 // HTTP 状态码
            );
        }

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

            logger.info("User: {}, RegionUid: {}, orderId: {}\nAuth Success, respMsg: {}",
                    ((UserDTO) ctx.attribute("user")).getUserId(),
                    ((UserDTO) ctx.attribute("user")).getRegionUid(),
                    response.getOrderId(),
                    response.getRespMsg());

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

            logger.info("User: {}, RegionUid: {}, orderId: {}\nAuth Failed, respMsg: {}",
                    ((UserDTO) ctx.attribute("user")).getUserId(),
                    ((UserDTO) ctx.attribute("user")).getRegionUid(),
                    response.getOrderId(),
                    response.getRespMsg());

            ctx.json(ApiResponse.success(enterpriseAuthResponse));
        }
    }
}
