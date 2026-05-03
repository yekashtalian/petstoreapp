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
    
    private static final Logger logger = Logger.getLogger(BlobStorageService.class.getName());
    
    private final String connectionString;
    private final String containerName;
    private BlobContainerClient containerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    

    public BlobStorageService(String connectionString, String containerName) {
        this.connectionString = connectionString;
        this.containerName = containerName != null ? containerName : "order-items";
        init();
    }
    
    private void init() {
        
        logger.info("Initializing BlobStorageService with container: " + containerName);

        if (connectionString != null && !connectionString.isEmpty()) {
            try {
                logger.info("Creating BlobServiceClient...");
                BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();
                
                logger.info("Getting container client for: " + containerName);
                containerClient = blobServiceClient.getBlobContainerClient(containerName);

                if (!containerClient.exists()) {
                    containerClient.create();
                    logger.info("Created blob container: " + containerName);
                } else {
                    logger.info("Using existing blob container: " + containerName);
                }
            } catch (Exception e) {
                logger.severe("Failed to initialize blob storage client: " + e.getMessage());
                e.printStackTrace();
                containerClient = null;
            }
        } else {
            logger.warning("Azure Storage connection string not configured. Blob storage operations will fail.");
            containerClient = null;
        }
    }

    public String uploadOrderRequest(String sessionId, OrderRequest orderRequest) throws Exception {
        if (containerClient == null) {
            throw new IllegalStateException("Blob storage not initialized. Check connection string configuration.");
        }

        String blobName = String.format("order-%s.json", sessionId);

        String jsonContent = objectMapper.writeValueAsString(orderRequest);
        byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        BlobClient blobClient = containerClient.getBlobClient(blobName);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
            blobClient.upload(inputStream, jsonBytes.length, true);
            logger.info("Successfully uploaded order for session " + sessionId + " to blob: " + blobName);
        }

        return blobClient.getBlobUrl();
    }
}

