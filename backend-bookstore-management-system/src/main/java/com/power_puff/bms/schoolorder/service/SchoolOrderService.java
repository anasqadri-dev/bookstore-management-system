package com.power_puff.bms.schoolorder.service;

import com.power_puff.bms.common.exception.ResourceNotFoundException;
import com.power_puff.bms.product.entity.Product;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.school.entity.School;
import com.power_puff.bms.school.repository.SchoolRepository;
import com.power_puff.bms.schoolorder.dto.SchoolOrderItemRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderRequest;
import com.power_puff.bms.schoolorder.dto.SchoolOrderResponse;
import com.power_puff.bms.schoolorder.entity.SchoolOrder;
import com.power_puff.bms.schoolorder.entity.SchoolOrderItem;
import com.power_puff.bms.schoolorder.entity.SchoolOrderStatus;
import com.power_puff.bms.schoolorder.mapper.SchoolOrderMapper;
import com.power_puff.bms.schoolorder.repository.SchoolOrderRepository;
import com.power_puff.bms.user.entity.User;
import com.power_puff.bms.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SchoolOrderService {

    private final SchoolOrderRepository schoolOrderRepository;
    private final SchoolRepository schoolRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public SchoolOrderService(SchoolOrderRepository schoolOrderRepository,
                              SchoolRepository schoolRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository) {
        this.schoolOrderRepository = schoolOrderRepository;
        this.schoolRepository = schoolRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public SchoolOrderResponse create(SchoolOrderRequest request, String username) {
        if (request == null || request.schoolId() == null || request.schoolId() <= 0) {
            throw new IllegalArgumentException("A valid school id is required.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("A school order must contain at least one item.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("An authenticated Admin user is required.");
        }

        School school = schoolRepository.findById(request.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "School with id " + request.schoolId() + " was not found."));
        User user = userRepository.findByUsernameIgnoreCase(username)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The authenticated user account could not be found."));

        Map<Integer, Integer> requestedQuantities = aggregateQuantities(request.items());
        Map<Integer, Product> productsById = productRepository
                .findAllById(requestedQuantities.keySet())
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        SchoolOrder order = new SchoolOrder(school, user);
        for (Map.Entry<Integer, Integer> entry : requestedQuantities.entrySet()) {
            Product product = productsById.get(entry.getKey());
            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product with id " + entry.getKey() + " was not found.");
            }

            int requestedQuantity = entry.getValue();
            if (requestedQuantity > product.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for " + product.getSku()
                                + ": requested " + requestedQuantity
                                + ", available " + product.getQuantity() + ".");
            }

            BigDecimal unitPrice = product.getPrice();
            order.addItem(new SchoolOrderItem(
                    product, requestedQuantity, unitPrice));
        }

        SchoolOrder saved = schoolOrderRepository.saveAndFlush(order);
        return SchoolOrderMapper.toResponse(saved);
    }

    public SchoolOrderResponse updateStatus(Integer id, SchoolOrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("A school order status is required.");
        }

        SchoolOrder order = schoolOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "School order with id " + id + " was not found."));
        order.setStatus(status);
        return SchoolOrderMapper.toResponse(schoolOrderRepository.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public List<SchoolOrderResponse> getAll() {
        return schoolOrderRepository.findAllByOrderByOrderDateDesc()
                .stream()
                .map(SchoolOrderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolOrderResponse getById(Integer id) {
        SchoolOrder order = schoolOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "School order with id " + id + " was not found."));
        return SchoolOrderMapper.toResponse(order);
    }

    private Map<Integer, Integer> aggregateQuantities(List<SchoolOrderItemRequest> items) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (SchoolOrderItemRequest item : items) {
            if (item == null || item.productId() == null || item.productId() <= 0) {
                throw new IllegalArgumentException(
                        "A valid product id is required for each school order item.");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException(
                        "School order item quantity must be greater than zero.");
            }
            try {
                quantities.merge(item.productId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(
                        "Combined school order quantity is too large.", exception);
            }
        }
        return quantities;
    }
}
