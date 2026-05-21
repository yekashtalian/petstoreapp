package com.chtrembl.petstore.pet.repository;

import com.chtrembl.petstore.pet.entity.PetEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<PetEntity, Long> {

    @EntityGraph(attributePaths = {"category", "tags"})
    List<PetEntity> findByStatusIn(List<String> statuses);

    @Override
    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<PetEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "tags"})
    List<PetEntity> findAll();
}
