package com.atlascommerce.inventory_service.service;


import com.atlascommerce.inventory_service.dto.CreateInventoryItemRequest;
import com.atlascommerce.inventory_service.dto.InventoryItemResponse;
import com.atlascommerce.inventory_service.dto.StockOperationRequest;
import com.atlascommerce.inventory_service.entity.InventoryItem;
import com.atlascommerce.inventory_service.exception.DuplicateInventoryItemException;
import com.atlascommerce.inventory_service.exception.InsufficientStockException;
import com.atlascommerce.inventory_service.exception.InventoryItemNotFoundException;
import com.atlascommerce.inventory_service.mapper.InventoryItemMapper;
import com.atlascommerce.inventory_service.repository.InventoryItemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository repository;

    @Mock
    private InventoryItemMapper mapper;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem inventoryItem;
    private InventoryItemResponse response;

    @BeforeEach
    void setUp() {

        inventoryItem = InventoryItem.builder()
                .id(1L)
                .productId(101L)
                .sku("SKU-101")
                .availableQuantity(50)
                .reservedQuantity(0)
                .minimumStockLevel(5)
                .warehouseLocation("MADRID-WH-01")
                .lastUpdated(Instant.now())
                .build();

        response = new InventoryItemResponse(
                1L,
                101L,
                "SKU-101",
                50,
                0,
                5,
                "MADRID-WH-01",
                false,
                Instant.now()
        );
    }

    @Test
    void create_shouldCreateInventorySuccessfully() {

        CreateInventoryItemRequest request =
                new CreateInventoryItemRequest(
                        101L,
                        "SKU-101",
                        50,
                        5,
                        "MADRID-WH-01"
                );

        when(repository.existsByProductId(101L)).thenReturn(false);
        when(repository.existsBySku("SKU-101")).thenReturn(false);
        when(repository.save(any(InventoryItem.class))).thenReturn(inventoryItem);
        when(mapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        InventoryItemResponse result = inventoryService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.productId()).isEqualTo(101L);

        verify(repository).save(any(InventoryItem.class));
        verify(mapper).toResponse(any(InventoryItem.class));
    }

    @Test
    void create_shouldThrowException_whenProductAlreadyExists() {

        CreateInventoryItemRequest request =
                new CreateInventoryItemRequest(
                        101L,
                        "SKU-101",
                        50,
                        5,
                        "MADRID-WH-01"
                );

        when(repository.existsByProductId(101L)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.create(request))
                .isInstanceOf(DuplicateInventoryItemException.class)
                .hasMessageContaining("Inventory already exists");

        verify(repository, never()).save(any());
        verify(repository, never()).existsBySku(anyString());
    }

    @Test
    void getByProductId_shouldReturnInventorySuccessfully() {

        when(repository.findByProductId(101L)).thenReturn(Optional.of(inventoryItem));
        when(mapper.toResponse(inventoryItem)).thenReturn(response);

        InventoryItemResponse result =
                inventoryService.getByProductId(101L);

        assertThat(result).isNotNull();
        assertThat(result.productId()).isEqualTo(101L);
    }

    @Test
    void getByProductId_shouldThrowException_whenInventoryNotFound() {

        when(repository.findByProductId(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getByProductId(999L))
                .isInstanceOf(InventoryItemNotFoundException.class);
    }

    @Test
    void reserve_shouldReserveStockSuccessfully() {

        StockOperationRequest request =
                new StockOperationRequest(101L, 5);

        when(repository.findByProductId(101L))
                .thenReturn(Optional.of(inventoryItem));

        when(repository.save(any(InventoryItem.class)))
                .thenReturn(inventoryItem);

        when(mapper.toResponse(any(InventoryItem.class)))
                .thenReturn(response);

        inventoryService.reserve(request);

        assertThat(inventoryItem.getAvailableQuantity()).isEqualTo(45);
        assertThat(inventoryItem.getReservedQuantity()).isEqualTo(5);

        verify(repository).save(inventoryItem);
    }

    @Test
    void reserve_shouldThrowException_whenInsufficientStock() {

        StockOperationRequest request =
                new StockOperationRequest(101L, 999);

        when(repository.findByProductId(101L))
                .thenReturn(Optional.of(inventoryItem));

        assertThatThrownBy(() -> inventoryService.reserve(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(repository, never()).save(any());        
    }

    @Test
    void release_shouldReleaseReservedStockSuccessfully() {

        InventoryItem item = InventoryItem.builder()
                    .id(1L)
                    .productId(101L)
                    .sku("SKU-101")
                    .availableQuantity(45)
                    .reservedQuantity(5)
                    .minimumStockLevel(5)
                    .warehouseLocation("MADRID-WH-01")
                    .lastUpdated(Instant.now())
                .build();

        StockOperationRequest request = new StockOperationRequest(101L, 2);

        when(repository.findByProductId(101L)).thenReturn(Optional.of(item));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));

        when(mapper.toResponse(any(InventoryItem.class)))
                .thenReturn(response);

        inventoryService.release(request);

        verify(repository).save(argThat(saved ->
                saved.getAvailableQuantity() == 47 &&
                saved.getReservedQuantity() == 3
        ));
    }

    @Test
    void decrease_shouldDecreaseReservedStockSuccessfully() {
        InventoryItem item = InventoryItem.builder()
                    .id(1L)
                    .productId(101L)
                    .sku("SKU-101")
                    .availableQuantity(45)
                    .reservedQuantity(5)
                    .minimumStockLevel(5)
                    .warehouseLocation("MADRID-WH-01")
                    .lastUpdated(Instant.now())
                .build();

        StockOperationRequest request = new StockOperationRequest(101L, 3);

        when(repository.findByProductId(101L)).thenReturn(Optional.of(item));
        when(repository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        
        when(mapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        inventoryService.decrease(request);

        assertThat(item.getReservedQuantity()).isEqualTo(2);
        verify(repository).save(argThat(saved -> saved.getReservedQuantity() == 2));
    }

    @Test
    void create_shouldThrowException_whenSkuAlreadyExists() {

        CreateInventoryItemRequest request =
                new CreateInventoryItemRequest(
                        101L,
                        "SKU-101",
                        50,
                        5,
                        "MADRID-WH-01"
                );

        when(repository.existsByProductId(101L)).thenReturn(false);
        when(repository.existsBySku("SKU-101")).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.create(request))
                .isInstanceOf(DuplicateInventoryItemException.class)
                .hasMessageContaining("sku");

        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
        "5, 45, 5",    // quantityToReserve, expectedAvailable, expectedReserved
        "10, 40, 10",
        "50, 0, 50"
    })
    void reserve_shouldReserveStockSuccessfully(int quantity, int expectedAvailable, int expectedReserved) {
        StockOperationRequest request = new StockOperationRequest(101L, quantity);

        InventoryItem item = InventoryItem.builder()
                    .id(1L)
                    .productId(101L)
                    .sku("SKU-101")
                    .availableQuantity(50)
                    .reservedQuantity(0)
                    .minimumStockLevel(5)
                    .warehouseLocation("MADRID-WH-01")
                    .lastUpdated(Instant.now())
                .build();
        when(repository.findByProductId(101L)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.reserve(request);

        verify(repository).save(argThat(saved ->
            saved.getAvailableQuantity() == expectedAvailable &&
            saved.getReservedQuantity() == expectedReserved
        ));
    }

    @ParameterizedTest
    @MethodSource("exceptionCases")
    void shouldThrowExpectedException(long productId, int quantity, Class<? extends Throwable> expectedException) {

        if (productId == 999L) {
            when(repository.findByProductId(999L)).thenReturn(Optional.empty());
        } else {
            when(repository.findByProductId(productId))
                    .thenReturn(Optional.of(createTestItem(10, 0)));
        }

        assertThatThrownBy(() -> inventoryService.reserve(new StockOperationRequest(productId, quantity)))
                .isInstanceOf(expectedException);
    }

    static Stream<Arguments> exceptionCases() {
        return Stream.of(
            Arguments.of(101L, 999, InsufficientStockException.class),
            Arguments.of(999L, 5,   InventoryItemNotFoundException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("reserveTestCases")
    void reserve_shouldHandleVariousScenarios(StockTestCase testCase) {

        // Stub solo del findByProductId (común a todos los casos)
        when(repository.findByProductId(101L))
                .thenReturn(Optional.of(testCase.item()));

        if (testCase.shouldThrow()) {
            assertThatThrownBy(() -> inventoryService.reserve(testCase.request()))
                    .isInstanceOf(InsufficientStockException.class);

            // Verificamos que NO se haya llamado a save()
            verify(repository, never()).save(any());

        } else {
            // Solo en los casos de éxito stubbeamos el save
            when(repository.save(any(InventoryItem.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            inventoryService.reserve(testCase.request());

            verify(repository).save(argThat(saved ->
                    saved.getAvailableQuantity() == testCase.expectedAvailable() &&
                    saved.getReservedQuantity() == testCase.expectedReserved()
            ));
        }
    }

    // ==================== FUENTE DE DATOS ====================

    static Stream<StockTestCase> reserveTestCases() {
        return Stream.of(
            new StockTestCase(
                createTestItem(50, 0),
                new StockOperationRequest(101L, 5),
                45, 5,
                false,
                "Should reserve 5 units successfully"
            ),
            new StockTestCase(
                createTestItem(50, 0),
                new StockOperationRequest(101L, 10),
                40, 10,
                false,
                "Should reserve 10 units successfully"
            ),
            new StockTestCase(
                createTestItem(50, 0),
                new StockOperationRequest(101L, 60),
                0, 0,
                true,
                "Should throw exception when insufficient stock"
            )
        );
    }

    // Record (muy limpio)
    record StockTestCase(
            InventoryItem item,
            StockOperationRequest request,
            int expectedAvailable,
            int expectedReserved,
            boolean shouldThrow,
            String description
    ) {}

    // Helper
    private static InventoryItem createTestItem(int available, int reserved) {
        return InventoryItem.builder()
                .id(1L)
                .productId(101L)
                .sku("SKU-101")
                .availableQuantity(available)
                .reservedQuantity(reserved)
                .minimumStockLevel(5)
                .warehouseLocation("MADRID-WH-01")
                .lastUpdated(Instant.now())
                .build();
    }                  

    
}