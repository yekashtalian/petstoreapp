package com.chtrembl.petstore.product.repository;

import com.chtrembl.petstore.product.entity.ProductEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(attributePaths = {"category", "tags"})
    List<ProductEntity> findByStatusIn(List<String> statuses);

    @Override
    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<ProductEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "tags"})
    List<ProductEntity> findAll();
}
