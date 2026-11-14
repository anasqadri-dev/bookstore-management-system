package com.power_puff.bms.sale.service;

import com.power_puff.bms.activitylog.service.ActivityLogService;
import com.power_puff.bms.common.exception.DuplicateResourceException;
import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.sale.dto.SaleItemRequest;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.entity.SaleItem;
import com.power_puff.bms.sale.mapper.SaleMapper;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SaleCalculator saleCalculator;
    private final ActivityLogService activityLogService;
    private final EntityManager entityManager;

    public SaleService(SaleRepository saleRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       SaleCalculator saleCalculator,
                       ActivityLogService activityLogService,
                       EntityManager entityManager) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.saleCalculator = saleCalculator;
        this.activityLogService = activityLogService;
        this.entityManager = entityManager;
    }

    public SaleReceiptResponse create(SaleRequest request, String username) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("A sale must contain at least one item.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("An authenticated user is required to create a sale.");
        }

        User user = userRepository.findByUsernameIgnoreCase(username)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The authenticated user account could not be found."));

        Map<Integer, Integer> requestedQuantities = aggregateQuantities(request.items());
        Map<Integer, Product> productsById = productRepository
                .findAllById(requestedQuantities.keySet())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));

        List<PreparedLine> preparedLines = new ArrayList<>();
        List<BigDecimal> subtotals = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : requestedQuantities.entrySet()) {
            Product product = productsById.get(entry.getKey());
            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product with id " + entry.getKey() + " was not found.");
            }

            int requestedQuantity = entry.getValue();
            if (product.getQuantity() < requestedQuantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + product.getSku()
                                + ": requested " + requestedQuantity
                                + ", available " + product.getQuantity() + ".");
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = saleCalculator.calculateSubtotal(
                    unitPrice, requestedQuantity);
            preparedLines.add(new PreparedLine(
                    product, requestedQuantity, unitPrice, subtotal));
            subtotals.add(subtotal);
        }

        BigDecimal totalAmount = saleCalculator.calculateTotal(subtotals);
        Sale sale = new Sale(
                Instant.now(),
                normalizeCustomerName(request.customerName()),
                totalAmount,
                user);

        // The SQL predicate is the final authority; the earlier Java check is
        // only an early user-friendly validation using the values just read.
        // Every decrement is in the same transaction as the saved sale and log.
        for (PreparedLine line : preparedLines) {
            int updated;
            try {
                updated = productRepository.decrementStockIfAvailable(
                        line.product().getId(), line.quantity());
            } catch (DataAccessException exception) {
                throw new DuplicateResourceException(
                        "Inventory is being updated by another operation. Please retry checkout.");
            }

            if (updated != 1) {
                entityManager.refresh(line.product());
                throw new DuplicateResourceException(
                        "Stock for " + line.product().getSku()
                                + " changed during checkout. Available quantity is "
                                + line.product().getQuantity() + "; please review the cart.");
            }

            // Bulk JPQL updates bypass the persistence context. Refresh before
            // mapping the receipt or flushing other managed entities.
            entityManager.refresh(line.product());
            sale.addItem(new SaleItem(
                    line.product(),
                    line.quantity(),
                    line.unitPrice(),
                    line.subtotal()));
        }

        Sale savedSale = saleRepository.saveAndFlush(sale);
        activityLogService.record(
                user,
                "SALE",
                "Created sale id=" + savedSale.getId()
                        + ", total=" + savedSale.getTotalAmount());

        return SaleMapper.toReceipt(savedSale);
    }

    @Transactional(readOnly = true)
    public List<SaleReceiptResponse> getSalesHistory() {
        return saleRepository.findAllByOrderBySaleDateDesc().stream()
                .map(SaleMapper::toReceipt)
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleReceiptResponse getReceipt(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sale with id " + saleId + " was not found."));
        return SaleMapper.toReceipt(sale);
    }

    private Map<Integer, Integer> aggregateQuantities(List<SaleItemRequest> items) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (SaleItemRequest item : items) {
            if (item == null || item.productId() == null || item.productId() <= 0) {
                throw new IllegalArgumentException("A valid product id is required for each sale item.");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Sale item quantity must be greater than zero.");
            }
            try {
                quantities.merge(item.productId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Combined sale quantity is too large.", exception);
            }
        }
        return quantities;
    }

    private String normalizeCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return null;
        }
        return customerName.trim();
    }

    private record PreparedLine(
            Product product,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }
}
