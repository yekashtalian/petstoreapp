package com.chtrembl.petstoreapp.client;

import com.chtrembl.petstoreapp.config.FeignConfig;
import com.chtrembl.petstoreapp.model.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
		name = "order-items-function",
		url = "${petstore.service.orderitems.url}",
		configuration = FeignConfig.class
)
public interface OrderItemsReserverClient {

	@PostMapping("/api/upload-order-data")
	String uploadOrderItem(@RequestBody Order order, @RequestHeader("X-Session-ID") String sessionId);

}