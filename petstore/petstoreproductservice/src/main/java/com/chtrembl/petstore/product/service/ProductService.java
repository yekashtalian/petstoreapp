package com.chtrembl.petstore.product.service;

import com.chtrembl.petstore.product.entity.ProductEntity;
import com.chtrembl.petstore.product.model.Category;
import com.chtrembl.petstore.product.model.Product;
import com.chtrembl.petstore.product.model.Tag;
import com.chtrembl.petstore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findProductsByStatus(List<String> status) {
        log.info("Finding products with status: {}", status);
        return productRepository.findByStatusIn(status).stream()
                .map(this::mapToDto)
                .toList();
    }

    public Optional<Product> findProductById(Long productId) {
        log.info("Finding product with id: {}", productId);
        return productRepository.findById(productId).map(this::mapToDto);
    }

    public List<Product> getAllProducts() {
        log.info("Getting all products");
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    public int getProductCount() {
        return (int) productRepository.count();
    }

    private Product mapToDto(ProductEntity entity) {
        List<Tag> tags = entity.getTags().stream()
                .map(t -> Tag.builder().id(t.getId()).name(t.getName()).build())
                .toList();

        Category category = Category.builder()
                .id(entity.getCategory().getId())
                .name(entity.getCategory().getName())
                .build();

        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(category)
                .photoURL(entity.getPhotoURL())
                .status(Product.Status.fromValue(entity.getStatus()))
                .tags(tags)
                .build();
    }
}
