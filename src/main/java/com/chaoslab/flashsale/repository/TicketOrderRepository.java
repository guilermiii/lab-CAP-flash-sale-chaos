package com.chaoslab.flashsale.repository;

import com.chaoslab.flashsale.domain.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {
}
