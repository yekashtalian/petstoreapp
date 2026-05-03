package com.chtrembl.orderitemsreserver;

import com.chtrembl.orderitemsreserver.service.BlobStorageService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;


public class OrderItemsReserverFunction {
    
    private static BlobStorageService blobStorageService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private BlobStorageService getBlobStorageService() {
        if (blobStorageService == null) {
            String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
            String containerName = System.getenv("AZURE_STORAGE_CONTAINER_NAME");
            if (containerName == null || containerName.isEmpty()) {
                containerName = "order-items";
            }
            blobStorageService = new BlobStorageService(connectionString, containerName);
        }
        return blobStorageService;
    }
    
    /**
     * HTTP endpoint for order items reservation
     * URL: /api/upload-data
     */
    @FunctionName("upload-data")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("OrderItemsReserver function triggered");
        
        try {
            Optional<String> body = request.getBody();
            
            if (!body.isPresent() || body.get().isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(new OrderResponse("Request body is required"))
                    .header("Content-Type", "application/json")
                    .build();
            }

            OrderRequest orderRequest = objectMapper.readValue(body.get(), OrderRequest.class);

            if (orderRequest.getSessionId() == null || orderRequest.getSessionId().isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(new OrderResponse("Session ID is required"))
                    .header("Content-Type", "application/json")
                    .build();
            }
            
            context.getLogger().info("Processing order for session: " + orderRequest.getSessionId());

            BlobStorageService service = getBlobStorageService();
            service.uploadOrderRequest(orderRequest.getSessionId(), orderRequest);
            
            String message = String.format("Order items reserved successfully for session: %s", 
                orderRequest.getSessionId());
            
            OrderResponse response = new OrderResponse(message);
            
            context.getLogger().info("Success: " + message);
            
            return request.createResponseBuilder(HttpStatus.OK)
                .body(response)
                .header("Content-Type", "application/json")
                .build();
            
        } catch (Exception e) {
            context.getLogger().severe("Error processing order: " + e.getMessage());
            e.printStackTrace();
            
            OrderResponse errorResponse = new OrderResponse("Error reserving order items: " + e.getMessage());
            
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse)
                .header("Content-Type", "application/json")
                .build();
        }
    }
}
