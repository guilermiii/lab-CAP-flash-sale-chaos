package com.chaoslab.flashsale.repository;

import com.chaoslab.flashsale.domain.TicketInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketInventoryRepository extends JpaRepository<TicketInventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM TicketInventory i WHERE i.id = :id")
    Optional<TicketInventory> findWithLockById(@Param("id") Long id);
}
