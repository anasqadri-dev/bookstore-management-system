package com.power_puff.bms.purchase.entity;

import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Supplier is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @NotNull(message = "Order date is required")
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @NotNull(message = "Purchase order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.ORDERED;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @NotNull(message = "Order creator is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PurchaseOrderItem> items = new ArrayList<>();

    protected PurchaseOrder() {
    }

    public PurchaseOrder(Supplier supplier, User user) {
        this(supplier, LocalDate.now(), PurchaseOrderStatus.ORDERED, null, user);
    }

    public PurchaseOrder(Supplier supplier, LocalDate orderDate,
                         PurchaseOrderStatus status, LocalDate receivedDate, User user) {
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.status = status == null ? PurchaseOrderStatus.ORDERED : status;
        this.receivedDate = receivedDate;
        this.user = user;
    }

    @PrePersist
    private void setDefaults() {
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
        if (status == null) {
            status = PurchaseOrderStatus.ORDERED;
        }
    }

    public void addItem(PurchaseOrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Purchase order item cannot be null");
        }
        items.add(item);
        item.setPurchaseOrder(this);
    }

    public void removeItem(PurchaseOrderItem item) {
        if (items.remove(item)) {
            item.setPurchaseOrder(null);
        }
    }

    public Integer getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public LocalDate getOrderDate() { return orderDate; }
    public PurchaseOrderStatus getStatus() { return status; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public User getUser() { return user; }
    public List<PurchaseOrderItem> getItems() { return List.copyOf(items); }

    public void setId(Integer id) { this.id = id; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public void setUser(User user) { this.user = user; }
}
