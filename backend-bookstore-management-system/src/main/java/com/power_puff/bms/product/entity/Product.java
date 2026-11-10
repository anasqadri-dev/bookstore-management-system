package com.power_puff.bms.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @NotBlank(message = "Product title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String title;

    @Size(max = 255, message = "Author cannot exceed 255 characters")
    @Column(length = 255)
    private String author;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Minimum stock is required")
    @Min(value = 0, message = "Minimum stock cannot be negative")
    @Column(name = "min_stock", nullable = false)
    private Integer minStock;

    protected Product() {
    }

    public Product(String sku, String title, String author, BigDecimal price,
                   Integer quantity, Integer minStock) {
        this.sku = sku;
        this.title = title;
        this.author = author;
        this.price = price;
        this.quantity = quantity;
        this.minStock = minStock;
        normalizeSku();
    }

    @PrePersist
    @PreUpdate
    private void normalizeSku() {
        if (sku != null) {
            sku = sku.trim().toUpperCase(Locale.ROOT);
        }
    }

    public boolean isLowStock() {
        return quantity != null && minStock != null && quantity <= minStock;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public BigDecimal getPrice() { return price; }
    public Integer getQuantity() { return quantity; }
    public Integer getMinStock() { return minStock; }

    public void setId(Long id) { this.id = id; }
    public void setSku(String sku) { this.sku = sku; normalizeSku(); }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }
}
