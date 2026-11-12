package com.power_puff.bms.dashboard.service;

import com.power_puff.bms.dashboard.dto.DashboardResponse;
import com.power_puff.bms.product.repository.ProductRepository;
import com.power_puff.bms.report.dto.SalesReportItemResponse;
import com.power_puff.bms.sale.entity.Sale;
import com.power_puff.bms.sale.repository.SaleRepository;
import com.power_puff.bms.supplier.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SupplierRepository supplierRepository;
    private final ZoneId businessTimeZone;

    public DashboardService(ProductRepository productRepository,
                            SaleRepository saleRepository,
                            SupplierRepository supplierRepository,
                            @Value("${bms.business-time-zone:Asia/Karachi}") String businessTimeZone) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.supplierRepository = supplierRepository;
        this.businessTimeZone = ZoneId.of(businessTimeZone);
    }

    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(businessTimeZone);
        var startInclusive = today.atStartOfDay(businessTimeZone).toInstant();
        var endExclusive = today.plusDays(1).atStartOfDay(businessTimeZone).toInstant();

        BigDecimal todaySales = saleRepository
                .sumTotalAmountBetween(startInclusive, endExclusive);
        if (todaySales == null) {
            todaySales = BigDecimal.ZERO;
        }

        List<SalesReportItemResponse> recentSales = saleRepository
                .findTop5ByOrderBySaleDateDesc()
                .stream()
                .map(this::toRecentSale)
                .toList();

        return new DashboardResponse(
                productRepository.count(),
                todaySales,
                productRepository.countLowStockProducts(),
                supplierRepository.count(),
                recentSales);
    }

    private SalesReportItemResponse toRecentSale(Sale sale) {
        return new SalesReportItemResponse(
                sale.getId(),
                sale.getSaleDate(),
                sale.getCustomerName(),
                sale.getUser().getFullName(),
                sale.getTotalAmount());
    }
}
