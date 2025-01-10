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

import io.javalin.openapi.*;

@OpenApi(path = "/enterprise-auth", methods = {
        HttpMethod.POST }, summary = "Authenticate Enterprise", operationId = "authenticateEnterprise", description = "Authenticates an enterprise using provided credentials", tags = {
                "Enterprise Authentication" }, security = @OpenApiSecurity(name = "Bearer"), requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = EnterpriseAuthRequest.class), required = true, description = "Enterprise authentication credentials"), responses = {
                        @OpenApiResponse(status = "200", description = "Authentication successful", content = @OpenApiContent(from = EnterpriseAuthResponse.class)),
                        @OpenApiResponse(status = "400", description = "Invalid request parameters", content = @OpenApiContent(from = ApiResponse.class)),
                        @OpenApiResponse(status = "401", description = "Unauthorized", content = @OpenApiContent(from = ApiResponse.class)),
                        @OpenApiResponse(status = "500", description = "Internal server error", content = @OpenApiContent(from = ApiResponse.class))
                })
public class EnterpriseAuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnterpriseAuthHandler.class);
    private static final EnterpriseAuthService service = new EnterpriseAuthService();

    // 使用静态初始化块来创建验证器，这样可以更好地处理可能的异常
    private static final Validator validator;

    static {
        try {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
        } catch (Exception e) {
            logger.error("Failed to initialize validator", e);
            throw new RuntimeException("Could not initialize validator", e);
        }
    }

    public static void handleEnterpriseAuth(Context ctx) throws Exception {
        EnterpriseAuthRequest request;
        try {
            request = ctx.bodyAsClass(EnterpriseAuthRequest.class);
            if (request == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request body cannot be null", 400);
            }
        } catch (Exception e) {
            logger.error("Failed to parse request body: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid request body format: " + e.getMessage(),
                    400);
        }

        // 执行验证
        Set<ConstraintViolation<EnterpriseAuthRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            // 收集所有验证错误信息
            String errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));

            logger.warn("Validation failed: {}", errorMessages);
            throw new IllegalArgumentException(errorMessages);
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
