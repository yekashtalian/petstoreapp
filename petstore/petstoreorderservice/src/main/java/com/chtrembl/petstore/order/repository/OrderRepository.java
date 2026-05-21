package com.chtrembl.petstore.order.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.chtrembl.petstore.order.entity.OrderDocument;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends CosmosRepository<OrderDocument, String> {
}
