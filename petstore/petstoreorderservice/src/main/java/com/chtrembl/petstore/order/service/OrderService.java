package com.chtrembl.petstore.order.service;

import com.chtrembl.petstore.order.exception.OrderNotFoundException;
import com.chtrembl.petstore.order.model.Order;
import com.chtrembl.petstore.order.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final ConcurrentHashMap<String, Order> orderStore = new ConcurrentHashMap<>();
    private final ProductService productService;

    public Order createOrder(String orderId) {
        log.info("Creating new order with id: {}", orderId);
        Order order = Order.builder()
                .id(orderId)
                .products(new ArrayList<>())
                .status(Order.Status.PLACED)
                .complete(false)
                .build();
        orderStore.put(orderId, order);
        return order;
    }

    /**
     * Retrieves an existing order by ID.
     * Does NOT create a new order if one doesn't exist.
     *
     * @param orderId the order ID to look up
     * @throws OrderNotFoundException if order does not exist
     */
    public Order getOrderById(String orderId) {
        log.info("Retrieving order: {}", orderId);

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        Order order = orderStore.get(orderId);
        if (order != null) {
            log.info("Found existing order: {}", orderId);
            return order;
        }

        log.warn("Order not found: {}", orderId);
        throw new OrderNotFoundException("Order with ID " + orderId + " not found");
    }

    /**
     * Gets an existing order or creates a new one if it doesn't exist.
     * Used for update operations.
     */
    public Order getOrCreateOrder(String orderId) {
        log.info("Getting or creating order: {}", orderId);
        return orderStore.computeIfAbsent(orderId, id -> {
            log.info("Creating new order for update: {}", id);
            return Order.builder()
                    .id(id)
                    .products(new ArrayList<>())
                    .status(Order.Status.PLACED)
                    .complete(false)
                    .build();
        });
    }

    public int getOrderCount() {
        return orderStore.size();
    }

    public Order updateOrder(Order order) {
        log.info("Updating order: {}", order.getId());

        if (order.getProducts() != null && !order.getProducts().isEmpty()) {
            List<Product> availableProducts = productService.getAvailableProducts();
            validateProductsExist(order.getProducts(), availableProducts);
        }

        Order storedOrder = getOrCreateOrder(order.getId());

        storedOrder.setEmail(order.getEmail());

        if (order.getStatus() != null) {
            storedOrder.setStatus(order.getStatus());
        }

        Boolean isComplete = order.getComplete();
        if (isComplete != null && isComplete) {
            log.info("Completing order {} - clearing products", order.getId());
            storedOrder.setProducts(new ArrayList<>());
            storedOrder.setComplete(true);
        } else {
            storedOrder.setComplete(isComplete != null ? isComplete : false);
            updateOrderProducts(storedOrder, order.getProducts());
        }

        orderStore.put(order.getId(), storedOrder);

        return storedOrder;
    }

    public void enrichOrderWithProductDetails(Order order, List<Product> availableProducts) {
        if (order.getProducts() == null || availableProducts == null) {
            log.warn("Cannot enrich order: order.products={}, availableProducts={}",
                    order.getProducts(), availableProducts != null ? availableProducts.size() : "null");
            return;
        }

        log.info("Enriching order {} with {} available products",
                order.getId(), availableProducts.size());

        for (Product orderProduct : order.getProducts()) {
            String originalName = orderProduct.getName();
            String originalURL = orderProduct.getPhotoURL();

            Optional<Product> foundProduct = availableProducts.stream()
                    .filter(p -> p.getId().equals(orderProduct.getId()))
                    .findFirst();

            if (foundProduct.isPresent()) {
                Product availableProduct = foundProduct.get();
                orderProduct.setName(availableProduct.getName());
                orderProduct.setPhotoURL(availableProduct.getPhotoURL());

                log.info("Enriched product {}: '{}' -> '{}', URL: '{}' -> '{}'",
                        orderProduct.getId(), originalName, availableProduct.getName(),
                        originalURL, availableProduct.getPhotoURL());
            } else {
                log.warn("Product with id {} not found in available products during enrichment",
                        orderProduct.getId());
            }
        }
    }

    /**
     * Validates that all products in the order exist in the available products list
     */
    private void validateProductsExist(List<Product> orderProducts, List<Product> availableProducts) {
        if (orderProducts == null || orderProducts.isEmpty()) {
            return;
        }

        if (availableProducts == null || availableProducts.isEmpty()) {
            log.warn("No available products found - cannot validate order products");
            return;
        }

        Set<Long> availableProductIds = availableProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        Set<Long> requestedProductIds = orderProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        Set<Long> missingProductIds = requestedProductIds.stream()
                .filter(id -> !availableProductIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingProductIds.isEmpty()) {
            log.warn("Products not found in catalog: {}", missingProductIds);
        }

        log.debug("Product validation passed for {} products", requestedProductIds.size());
    }

    private void updateOrderProducts(Order cachedOrder, List<Product> incomingProducts) {
        if (incomingProducts == null || incomingProducts.isEmpty()) {
            return;
        }

        if (incomingProducts.size() == 1) {
            handleSingleProductUpdate(cachedOrder, incomingProducts.getFirst());
        } else {
            cachedOrder.setProducts(new ArrayList<>(incomingProducts));
        }
    }

    private void handleSingleProductUpdate(Order cachedOrder, Product incomingProduct) {
        List<Product> existingProducts = cachedOrder.getProducts();
        if (existingProducts == null) {
            existingProducts = new ArrayList<>();
            cachedOrder.setProducts(existingProducts);
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
                        incomingProduct.getId(), cachedOrder.getId(), newQuantity);
            } else if (newQuantity <= 10) {
                existingProduct.setQuantity(newQuantity);
                log.info("Updated product {} quantity to {} in order {}",
                        incomingProduct.getId(), newQuantity, cachedOrder.getId());
            } else {
                existingProduct.setQuantity(10);
                log.warn("Quantity capped at maximum (10) for product {} in order {}",
                        incomingProduct.getId(), cachedOrder.getId());
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
                        incomingProduct.getId(), finalQuantity, cachedOrder.getId());

                if (quantity > 10) {
                    log.warn("Quantity reduced to maximum (10) for new product {} in order {}",
                            incomingProduct.getId(), cachedOrder.getId());
                }
            } else {
                log.info("Ignoring request to add product {} with non-positive quantity {} to order {}",
                        incomingProduct.getId(), quantity, cachedOrder.getId());
            }
        }
    }
}
