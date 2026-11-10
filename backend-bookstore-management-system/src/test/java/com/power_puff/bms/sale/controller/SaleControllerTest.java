package com.power_puff.bms.sale.controller;

import com.power_puff.bms.common.exception.GlobalExceptionHandler;
import com.power_puff.bms.sale.dto.SaleItemReceiptResponse;
import com.power_puff.bms.sale.dto.SaleReceiptResponse;
import com.power_puff.bms.sale.dto.SaleRequest;
import com.power_puff.bms.sale.service.SaleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SaleControllerTest {

    private SaleService saleService;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        saleService = mock(SaleService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SaleController(saleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void createsSaleAndReturnsReceipt() throws Exception {
        SaleReceiptResponse receipt = new SaleReceiptResponse(
                12,
                Instant.parse("2026-11-10T10:00:00Z"),
                "Walk-in Customer",
                new BigDecimal("30.00"),
                "Counter Staff",
                List.of(new SaleItemReceiptResponse(
                        7, "BOOK-7", "Sample Book", 2,
                        new BigDecimal("15.00"), new BigDecimal("30.00"))));
        when(saleService.create(any(SaleRequest.class), eq("counter-staff")))
                .thenReturn(receipt);

        mockMvc.perform(post("/api/sales")
                        .principal(() -> "counter-staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Walk-in Customer",
                                 "items":[{"productId":7,"quantity":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.saleDate").value("2026-11-10T10:00:00Z"))
                .andExpect(jsonPath("$.data.customerName").value("Walk-in Customer"))
                .andExpect(jsonPath("$.data.cashierName").value("Counter Staff"))
                .andExpect(jsonPath("$.data.totalAmount").value(30.00))
                .andExpect(jsonPath("$.data.items[0].productId").value(7))
                .andExpect(jsonPath("$.data.items[0].sku").value("BOOK-7"))
                .andExpect(jsonPath("$.data.items[0].title").value("Sample Book"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(15.00))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(30.00));

        verify(saleService).create(any(SaleRequest.class), eq("counter-staff"));
    }

    @Test
    void rejectsSaleWithNoItemsBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/sales")
                        .principal(() -> "counter-staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Walk-in Customer","items":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(saleService);
    }

    @Test
    void retrievesReceiptBySaleId() throws Exception {
        SaleReceiptResponse receipt = new SaleReceiptResponse(
                12,
                Instant.parse("2026-11-10T10:00:00Z"),
                "Walk-in Customer",
                new BigDecimal("30.00"),
                "Counter Staff",
                List.of());
        when(saleService.getReceipt(12)).thenReturn(receipt);

        mockMvc.perform(get("/api/sales/12/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.totalAmount").value(30.00));

        verify(saleService).getReceipt(12);
    }
}
