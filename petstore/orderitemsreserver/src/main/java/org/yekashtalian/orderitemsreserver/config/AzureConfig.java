package org.yekashtalian.orderitemsreserver.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AzureConfig {

	@Bean
	public BlobContainerClient blobContainerClient(
			@Value("${azure.storage.connection-string}") String connectionString,
			@Value("${azure.storage.container-name:order-items}") String containerName) {

		log.info("Initializing BlobContainerClient for container: {}", containerName);

		BlobServiceClient serviceClient = new BlobServiceClientBuilder()
				.connectionString(connectionString)
				.buildClient();

		BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);

		if (!client.exists()) {
			log.info("Container {} does not exist, creating...", containerName);
			client.create();
		}

		return client;
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}

