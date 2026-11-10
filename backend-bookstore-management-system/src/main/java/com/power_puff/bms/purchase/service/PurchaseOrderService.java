package com.power_puff.bms.purchase.service;

import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.purchase.dto.PurchaseOrderItemRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderResponse;
import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderItem;
import com.power_puff.bms.purchase.mapper.PurchaseOrderMapper;
import com.power_puff.bms.purchase.repository.PurchaseOrderRepository;
import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                SupplierRepository supplierRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public PurchaseOrderResponse create(PurchaseOrderRequest request, String username) {
        if (request == null || request.supplierId() == null || request.supplierId() <= 0) {
            throw new IllegalArgumentException("A valid supplier id is required.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("A purchase order must contain at least one item.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("An authenticated Admin user is required.");
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier with id " + request.supplierId() + " was not found."));
        User user = userRepository.findByUsernameIgnoreCase(username)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The authenticated user account could not be found."));

        Map<Integer, Product> productsById = productRepository
                .findAllById(collectProductIds(request.items()))
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        PurchaseOrder order = new PurchaseOrder(supplier, user);
        for (PurchaseOrderItemRequest item : request.items()) {
            validateItem(item);
            Product product = productsById.get(item.productId());
            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product with id " + item.productId() + " was not found.");
            }

            order.addItem(new PurchaseOrderItem(
                    product,
                    item.quantity(),
                    item.unitCost()));
        }

        PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(order);
        return PurchaseOrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAll() {
        return purchaseOrderRepository.findAllByOrderByOrderDateDesc()
                .stream()
                .map(PurchaseOrderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Integer id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order with id " + id + " was not found."));
        return PurchaseOrderMapper.toResponse(order);
    }

    private List<Integer> collectProductIds(List<PurchaseOrderItemRequest> items) {
        return items.stream()
                .filter(item -> item != null && item.productId() != null && item.productId() > 0)
                .map(PurchaseOrderItemRequest::productId)
                .distinct()
                .toList();
    }

    private void validateItem(PurchaseOrderItemRequest item) {
        if (item == null || item.productId() == null || item.productId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid product id is required for each purchase order item.");
        }
        if (item.quantity() == null || item.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "Purchase order item quantity must be greater than zero.");
        }
        if (item.unitCost() == null || item.unitCost().signum() <= 0
                || item.unitCost().scale() > 2 || item.unitCost().precision() > 12) {
            throw new IllegalArgumentException(
                    "Purchase order unit cost must be positive and have at most two decimal places.");
        }
    }
}
