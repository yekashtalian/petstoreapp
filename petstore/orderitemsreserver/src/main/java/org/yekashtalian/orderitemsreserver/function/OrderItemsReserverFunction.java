package org.yekashtalian.orderitemsreserver.function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.yekashtalian.orderitemsreserver.model.OrderRequest;
import org.yekashtalian.orderitemsreserver.service.BlobStorageService;

import java.util.Optional;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.INTERNAL_SERVER_ERROR;
import static com.microsoft.azure.functions.HttpStatus.OK;

public class OrderItemsReserverFunction {

    private final BlobStorageService blobStorageService;
    private final ObjectMapper mapper;


    @FunctionName("upload-order-data")
    public HttpResponseMessage run(@HttpTrigger(
            name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS
    ) HttpRequestMessage<Optional<OrderRequest>> request, ExecutionContext context) {

        try {
            var orderRequest = request.getBody()
                    .orElseThrow(() -> new IllegalArgumentException("Order request body is required"));

            var sessionId = request.getHeaders().get("x-session-id");
            if (sessionId == null || sessionId.isBlank()) {
                context.getLogger().warning("Session ID is missing in order request");
                return request.createResponseBuilder(BAD_REQUEST)
                        .body("Session ID is required")
                        .build();
            }

            var orderRequestJson = orderRequestToJson(orderRequest);
            blobStorageService.uploadOrderData(sessionId, orderRequestJson);

            context.getLogger().info("Successfully uploaded order data for session: " + sessionId);

            return request.createResponseBuilder(OK)
                    .body("Order item data has successfully uploaded to blob storage")
                    .build();

        } catch (IllegalArgumentException e) {
            context.getLogger().warning("Invalid request: " + e.getMessage());
            return request.createResponseBuilder(BAD_REQUEST)
                    .body("Invalid request: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Error uploading order data: " + e.getMessage());
            return request.createResponseBuilder(INTERNAL_SERVER_ERROR)
                    .body("Error uploading order data: " + e.getMessage())
                    .build();
        }
    }

    private String orderRequestToJson(OrderRequest orderRequest) {
        try {
            return mapper.writeValueAsString(orderRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order request", e);
        }
    }
}
