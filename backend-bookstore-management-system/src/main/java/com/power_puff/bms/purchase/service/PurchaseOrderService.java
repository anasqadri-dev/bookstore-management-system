package com.power_puff.bms.purchase.service;

import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.purchase.dto.PurchaseOrderItemRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderRequest;
import com.power_puff.bms.purchase.dto.PurchaseOrderResponse;
import com.power_puff.bms.purchase.entity.PurchaseOrder;
import com.power_puff.bms.purchase.entity.PurchaseOrderItem;
import com.power_puff.bms.purchase.entity.PurchaseOrderStatus;
import com.power_puff.bms.purchase.mapper.PurchaseOrderMapper;
import com.power_puff.bms.purchase.repository.PurchaseOrderRepository;
import com.power_puff.bms.supplier.entity.Supplier;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    public PurchaseOrderResponse receiveGoods(Integer purchaseOrderId) {
        PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order with id " + purchaseOrderId + " was not found."));

        if (order.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new DuplicateResourceException(
                    "Goods have already been received for purchase order " + purchaseOrderId + ".");
        }

        Map<Integer, Product> productsById = new LinkedHashMap<>();
        Map<Integer, Integer> receivedQuantities = new LinkedHashMap<>();

        for (PurchaseOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            productsById.put(product.getId(), product);
            try {
                receivedQuantities.merge(
                        product.getId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Combined received quantity exceeds the supported stock range.", exception);
            }
        }

        Map<Integer, Integer> updatedStock = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : receivedQuantities.entrySet()) {
            Product product = productsById.get(entry.getKey());
            try {
                updatedStock.put(
                        entry.getKey(),
                        Math.addExact(product.getQuantity(), entry.getValue()));
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Receiving this purchase order would exceed the supported stock range for "
                                + product.getSku() + ".", exception);
            }
        }

        LocalDate receivedDate = LocalDate.now();
        int claimed = purchaseOrderRepository.markReceivedIfOrdered(
                purchaseOrderId,
                PurchaseOrderStatus.ORDERED,
                PurchaseOrderStatus.RECEIVED,
                receivedDate);
        if (claimed != 1) {
            throw new DuplicateResourceException(
                    "Goods have already been received for purchase order " + purchaseOrderId + ".");
        }

        updatedStock.forEach((productId, quantity) ->
                productsById.get(productId).setQuantity(quantity));
        productRepository.saveAllAndFlush(productsById.values());

        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setReceivedDate(receivedDate);
        return PurchaseOrderMapper.toResponse(order);
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
