package com.chtrembl.petstore.order.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Container(containerName = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDocument {

    @Id
    @PartitionKey
    private String id;

    private String email;

    @Builder.Default
    private List<OrderProductItem> products = new ArrayList<>();

    private String status;

    @Builder.Default
    private Boolean complete = false;
}
