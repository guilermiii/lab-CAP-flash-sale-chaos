package com.chaoslab.flashsale.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Objects;

@Entity
@Table(name = "ticket_inventory")
public class TicketInventory {

    @Id
    private Long id;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Version
    private Long version;

    public TicketInventory() {
    }

    public TicketInventory(Long id, int totalQuantity, int availableQuantity, Long version) {
        this.id = id;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.version = version;
    }

    public boolean hasAvailableStock() {
        return availableQuantity > 0;
    }

    public void decrementStock() {
        if (!hasAvailableStock()) {
            throw new IllegalStateException("Cannot decrement stock when available quantity is 0");
        }
        this.availableQuantity--;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketInventory that = (TicketInventory) o;
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
        private int totalQuantity;
        private int availableQuantity;
        private Long version;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder totalQuantity(int totalQuantity) {
            this.totalQuantity = totalQuantity;
            return this;
        }

        public Builder availableQuantity(int availableQuantity) {
            this.availableQuantity = availableQuantity;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public TicketInventory build() {
            return new TicketInventory(id, totalQuantity, availableQuantity, version);
        }
    }
}
