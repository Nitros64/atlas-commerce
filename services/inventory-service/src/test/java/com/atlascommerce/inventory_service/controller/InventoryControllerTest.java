package com.atlascommerce.inventory_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.atlascommerce.inventory_service.dto.CreateInventoryItemRequest;
import com.atlascommerce.inventory_service.dto.InventoryItemResponse;
import com.atlascommerce.inventory_service.dto.StockOperationRequest;
import com.atlascommerce.inventory_service.service.InventoryService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryService inventoryService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        InventoryController controller =
                new InventoryController(inventoryService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    private InventoryItemResponse response() {
        return new InventoryItemResponse(
                1L,
                101L,
                "SKU-101",
                10,
                2,
                1,
                "A1",
                false,
                Instant.now()
        );
    }

    @Test
    void create_shouldReturnCreated() throws Exception {

        CreateInventoryItemRequest request =
                new CreateInventoryItemRequest(
                        101L,
                        "SKU-101",
                        10,
                        1,
                        "A1"
                );

        when(inventoryService.create(any()))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(101))
                .andExpect(jsonPath("$.sku").value("SKU-101"));
    }

    @Test
    void getByProductId_shouldReturnOk() throws Exception {

        when(inventoryService.getByProductId(101L))
                .thenReturn(response());

        mockMvc.perform(get("/api/v1/inventory/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(101));
    }

    @Test
    void reserve_shouldReturnOk() throws Exception {

        StockOperationRequest request =
                new StockOperationRequest(101L, 2);

        when(inventoryService.reserve(any()))
                .thenReturn(response());

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(101));
    }
}