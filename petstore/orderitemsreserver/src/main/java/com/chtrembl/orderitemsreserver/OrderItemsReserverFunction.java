package com.chtrembl.orderitemsreserver;

import com.chtrembl.orderitemsreserver.service.BlobStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;
import java.util.UUID;

public class OrderItemsReserverFunction {

	private static volatile BlobStorageService blobStorageService;
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private BlobStorageService getBlobStorageService(ExecutionContext context) {
		if (blobStorageService == null) {
			synchronized (OrderItemsReserverFunction.class) {
				if (blobStorageService == null) {
					String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
					String containerName = System.getenv("AZURE_STORAGE_CONTAINER_NAME");
					if (containerName == null || containerName.isEmpty()) {
						containerName = "order-items";
					}
					blobStorageService = new BlobStorageService(connectionString, containerName, context.getLogger());
				}
			}
		}
		return blobStorageService;
	}

	/**
	 * HTTP endpoint for order items reservation. POST /api/upload-data
	 */
	@FunctionName("upload-data")
	public HttpResponseMessage run(
			@HttpTrigger(
					name = "req",
					methods = { HttpMethod.POST },
					authLevel = AuthorizationLevel.ANONYMOUS)
			HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {

		String correlationId = resolveCorrelationId(request);
		context.getLogger().info("OrderItemsReserver triggered | correlationId=" + correlationId);

		try {
			Optional<String> body = request.getBody();

			if (!body.isPresent() || body.get().isEmpty()) {
				context.getLogger().warning("Empty request body received | correlationId=" + correlationId);
				return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
						.body(new OrderResponse("Request body is required"))
						.header("Content-Type", "application/json")
						.header("X-Request-ID", correlationId)
						.build();
			}

			OrderRequest orderRequest = objectMapper.readValue(body.get(), OrderRequest.class);

			if (orderRequest.getSessionId() == null || orderRequest.getSessionId().isEmpty()) {
				context.getLogger().warning("Missing sessionId in request | correlationId=" + correlationId);
				return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
						.body(new OrderResponse("sessionId is required"))
						.header("Content-Type", "application/json")
						.header("X-Request-ID", correlationId)
						.build();
			}

			context.getLogger().info("Processing reservation for sessionId=" + orderRequest.getSessionId()
					+ " | correlationId=" + correlationId);

			String blobUrl = getBlobStorageService(context)
					.uploadOrderRequest(orderRequest.getSessionId(), orderRequest, context.getLogger());

			context.getLogger().info("Reservation complete for sessionId=" + orderRequest.getSessionId()
					+ " | blobUrl=" + blobUrl + " | correlationId=" + correlationId);

			return request.createResponseBuilder(HttpStatus.CREATED)
					.body(new OrderResponse("Order items reserved successfully for session: "
							+ orderRequest.getSessionId()))
					.header("Content-Type", "application/json")
					.header("Location", blobUrl)
					.header("X-Request-ID", correlationId)
					.build();

		} catch (Exception e) {
			context.getLogger().severe("Unhandled error reserving order items | correlationId=" + correlationId
					+ " | error=" + e.getMessage());
			return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new OrderResponse("An unexpected error occurred. Please retry or contact support."))
					.header("Content-Type", "application/json")
					.header("X-Request-ID", correlationId)
					.build();
		}
	}

	private String resolveCorrelationId(HttpRequestMessage<?> request) {
		String existing = request.getHeaders().get("x-request-id");
		return (existing != null && !existing.isEmpty()) ? existing : UUID.randomUUID().toString();
	}
}
