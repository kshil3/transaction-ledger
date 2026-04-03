package com.sk.ledger.repository;

import com.sk.ledger.entity.PurchaseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<PurchaseTransaction, UUID> {
    // Standard CRUD methods like save() and findById() are inherited from JpaRepository
    Optional<PurchaseTransaction> findByIdempotencyKey(UUID idempotencyKey);
}
