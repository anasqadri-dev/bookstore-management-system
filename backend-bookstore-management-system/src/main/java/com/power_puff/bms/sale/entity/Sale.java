package com.power_puff.bms.sale.entity;

import com.power_puff.bms.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Sale date is required")
    @Column(name = "sale_date", nullable = false)
    private Instant saleDate;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.00", message = "Total amount cannot be negative")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Sale user is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SaleItem> items = new ArrayList<>();

    protected Sale() {
    }

    public Sale(Instant saleDate, String customerName, BigDecimal totalAmount, User user) {
        this.saleDate = saleDate;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.user = user;
    }

    @PrePersist
    private void setSaleDateIfMissing() {
        if (saleDate == null) {
            saleDate = Instant.now();
        }
    }

    public void addItem(SaleItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Sale item cannot be null");
        }
        items.add(item);
        item.setSale(this);
    }

    public void removeItem(SaleItem item) {
        if (items.remove(item)) {
            item.setSale(null);
        }
    }

    public Integer getId() { return id; }
    public Instant getSaleDate() { return saleDate; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public User getUser() { return user; }
    public List<SaleItem> getItems() { return List.copyOf(items); }

    public void setId(Integer id) { this.id = id; }
    public void setSaleDate(Instant saleDate) { this.saleDate = saleDate; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setUser(User user) { this.user = user; }
}
