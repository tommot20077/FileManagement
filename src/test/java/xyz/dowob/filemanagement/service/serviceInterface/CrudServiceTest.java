package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CrudService 通用 CRUD 服務接口測試
 *
 * <p>測試 CrudService 通用 CRUD 服務接口的泛型契約和響應式 CRUD 操作模式，驗證接口在數據持久化層的設計正確性。
 * 
 * <p>測試涵蓋的接口方法：
 * <p>- create 實體創建方法的響應式實現
 * <p>- getById 主鍵查詢方法的泛型支持
 * <p>- getAll 全量查詢方法的 Flux 流處理
 * <p>- getAllByParams 條件查詢方法的參數化查詢
 * <p>- update 實體更新方法的響應式處理
 * <p>- delete 實體刪除方法的響應式實現
 * <p>- 泛型類型參數 T 和 ID 的正確使用
 * <p>- Mono 和 Flux 響應式類型的適當選擇
 *
 * 測試摘要：
 * 
 * 驗證 CrudService 接口作為通用數據訪問層的設計模式正確性，確保其泛型約定能夠為不同實體類型提供統一的 CRUD 操作能力。
 *
 * 前置條件：
 * - CrudService 泛型接口可用
 * - Reactor WebFlux 響應式編程環境可用
 * - JUnit 5 測試框架環境可用
 *
 * 測試步驟：
 * - 驗證接口方法簽名和泛型約定的正確性
 * - 測試 CRUD 操作的響應式流處理
 * - 驗證泛型類型參數的邊界和使用
 * - 測試異常處理和邊界條件
 *
 * 預期結果：
 * - 接口泛型約定符合 CRUD 服務模式
 * - 響應式流類型選擇正確合理
 * - 泛型參數使用靈活且類型安全
 * - 異常處理和邊界條件完善
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CrudService 通用CRUD服務接口測試")
class CrudServiceTest {

    private CrudService<TestEntity, Long> crudService;
    private TestEntity testEntity;


    @BeforeEach
    void setUp() {
        // 創建測試用的 CrudService 實現
        crudService = new CrudService<TestEntity, Long>() {
            @Override
            public Mono<TestEntity> create() {
                TestEntity entity = new TestEntity(1L, "新實體");
                return Mono.just(entity);
            }

            @Override
            public Mono<TestEntity> getById(Long id) {
                if (id == null) {
                    return Mono.empty();
                }
                if (id <= 0) {
                    return Mono.error(new IllegalArgumentException("ID必須大於0"));
                }
                return Mono.just(new TestEntity(id, "實體-" + id));
            }

            @Override
            public Flux<TestEntity> getAll() {
                return Flux.fromIterable(Arrays.asList(
                    new TestEntity(1L, "實體1"),
                    new TestEntity(2L, "實體2"),
                    new TestEntity(3L, "實體3")
                ));
            }

            @Override
            public Flux<TestEntity> getAllByParams(String type, Object... args) {
                if (type == null) {
                    return Flux.error(new IllegalArgumentException("類型不能為空"));
                }
                if ("name".equals(type) && args.length > 0) {
                    String nameFilter = args[0].toString();
                    return getAll().filter(entity -> entity.getName().contains(nameFilter));
                }
                if ("count".equals(type) && args.length > 0) {
                    int count = (Integer) args[0];
                    return getAll().take(count);
                }
                return getAll();
            }

            @Override
            public Mono<TestEntity> update(TestEntity entity) {
                if (entity == null) {
                    return Mono.error(new IllegalArgumentException("實體不能為空"));
                }
                if (entity.getId() == null) {
                    return Mono.error(new IllegalArgumentException("實體ID不能為空"));
                }
                entity.setDescription("已更新");
                return Mono.just(entity);
            }

            @Override
            public Mono<Void> delete(TestEntity entity) {
                if (entity == null) {
                    return Mono.error(new IllegalArgumentException("實體不能為空"));
                }
                if (entity.getId() == null) {
                    return Mono.error(new IllegalArgumentException("實體ID不能為空"));
                }
                return Mono.empty();
            }
        };

        testEntity = new TestEntity(1L, "測試實體", "測試描述");
    }


    @Test
    @DisplayName("一般測試 - create 方法基本功能")
    void testCreate_basicFunctionality() {
        StepVerifier.create(crudService.create())
                .assertNext(entity -> {
                    assertNotNull(entity);
                    assertEquals(1L, entity.getId());
                    assertEquals("新實體", entity.getName());
                })
                .verifyComplete();
    }

    // ==================== 一般測試 ====================


    @Test
    @DisplayName("一般測試 - getById 方法基本功能")
    void testGetById_basicFunctionality() {
        Long testId = 5L;

        StepVerifier.create(crudService.getById(testId))
                .assertNext(entity -> {
                    assertNotNull(entity);
                    assertEquals(testId, entity.getId());
                    assertEquals("實體-" + testId, entity.getName());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - getAll 方法基本功能")
    void testGetAll_basicFunctionality() {
        StepVerifier.create(crudService.getAll())
                .expectNextCount(3)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - getAllByParams 基本參數查詢")
    void testGetAllByParams_basicFunctionality() {
        StepVerifier.create(crudService.getAllByParams("name", "實體1"))
                .assertNext(entity -> {
                    assertNotNull(entity);
                    assertTrue(entity.getName().contains("實體1"));
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - update 方法基本功能")
    void testUpdate_basicFunctionality() {
        StepVerifier.create(crudService.update(testEntity))
                .assertNext(entity -> {
                    assertNotNull(entity);
                    assertEquals(testEntity.getId(), entity.getId());
                    assertEquals("已更新", entity.getDescription());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - delete 方法基本功能")
    void testDelete_basicFunctionality() {
        StepVerifier.create(crudService.delete(testEntity))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 接口方法簽名驗證")
    void testInterfaceMethodSignatures() {
        // 驗證 create 方法
        try {
            var createMethod = CrudService.class.getMethod("create");
            assertEquals(Mono.class, createMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("create 方法應該存在");
        }

        // 驗證 getById 方法
        try {
            var getByIdMethod = CrudService.class.getMethod("getById", Object.class);
            assertEquals(Mono.class, getByIdMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("getById 方法應該存在");
        }

        // 驗證 getAll 方法
        try {
            var getAllMethod = CrudService.class.getMethod("getAll");
            assertEquals(Flux.class, getAllMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("getAll 方法應該存在");
        }

        // 驗證 getAllByParams 方法
        try {
            var getAllByParamsMethod = CrudService.class.getMethod("getAllByParams", String.class, Object[].class);
            assertEquals(Flux.class, getAllByParamsMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("getAllByParams 方法應該存在");
        }

        // 驗證 update 方法
        try {
            var updateMethod = CrudService.class.getMethod("update", Object.class);
            assertEquals(Mono.class, updateMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("update 方法應該存在");
        }

        // 驗證 delete 方法
        try {
            var deleteMethod = CrudService.class.getMethod("delete", Object.class);
            assertEquals(Mono.class, deleteMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("delete 方法應該存在");
        }
    }


    @Test
    @DisplayName("一般測試 - 泛型類型參數驗證")
    void testGenericTypeParameters() {
        // 驗證接口有正確的泛型參數
        var typeParameters = CrudService.class.getTypeParameters();
        assertEquals(2, typeParameters.length);
        assertEquals("T", typeParameters[0].getName());
        assertEquals("ID", typeParameters[1].getName());
    }


    @Test
    @DisplayName("一般測試 - getAllByParams 計數參數查詢")
    void testGetAllByParams_countParameter() {
        StepVerifier.create(crudService.getAllByParams("count", 2))
                .expectNextCount(2)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - getAllByParams 無參數查詢")
    void testGetAllByParams_noParameters() {
        StepVerifier.create(crudService.getAllByParams("all"))
                .expectNextCount(3)
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - 響應式流鏈式操作")
    void testReactiveChaining() {
        Mono<TestEntity> result = crudService.create()
                .flatMap(entity -> crudService.update(entity))
                .doOnNext(entity -> assertNotNull(entity.getDescription()));

        StepVerifier.create(result)
                .assertNext(entity -> assertEquals("已更新", entity.getDescription()))
                .verifyComplete();
    }


    @Test
    @DisplayName("一般測試 - Flux 流操作")
    void testFluxOperations() {
        Flux<String> names = crudService.getAll()
                .map(TestEntity::getName)
                .filter(name -> name.contains("實體"));

        StepVerifier.create(names)
                .expectNext("實體1", "實體2", "實體3")
                .verifyComplete();
    }


    @Test
    @DisplayName("異常測試 - getById 傳入 null ID")
    void testGetById_withNullId() {
        StepVerifier.create(crudService.getById(null))
                .verifyComplete(); // 應該返回空的 Mono
    }

    // ==================== 異常測試 ====================


    @Test
    @DisplayName("異常測試 - getById 傳入無效 ID")
    void testGetById_withInvalidId() {
        StepVerifier.create(crudService.getById(-1L))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - getAllByParams 傳入 null 類型")
    void testGetAllByParams_withNullType() {
        StepVerifier.create(crudService.getAllByParams(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - update 傳入 null 實體")
    void testUpdate_withNullEntity() {
        StepVerifier.create(crudService.update(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - update 傳入無 ID 實體")
    void testUpdate_withEntityWithoutId() {
        TestEntity entityWithoutId = new TestEntity(null, "無ID實體");

        StepVerifier.create(crudService.update(entityWithoutId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - delete 傳入 null 實體")
    void testDelete_withNullEntity() {
        StepVerifier.create(crudService.delete(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - delete 傳入無 ID 實體")
    void testDelete_withEntityWithoutId() {
        TestEntity entityWithoutId = new TestEntity(null, "無ID實體");

        StepVerifier.create(crudService.delete(entityWithoutId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }


    @Test
    @DisplayName("異常測試 - getAllByParams 類型轉換錯誤")
    void testGetAllByParams_typeCastError() {
        // 測試預期會發生 ClassCastException 的情況
        assertThrows(ClassCastException.class, () -> {
            crudService.getAllByParams("count", "非數字").blockFirst();
        });
    }


    @Test
    @DisplayName("邊界測試 - getById 使用極大 ID 值")
    void testGetById_withMaxId() {
        StepVerifier.create(crudService.getById(Long.MAX_VALUE))
                .assertNext(entity -> {
                    assertEquals(Long.MAX_VALUE, entity.getId());
                    assertEquals("實體-" + Long.MAX_VALUE, entity.getName());
                })
                .verifyComplete();
    }

    // ==================== 邊界測試 ====================


    @Test
    @DisplayName("邊界測試 - getById 使用最小正數 ID")
    void testGetById_withMinPositiveId() {
        StepVerifier.create(crudService.getById(1L))
                .assertNext(entity -> {
                    assertEquals(1L, entity.getId());
                    assertEquals("實體-1", entity.getName());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - getAllByParams 使用空字符串類型")
    void testGetAllByParams_withEmptyType() {
        StepVerifier.create(crudService.getAllByParams(""))
                .expectNextCount(3)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - getAllByParams 使用大量參數")
    void testGetAllByParams_withManyParameters() {
        Object[] manyParams = new Object[100];
        for (int i = 0; i < 100; i++) {
            manyParams[i] = "param" + i;
        }

        StepVerifier.create(crudService.getAllByParams("test", manyParams))
                .expectNextCount(3)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - update 使用極長名稱的實體")
    void testUpdate_withVeryLongName() {
        String longName = "a".repeat(10000);
        TestEntity entityWithLongName = new TestEntity(1L, longName);

        StepVerifier.create(crudService.update(entityWithLongName))
                .assertNext(entity -> {
                    assertEquals(longName, entity.getName());
                    assertEquals("已更新", entity.getDescription());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 實體名稱包含特殊字符")
    void testUpdate_withSpecialCharactersInName() {
        String specialName = "實體<>&\"'`\n\t🔒";
        TestEntity entityWithSpecialName = new TestEntity(1L, specialName);

        StepVerifier.create(crudService.update(entityWithSpecialName))
                .assertNext(entity -> assertEquals(specialName, entity.getName()))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 併發多次 create 操作")
    void testConcurrentCreate() {
        List<Mono<TestEntity>> creates = Arrays.asList(
            crudService.create(),
            crudService.create(),
            crudService.create(),
            crudService.create(),
            crudService.create()
        );

        StepVerifier.create(Flux.merge(creates))
                .expectNextCount(5)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 大量實體的 getAll 操作")
    void testGetAll_largeDataset() {
        // 創建返回大量數據的 CrudService
        CrudService<TestEntity, Long> largeCrudService = new CrudService<TestEntity, Long>() {
            @Override
            public Mono<TestEntity> create() { return Mono.just(new TestEntity()); }
            @Override
            public Mono<TestEntity> getById(Long id) { return Mono.just(new TestEntity(id, "實體-" + id)); }
            @Override
            public Flux<TestEntity> getAll() {
                return Flux.range(1, 10000)
                        .map(i -> new TestEntity(i.longValue(), "實體-" + i));
            }
            @Override
            public Flux<TestEntity> getAllByParams(String type, Object... args) { return getAll(); }
            @Override
            public Mono<TestEntity> update(TestEntity entity) { return Mono.just(entity); }
            @Override
            public Mono<Void> delete(TestEntity entity) { return Mono.empty(); }
        };

        StepVerifier.create(largeCrudService.getAll().take(100))
                .expectNextCount(100)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 響應式流的超時處理")
    void testReactiveTimeout() {
        // 創建一個會延遲的 CrudService
        CrudService<TestEntity, Long> delayCrudService = new CrudService<TestEntity, Long>() {
            @Override
            public Mono<TestEntity> create() {
                return Mono.just(new TestEntity()).delayElement(Duration.ofMillis(100));
            }
            @Override
            public Mono<TestEntity> getById(Long id) { return Mono.just(new TestEntity()); }
            @Override
            public Flux<TestEntity> getAll() { return Flux.empty(); }
            @Override
            public Flux<TestEntity> getAllByParams(String type, Object... args) { return Flux.empty(); }
            @Override
            public Mono<TestEntity> update(TestEntity entity) { return Mono.just(entity); }
            @Override
            public Mono<Void> delete(TestEntity entity) { return Mono.empty(); }
        };

        StepVerifier.create(delayCrudService.create().timeout(Duration.ofSeconds(1)))
                .expectNextCount(1)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - getAllByParams 使用空參數數組")
    void testGetAllByParams_withEmptyArgs() {
        StepVerifier.create(crudService.getAllByParams("test", new Object[0]))
                .expectNextCount(3)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 驗證接口約定完整性")
    void testInterfaceContract() {
        // 驗證接口是 public 的
        assertTrue(java.lang.reflect.Modifier.isPublic(CrudService.class.getModifiers()));

        // 驗證接口是 interface
        assertTrue(CrudService.class.isInterface());

        // 驗證方法數量
        assertEquals(6, CrudService.class.getDeclaredMethods().length);

        // 驗證所有方法都是抽象的（接口中的非默認方法）
        long abstractMethodCount = Arrays.stream(CrudService.class.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier.isAbstract(method.getModifiers()))
                .count();
        assertEquals(6, abstractMethodCount);
    }


    @Test
    @DisplayName("邊界測試 - 泛型邊界測試")
    void testGenericBoundaries() {
        // 測試不同類型的 ID
        CrudService<TestEntity, String> stringIdService = new CrudService<TestEntity, String>() {
            @Override
            public Mono<TestEntity> create() { return Mono.just(new TestEntity()); }
            @Override
            public Mono<TestEntity> getById(String id) {
                return Mono.just(new TestEntity(Long.parseLong(id), "實體-" + id));
            }
            @Override
            public Flux<TestEntity> getAll() { return Flux.empty(); }
            @Override
            public Flux<TestEntity> getAllByParams(String type, Object... args) { return Flux.empty(); }
            @Override
            public Mono<TestEntity> update(TestEntity entity) { return Mono.just(entity); }
            @Override
            public Mono<Void> delete(TestEntity entity) { return Mono.empty(); }
        };

        StepVerifier.create(stringIdService.getById("123"))
                .assertNext(entity -> assertEquals("實體-123", entity.getName()))
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 響應式流的背壓處理")
    void testReactiveBackpressure() {
        Flux<TestEntity> manyEntities = crudService.getAll()
                .repeat(1000)
                .onBackpressureBuffer();

        StepVerifier.create(manyEntities.take(100))
                .expectNextCount(100)
                .verifyComplete();
    }


    @Test
    @DisplayName("邊界測試 - 錯誤處理和恢復")
    void testErrorHandlingAndRecovery() {
        Mono<TestEntity> resultWithFallback = crudService.getById(-1L)
                .onErrorReturn(new TestEntity(0L, "錯誤回退實體"));

        StepVerifier.create(resultWithFallback)
                .assertNext(entity -> {
                    assertEquals(0L, entity.getId());
                    assertEquals("錯誤回退實體", entity.getName());
                })
                .verifyComplete();
    }

    /**
     * 測試實體類
     */
    private static class TestEntity {
        private Long id;
        private String name;
        private String description;

        public TestEntity() {}

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public TestEntity(Long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }


        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestEntity that = (TestEntity) obj;
            return id != null ? id.equals(that.id) : that.id == null;
        }


        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", name='" + name + "', description='" + description + "'}";
        }
    }
}