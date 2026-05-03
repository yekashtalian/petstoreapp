package com.chtrembl.petstoreapp.client;

import com.chtrembl.petstoreapp.config.FeignConfig;
import com.chtrembl.petstoreapp.model.dto.OrderItemsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
		name = "order-items-reserver",
		url = "${petstore.service.orderitems.url}",
		configuration = FeignConfig.class
)
public interface OrderItemsReserverClient {

	@PostMapping("/api/upload-data")
	String uploadOrderItem(@RequestBody OrderItemsRequest orderItemsRequest);

}