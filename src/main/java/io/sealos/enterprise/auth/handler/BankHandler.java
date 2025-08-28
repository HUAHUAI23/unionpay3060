package io.sealos.enterprise.auth.handler;

import io.javalin.http.Context;
import io.sealos.enterprise.auth.model.response.ApiResponse;
import io.sealos.enterprise.auth.service.BankService;
import io.javalin.openapi.*;

import java.util.Map;

public class BankHandler {
    private static final BankService bankService = new BankService();

    @OpenApi(path = "/banks", methods = {
            HttpMethod.GET }, summary = "Get bank map", operationId = "getBanks", description = "Returns bank name mapping loaded from configured JSON file", tags = {
                    "Bank" }, security = @OpenApiSecurity(name = "Bearer"), responses = {
                            @OpenApiResponse(status = "200", description = "OK", content = @OpenApiContent(from = Map.class)),
                            @OpenApiResponse(status = "401", description = "Unauthorized", content = @OpenApiContent(from = ApiResponse.class)),
                            @OpenApiResponse(status = "500", description = "Internal server error", content = @OpenApiContent(from = ApiResponse.class))
                    })
    public static void getBanks(Context ctx) {
        Map<String, String> bankMap = bankService.getBankMap();
        ctx.json(ApiResponse.success(bankMap));
    }
}
