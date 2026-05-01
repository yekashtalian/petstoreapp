package org.yekashtalian.orderitemsreserver.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlobStorageService {

	private final BlobContainerClient blobContainerClient;

	public void uploadOrderData(String sessionId, String jsonContent) {
		String blobName = sessionId + ".json";
		try {
			log.info("Uploading order data to blob: {}", blobName);
			blobContainerClient.getBlobClient(blobName)
					.upload(BinaryData.fromString(jsonContent), true);
			log.info("Successfully uploaded order data to blob: {}", blobName);
		} catch (Exception e) {
			log.error("Failed to upload order data to blob {}: {}", blobName, e.getMessage(), e);
			throw new RuntimeException("Failed to upload order data to Blob Storage", e);
		}
	}
}
