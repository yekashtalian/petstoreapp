package com.chtrembl.orderitemsreserver.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.chtrembl.orderitemsreserver.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;


public class BlobStorageService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String containerName;
    private BlobContainerClient containerClient;

    public BlobStorageService(String connectionString, String containerName, Logger logger) {
        this.containerName = containerName != null ? containerName : "order-items";
        init(connectionString, logger);
    }

    private void init(String connectionString, Logger logger) {
        logger.info("Initializing BlobStorageService with container: " + this.containerName);

        if (connectionString != null && !connectionString.isEmpty()) {
            try {
                BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();

                containerClient = blobServiceClient.getBlobContainerClient(this.containerName);

                if (!containerClient.exists()) {
                    containerClient.create();
                    logger.info("Created blob container: " + this.containerName);
                } else {
                    logger.info("Using existing blob container: " + this.containerName);
                }
            } catch (Exception e) {
                logger.severe("Failed to initialize blob storage client for container '"
                        + this.containerName + "': " + e.getMessage());
                containerClient = null;
            }
        } else {
            logger.warning("AZURE_STORAGE_CONNECTION_STRING is not configured — blob storage will be unavailable.");
            containerClient = null;
        }
    }

    public String uploadOrderRequest(String sessionId, OrderRequest orderRequest, Logger logger) throws Exception {
        if (containerClient == null) {
            throw new IllegalStateException(
                    "Blob storage client is not initialized. Verify AZURE_STORAGE_CONNECTION_STRING is set.");
        }

        String blobName = String.format("order-%s.json", sessionId);
        String jsonContent = objectMapper.writeValueAsString(orderRequest);
        byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        BlobClient blobClient = containerClient.getBlobClient(blobName);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
            blobClient.upload(inputStream, jsonBytes.length, true);
            logger.info("Uploaded order blob '" + blobName + "' for session: " + sessionId);
        }

        return blobClient.getBlobUrl();
    }
}
