package com.chaoslab.flashsale.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ticket_orders")
public class TicketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String buyerId;

    @Column(nullable = false)
    private Instant purchasedAt;

    @Column(nullable = false)
    private String status;

    public TicketOrder() {
    }

    public TicketOrder(Long id, String buyerId, Instant purchasedAt, String status) {
        this.id = id;
        this.buyerId = buyerId;
        this.purchasedAt = purchasedAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(Instant purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketOrder that = (TicketOrder) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String buyerId;
        private Instant purchasedAt;
        private String status;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder buyerId(String buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public Builder purchasedAt(Instant purchasedAt) {
            this.purchasedAt = purchasedAt;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public TicketOrder build() {
            return new TicketOrder(id, buyerId, purchasedAt, status);
        }
    }
}
