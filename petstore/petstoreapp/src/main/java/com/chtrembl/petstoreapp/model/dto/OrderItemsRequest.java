package com.chtrembl.petstoreapp.model.dto;

import com.chtrembl.petstoreapp.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for OrderItemsReserver Azure Function request
 * Maps petstoreapp Order model to the Azure Function's expected format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemsRequest {
  private String sessionId;
  private String orderId;
  private String email;
  private List<Product> products;
  private String status;
  private boolean complete;
}
