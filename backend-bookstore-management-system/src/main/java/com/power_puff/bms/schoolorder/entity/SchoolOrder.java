package com.power_puff.bms.schoolorder.entity;

import com.power_puff.bms.school.entity.School;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school_orders")
public class SchoolOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "School is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @NotNull(message = "Order date is required")
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @NotNull(message = "School order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchoolOrderStatus status = SchoolOrderStatus.PENDING;

    @NotNull(message = "Order creator is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "schoolOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchoolOrderItem> items = new ArrayList<>();

    protected SchoolOrder() {
    }

    public SchoolOrder(School school, User user) {
        this(school, LocalDate.now(), SchoolOrderStatus.PENDING, user);
    }

    public SchoolOrder(School school, LocalDate orderDate,
                       SchoolOrderStatus status, User user) {
        this.school = school;
        this.orderDate = orderDate;
        this.status = status == null ? SchoolOrderStatus.PENDING : status;
        this.user = user;
    }

    @PrePersist
    private void setDefaults() {
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
        if (status == null) {
            status = SchoolOrderStatus.PENDING;
        }
    }

    public void addItem(SchoolOrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("School order item cannot be null");
        }
        items.add(item);
        item.setSchoolOrder(this);
    }

    public void removeItem(SchoolOrderItem item) {
        if (items.remove(item)) {
            item.setSchoolOrder(null);
        }
    }

    public Integer getId() { return id; }
    public School getSchool() { return school; }
    public LocalDate getOrderDate() { return orderDate; }
    public SchoolOrderStatus getStatus() { return status; }
    public User getUser() { return user; }
    public List<SchoolOrderItem> getItems() { return List.copyOf(items); }

    public void setId(Integer id) { this.id = id; }
    public void setSchool(School school) { this.school = school; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public void setStatus(SchoolOrderStatus status) { this.status = status; }
    public void setUser(User user) { this.user = user; }
}
