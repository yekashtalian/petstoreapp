package com.chtrembl.petstore.order.service;

import com.chtrembl.petstore.order.entity.OrderDocument;
import com.chtrembl.petstore.order.entity.OrderProductItem;
import com.chtrembl.petstore.order.exception.OrderNotFoundException;
import com.chtrembl.petstore.order.model.Order;
import com.chtrembl.petstore.order.model.Product;
import com.chtrembl.petstore.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public Order createOrder(String orderId) {
        log.info("Creating new order with id: {}", orderId);
        OrderDocument doc = OrderDocument.builder()
                .id(orderId)
                .products(new ArrayList<>())
                .status(Order.Status.PLACED.toString())
                .complete(false)
                .build();
        orderRepository.save(doc);
        return mapToDto(doc);
    }

    /**
     * Retrieves an existing order by ID. Does NOT create a new order if not found.
     *
     * @param orderId the order ID to retrieve
     * @return the existing order
     * @throws OrderNotFoundException if order does not exist
     */
    public Order getOrderById(String orderId) {
        log.info("Retrieving order from repository: {}", orderId);

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        return orderRepository.findById(orderId)
                .map(this::mapToDto)
                .orElseThrow(() -> {
                    log.warn("Order not found: {}", orderId);
                    return new OrderNotFoundException("Order with ID " + orderId + " not found");
                });
    }

    /**
     * Gets an existing order or creates a new one if it doesn't exist.
     * Used internally for order updates.
     */
    public Order getOrCreateOrder(String orderId) {
        log.info("Getting or creating order: {}", orderId);
        return orderRepository.findById(orderId)
                .map(doc -> {
                    log.info("Found existing order for update: {}", orderId);
                    return mapToDto(doc);
                })
                .orElseGet(() -> {
                    log.info("Creating new order for update: {}", orderId);
                    return createOrder(orderId);
                });
    }

    public Order updateOrder(Order order) {
        log.info("Updating order: {}", order.getId());

        if (order.getProducts() != null && !order.getProducts().isEmpty()) {
            List<Product> availableProducts = productService.getAvailableProducts();
            validateProductsExist(order.getProducts(), availableProducts);
        }

        Order currentOrder = getOrCreateOrder(order.getId());

        currentOrder.setEmail(order.getEmail());

        if (order.getStatus() != null) {
            currentOrder.setStatus(order.getStatus());
        }

        Boolean isComplete = order.getComplete();
        if (isComplete != null && isComplete) {
            log.info("Completing order {} - clearing products", order.getId());
            currentOrder.setProducts(new ArrayList<>());
            currentOrder.setComplete(true);
        } else {
            currentOrder.setComplete(isComplete != null ? isComplete : false);
            updateOrderProducts(currentOrder, order.getProducts());
        }

        orderRepository.save(mapToDocument(currentOrder));

        return currentOrder;
    }

    public void enrichOrderWithProductDetails(Order order, List<Product> availableProducts) {
        if (order.getProducts() == null || order.getProducts().isEmpty()
                || availableProducts == null || availableProducts.isEmpty()) {
            return;
        }

        order.getProducts().forEach(orderProduct -> availableProducts.stream()
                .filter(p -> p.getId().equals(orderProduct.getId()))
                .findFirst()
                .ifPresent(matched -> {
                    orderProduct.setName(matched.getName());
                    orderProduct.setPhotoURL(matched.getPhotoURL());
                }));
    }

    Order mapToDto(OrderDocument doc) {
        List<Product> products = doc.getProducts() == null ? new ArrayList<>()
                : doc.getProducts().stream()
                        .map(this::mapItemToProduct)
                        .collect(Collectors.toList());

        return Order.builder()
                .id(doc.getId())
                .email(doc.getEmail())
                .products(products)
                .status(Order.Status.fromValue(doc.getStatus()))
                .complete(doc.getComplete() != null ? doc.getComplete() : false)
                .build();
    }

    OrderDocument mapToDocument(Order order) {
        List<OrderProductItem> items = order.getProducts() == null ? new ArrayList<>()
                : order.getProducts().stream()
                        .map(this::mapProductToItem)
                        .collect(Collectors.toList());

        return OrderDocument.builder()
                .id(order.getId())
                .email(order.getEmail())
                .products(items)
                .status(order.getStatus() != null ? order.getStatus().toString() : null)
                .complete(order.getComplete())
                .build();
    }

    OrderProductItem mapProductToItem(Product p) {
        return OrderProductItem.builder()
                .id(p.getId())
                .quantity(p.getQuantity() != null ? p.getQuantity() : 0)
                .name(p.getName())
                .photoURL(p.getPhotoURL())
                .build();
    }

    Product mapItemToProduct(OrderProductItem item) {
        return Product.builder()
                .id(item.getId())
                .quantity(item.getQuantity() != null ? item.getQuantity() : 0)
                .name(item.getName())
                .photoURL(item.getPhotoURL())
                .build();
    }

    private void validateProductsExist(List<Product> orderProducts, List<Product> availableProducts) {
        if (orderProducts == null || orderProducts.isEmpty()) {
            return;
        }

        List<Long> requestedProductIds = orderProducts.stream()
                .map(Product::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        List<Long> availableProductIds = availableProducts.stream()
                .map(Product::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        List<Long> missingProductIds = requestedProductIds.stream()
                .filter(id -> !availableProductIds.contains(id))
                .collect(Collectors.toList());

        if (!missingProductIds.isEmpty()) {
            String errorMessage = String.format("Products with IDs %s are not available or do not exist",
                    missingProductIds);
            log.warn("Product validation failed for order: {}", errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        log.debug("Product validation passed for {} products", requestedProductIds.size());
    }

    private void updateOrderProducts(Order currentOrder, List<Product> incomingProducts) {
        if (incomingProducts == null || incomingProducts.isEmpty()) {
            return;
        }

        if (incomingProducts.size() == 1) {
            handleSingleProductUpdate(currentOrder, incomingProducts.getFirst());
        } else {
            currentOrder.setProducts(new ArrayList<>(incomingProducts));
        }
    }

    private void handleSingleProductUpdate(Order currentOrder, Product incomingProduct) {
        List<Product> existingProducts = currentOrder.getProducts();
        if (existingProducts == null) {
            existingProducts = new ArrayList<>();
            currentOrder.setProducts(existingProducts);
        }

        Integer quantity = incomingProduct.getQuantity();

        Optional<Product> existingProductOpt = existingProducts.stream()
                .filter(p -> p.getId().equals(incomingProduct.getId()))
                .findFirst();

        if (existingProductOpt.isPresent()) {
            Product existingProduct = existingProductOpt.get();
            int currentQuantity = existingProduct.getQuantity();
            int newQuantity = currentQuantity + quantity;

            log.info("Updating product {} quantity: {} + {} = {}",
                    incomingProduct.getId(), currentQuantity, quantity, newQuantity);

            if (newQuantity <= 0) {
                existingProducts.removeIf(p -> p.getId().equals(incomingProduct.getId()));
                log.info("Removed product {} from order {} (quantity became {})",
                        incomingProduct.getId(), currentOrder.getId(), newQuantity);
            } else if (newQuantity <= 10) {
                existingProduct.setQuantity(newQuantity);
                log.info("Updated product {} quantity to {} in order {}",
                        incomingProduct.getId(), newQuantity, currentOrder.getId());
            } else {
                existingProduct.setQuantity(10);
                log.warn("Quantity capped at maximum (10) for product {} in order {}",
                        incomingProduct.getId(), currentOrder.getId());
            }
        } else {
            if (quantity > 0) {
                int finalQuantity = Math.min(quantity, 10);
                existingProducts.add(Product.builder()
                        .id(incomingProduct.getId())
                        .quantity(finalQuantity)
                        .name(incomingProduct.getName())
                        .photoURL(incomingProduct.getPhotoURL())
                        .build());

                log.info("Added new product {} with quantity {} to order {}",
                        incomingProduct.getId(), finalQuantity, currentOrder.getId());

                if (quantity > 10) {
                    log.warn("Quantity reduced to maximum (10) for new product {} in order {}",
                            incomingProduct.getId(), currentOrder.getId());
                }
            } else {
                log.info("Ignoring request to add product {} with non-positive quantity {} to order {}",
                        incomingProduct.getId(), quantity, currentOrder.getId());
            }
        }
    }
}
