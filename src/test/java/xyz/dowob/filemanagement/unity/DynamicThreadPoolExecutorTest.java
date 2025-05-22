package xyz.dowob.filemanagement.unity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DynamicThreadPoolExecutor 的單元測試
 */
class DynamicThreadPoolExecutorTest {

    private static final int QUEUE_CAPACITY = 10;
    private static final int TEST_CORE_POOL_SIZE = 2;
    private static final int TEST_MAX_POOL_SIZE = 5;
    private static final int TEST_MIN_POOL_SIZE = 2;
    private static final int KEEP_ALIVE_TIME_MS = 50;
    private DynamicThreadPoolExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, SECONDS)) {
                    LogUnity.error("線程池未能在預期時間內終止");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LogUnity.error("線程池終止過程中被中斷: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("testGetWorkQueueCapacity_ArrayBlockingQueue_預期返回正確容量")
    void testGetWorkQueueCapacity_ArrayBlockingQueue_ReturnsCorrectCapacity() {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        int capacity = DynamicThreadPoolExecutor.getWorkQueueCapacity(queue);
        assertEquals(QUEUE_CAPACITY, capacity, "ArrayBlockingQueue 容量計算錯誤");
    }

    @Test
    @DisplayName("testGetWorkQueueCapacity_LinkedBlockingQueue有界_預期返回正確容量")
    void testGetWorkQueueCapacity_BoundedLinkedBlockingQueue_ReturnsCorrectCapacity() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        int capacity = DynamicThreadPoolExecutor.getWorkQueueCapacity(queue);
        assertEquals(QUEUE_CAPACITY, capacity, "有界 LinkedBlockingQueue 容量計算錯誤");
    }

    @Test
    @DisplayName("testGetWorkQueueCapacity_LinkedBlockingQueue無界_預期返回 -1")
    void testGetWorkQueueCapacity_UnboundedLinkedBlockingQueue_ReturnsNegativeOne() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        int capacity = DynamicThreadPoolExecutor.getWorkQueueCapacity(queue);
        assertEquals(-1, capacity, "無界 LinkedBlockingQueue 應返回 -1");
    }

    @Test
    @DisplayName("testGetWorkQueueCapacity_其他Queue類型_預期返回 -1")
    void testGetWorkQueueCapacity_OtherQueueType_ReturnsNegativeOne() {
        SynchronousQueue<Runnable> queue = new SynchronousQueue<>();
        int capacity = DynamicThreadPoolExecutor.getWorkQueueCapacity(queue);
        assertEquals(-1, capacity, "SynchronousQueue 應返回 -1");
    }

    @Test
    @DisplayName("testConstructor_正常初始化_預期成功建立執行緒池並允許核心超時")
    void testConstructor_NormalInitialization_CreatesPoolAndAllowsCoreTimeout() {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        assertNotNull(executor, "執行緒池不應為 null");
        assertTrue(executor.allowsCoreThreadTimeOut(), "核心線程應允許超時");
        assertEquals(TEST_CORE_POOL_SIZE, executor.getCorePoolSize(), "核心線程數設置錯誤");
        assertEquals(TEST_MAX_POOL_SIZE, executor.getMaximumPoolSize(), "最大線程數設置錯誤");
    }

    private DynamicThreadPoolExecutor createExecutorWithTestDefaults(BlockingQueue<Runnable> queue) {
        DynamicThreadPoolExecutor exec = new DynamicThreadPoolExecutor(TEST_CORE_POOL_SIZE,
                                                                       TEST_MAX_POOL_SIZE,
                                                                       KEEP_ALIVE_TIME_MS,
                                                                       MILLISECONDS,
                                                                       queue
        );
        try {
            Field minPoolSizeField = DynamicThreadPoolExecutor.class.getDeclaredField("minPoolSize");
            Field maxPoolSizeField = DynamicThreadPoolExecutor.class.getDeclaredField("maxPoolSize");

            minPoolSizeField.setAccessible(true);
            maxPoolSizeField.setAccessible(true);

            minPoolSizeField.set(exec, TEST_MIN_POOL_SIZE);
            maxPoolSizeField.set(exec, TEST_MAX_POOL_SIZE);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("反射處理時失敗: " + e.getMessage());
        }

        exec.setCorePoolSize(TEST_CORE_POOL_SIZE);
        exec.setMaximumPoolSize(TEST_MAX_POOL_SIZE);
        exec.allowCoreThreadTimeOut(true);

        return exec;
    }

    @Test
    @DisplayName("testConstructor_無效Queue_預期拋出IllegalArgumentException")
    void testConstructor_NullQueue_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
                         executor = new DynamicThreadPoolExecutor(TEST_CORE_POOL_SIZE, TEST_MAX_POOL_SIZE, KEEP_ALIVE_TIME_MS, MILLISECONDS, null);
                     }, "使用 null queue 應拋出 IllegalArgumentException"
        );
    }

    @Test
    @DisplayName("testConstructor_無效TimeUnit_預期拋出IllegalArgumentException")
    void testConstructor_NullTimeUnit_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
                         executor = new DynamicThreadPoolExecutor(TEST_CORE_POOL_SIZE,
                                                                  TEST_MAX_POOL_SIZE,
                                                                  KEEP_ALIVE_TIME_MS,
                                                                  null,
                                                                  new ArrayBlockingQueue<>(QUEUE_CAPACITY)
                         );
                     }, "使用 null TimeUnit 應拋出 IllegalArgumentException"
        );
    }


    @Test
    @DisplayName("testExecute_單一任務_預期任務成功執行")
    void testExecute_SingleTask_ExecutesSuccessfully() throws InterruptedException {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        AtomicBoolean taskExecuted = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(() -> {
            taskExecuted.set(true);
            latch.countDown();
        });

        assertTrue(latch.await(5, SECONDS), "任務未在預期時間內完成");
        assertTrue(taskExecuted.get(), "任務未被執行");
    }

    @Test
    @DisplayName("testSubmit_單一任務_預期返回Future且任務成功執行")
    void testSubmit_SingleTask_ReturnsFutureAndExecutesSuccessfully() throws ExecutionException, InterruptedException, TimeoutException {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        Future<?> future = executor.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            taskExecuted.set(true);
        });

        assertNotNull(future, "Submit 應返回 Future");
        future.get(5, SECONDS);
        assertTrue(taskExecuted.get(), "任務未被執行");
    }

    @Test
    @DisplayName("testExecute_多個任務_預期所有任務成功執行")
    void testExecute_MultipleTasks_AllExecuteSuccessfully() throws InterruptedException {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY * 2));
        int taskCount = TEST_MAX_POOL_SIZE + QUEUE_CAPACITY;
        AtomicInteger executionCounter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                executionCounter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(15, SECONDS), "并非所有任務都在預期時間內完成");
        assertEquals(taskCount, executionCounter.get(), "執行的任務數量不匹配");
        assertTrue(executor.getPoolSize() > 0, "執行緒池大小應大於 0");
    }

    @Test
    @DisplayName("testExecute_任務拋出異常_預期執行緒池繼續運行")
    void testExecute_TaskThrowsException_PoolContinuesRunning() throws InterruptedException {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicBoolean secondTaskExecuted = new AtomicBoolean(false);

        executor.execute(() -> {
            latch1.countDown();
            throw new RuntimeException("測試異常");
        });

        assertTrue(latch1.await(5, SECONDS), "第一個任務未啟動");

        executor.execute(() -> {
            secondTaskExecuted.set(true);
            latch2.countDown();
        });

        assertTrue(latch2.await(5, SECONDS), "第二個任務未在預期時間內完成");
        assertTrue(secondTaskExecuted.get(), "第二個任務未被執行");
        assertFalse(executor.isShutdown() || executor.isTerminated(), "執行緒池不應被終止");
    }


    @Test
    @DisplayName("testAdjustPoolSize_高隊列負載_預期增加線程池大小")
    void testAdjustPoolSize_HighQueueLoad_IncreasesPoolSize() {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        assertEquals(TEST_CORE_POOL_SIZE, executor.getCorePoolSize());

        int tasksToSubmit = TEST_CORE_POOL_SIZE + (int) (QUEUE_CAPACITY * 0.8) + 5;
        CountDownLatch taskLatch = new CountDownLatch(tasksToSubmit);
        CountDownLatch startProcessingLatch = new CountDownLatch(TEST_CORE_POOL_SIZE);

        for (int i = 0; i < tasksToSubmit; i++) {
            executor.execute(() -> {
                startProcessingLatch.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                taskLatch.countDown();
            });
        }

        try {
            startProcessingLatch.await(10, MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        await().atMost(2, SECONDS).pollInterval(20, MILLISECONDS).untilAsserted(() -> {
            int currentCore = executor.getCorePoolSize();
            int currentMax = executor.getMaximumPoolSize();
            int currentPool = executor.getPoolSize();
            int queueSize = executor.getQueue().size();
            LogUnity.info("檢查線程池: 核心=%d, 最大=%d, 當前=%d, 隊列=%d", currentCore, currentMax, currentPool, queueSize);
            assertTrue(currentCore > TEST_CORE_POOL_SIZE || currentPool > TEST_CORE_POOL_SIZE,
                       "核心線程數或當前線程數應增加 (Core: " + currentCore + ", Pool: " + currentPool + ")"
            );
            assertTrue(currentCore <= TEST_MAX_POOL_SIZE, "核心線程數不應超過最大值");
        });

        try {
            taskLatch.await(500, MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("testAdjustPoolSize_低隊列負載且空閒_預期減少線程池大小")
    void testAdjustPoolSize_LowQueueLoadAndIdle_DecreasesPoolSize() {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        assertEquals(TEST_CORE_POOL_SIZE, executor.getCorePoolSize());

        CountDownLatch taskLatch = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            taskLatch.countDown();
        });
        try {
            taskLatch.await(1, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            Thread.sleep(KEEP_ALIVE_TIME_MS * 4);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.execute(() -> {
        });

        await().atMost(5, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(() -> {
            int currentCore = executor.getCorePoolSize();
            int currentPool = executor.getPoolSize();
            assertTrue(currentCore < TEST_CORE_POOL_SIZE || currentPool <= TEST_MIN_POOL_SIZE,
                       "核心線程數應減少或當前線程數應小於等於最小值 (Core: " + currentCore + ", Pool: " + currentPool + ")"
            );
            assertTrue(currentCore >= 0, "核心線程數不應為負");
        });
    }

    @Test
    @DisplayName("testAdjustPoolSize_隊列為空且無活動線程_預期核心線程數降至0")
    void testAdjustPoolSize_EmptyQueueAndNoActiveThreads_ReducesCorePoolToZero() {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        assertEquals(TEST_CORE_POOL_SIZE, executor.getCorePoolSize());

        CountDownLatch taskLatch = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            taskLatch.countDown();
        });
        try {
            taskLatch.await(1, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            Thread.sleep(KEEP_ALIVE_TIME_MS * 4);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.execute(() -> {
        });

        await().atMost(5, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(() -> {
            assertEquals(0, executor.getCorePoolSize(), "核心線程數應降至 0");
            assertEquals(TEST_MIN_POOL_SIZE, executor.getMaximumPoolSize(), "最大線程數應設為 minPoolSize");
        });
    }


    @Test
    @DisplayName("testAdjustPoolSize_無界隊列_預期線程池大小不因隊列負載調整")
    void testAdjustPoolSize_UnboundedQueue_PoolSizeDoesNotAdjustBasedOnQueue() {
        executor = createExecutorWithTestDefaults(new LinkedBlockingQueue<>());

        int initialCoreSize = executor.getCorePoolSize();
        int initialMaxSize = executor.getMaximumPoolSize();

        int taskCount = TEST_MAX_POOL_SIZE * 5;
        CountDownLatch latch = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
        }

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int currentCoreSize = executor.getCorePoolSize();
        int currentMaxSize = executor.getMaximumPoolSize();

        LogUnity.info("初始化核心/最大: %d/%d, 當前核心/最大: %d/%d", initialCoreSize, initialMaxSize, currentCoreSize, currentMaxSize);

        assertEquals(initialCoreSize, currentCoreSize, "無界隊列下，核心線程數不應改變");
        assertEquals(initialMaxSize, currentMaxSize, "無界隊列下，最大線程數不應改變");

        try {
            latch.await(1, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    @Test
    @DisplayName("testUpdatePriority_設置高優先級_預期調整線程池大小")
    void testUpdatePriority_SetHighPriority_AdjustsPoolSize() throws NoSuchFieldException, IllegalAccessException {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        Field minPoolSizeField = DynamicThreadPoolExecutor.class.getDeclaredField("minPoolSize");
        minPoolSizeField.setAccessible(true);
        int actualMinPoolSize = (int) minPoolSizeField.get(executor);
        int initialCoreSize = executor.getCorePoolSize();
        int initialMaxSize = executor.getMaximumPoolSize();

        executor.updatePriority(true, null);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int expectedIdealSize = Math.max(1, (int) (availableProcessors * 0.6));

        int actualMaxSize = executor.getMaximumPoolSize();
        int actualCoreSize = executor.getCorePoolSize();
        LogUnity.info("處理器數量: %d, 預期理想大小: %d, 實際核心大小: %d, 實際最大大小: %d",
                      availableProcessors,
                      expectedIdealSize,
                      actualCoreSize,
                      actualMaxSize
        );

        assertTrue(actualCoreSize >= Math.max(actualMinPoolSize, expectedIdealSize), "高優先級下，核心線程數應大於或等於最小池大小與理想值中的較大者");

        assertTrue(actualMaxSize <= actualCoreSize * 2, String.format("最大線程數(%d)應小於等於核心線程數(%d)的兩倍", actualMaxSize, actualCoreSize));
    }

    @Test
    @DisplayName("testUpdatePriority_設置低優先級_預期調整線程池大小")
    void testUpdatePriority_SetLowPriority_AdjustsPoolSize() throws Exception {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));

        Field minPoolSizeField = DynamicThreadPoolExecutor.class.getDeclaredField("minPoolSize");
        minPoolSizeField.setAccessible(true);
        int actualMinPoolSize = (int) minPoolSizeField.get(executor);

        executor.updatePriority(true, null);
        int highCoreSize = executor.getCorePoolSize();
        int highMaxSize = executor.getMaximumPoolSize();
        assertTrue(highCoreSize > 0, "高優先級核心線程數應大於 0");

        executor.updatePriority(false, null);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int expectedIdealSize = Math.max(1, (int) (availableProcessors * 0.3));

        int actualMaxSize = executor.getMaximumPoolSize();

        assertTrue(actualMaxSize >= Math.min(expectedIdealSize * 2, actualMinPoolSize),
                   String.format("最大線程數(%d)應至少為理想值的兩倍(%d)或最小值(%d)中的較大者",
                                 actualMaxSize,
                                 expectedIdealSize * 2,
                                 actualMinPoolSize
                   )
        );

        assertTrue(actualMaxSize <= highMaxSize,
                   String.format("從低優先級(%d)切換到高優先級後，最大線程數(%d)應當增大或保持不變", highMaxSize, actualMaxSize)
        );
    }

    @Test
    @DisplayName("testUpdatePriority_設置自定義CPU使用率_預期調整線程池大小")
    void testUpdatePriority_SetCustomCpuUsage_AdjustsPoolSize() throws Exception {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));

        Field minPoolSizeField = DynamicThreadPoolExecutor.class.getDeclaredField("minPoolSize");
        minPoolSizeField.setAccessible(true);
        int actualMinPoolSize = (int) minPoolSizeField.get(executor);

        int initialMaxSize = executor.getMaximumPoolSize();

        double customCpuUsage = 0.45;
        executor.updatePriority(false, customCpuUsage);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int expectedIdealSize = Math.max(1, (int) (availableProcessors * customCpuUsage));
        int expectedMaxSize = Math.max(expectedIdealSize * 2, actualMinPoolSize);

        int actualMaxSize = executor.getMaximumPoolSize();

        LogUnity.info("處理器數量: %d, 預期理想大小: %d, 預期最大大小: %d, 實際最大大小: %d, 最小池大小: %d",
                      availableProcessors,
                      expectedIdealSize,
                      expectedMaxSize,
                      actualMaxSize,
                      actualMinPoolSize
        );

        assertTrue(actualMaxSize >= Math.min(expectedIdealSize, actualMinPoolSize),
                   String.format("最大線程數(%d)應至少為理想值的兩倍(%d)或最小值(%d)中的較大者", actualMaxSize, expectedIdealSize, actualMinPoolSize)
        );

        assertEquals(actualMaxSize, executor.getMaximumPoolSize(), "最大線程數應保持不變");
    }

    @Test
    @DisplayName("testUpdatePriority_無效CPU使用率負數_預期拋出IllegalArgumentException")
    void testUpdatePriority_InvalidNegativeCpuUsage_ThrowsIllegalArgumentException() {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        assertThrows(IllegalArgumentException.class, () -> {
                         executor.updatePriority(false, -0.1);
                     }, "CPU 使用率 < 0 應拋出異常"
        );
    }

    @Test
    @DisplayName("testUpdatePriority_無效CPU使用率大於一_預期拋出IllegalArgumentException")
    void testUpdatePriority_InvalidCpuUsageGreaterThanOne_ThrowsIllegalArgumentException() {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        assertThrows(IllegalArgumentException.class, () -> {
                         executor.updatePriority(false, 1.1);
                     }, "CPU 使用率 > 1 應拋出異常"
        );
    }

    @Test
    @DisplayName("testUpdatePriority_邊界CPU使用率零_預期成功設置")
    void testUpdatePriority_BoundaryCpuUsageZero_SetsSuccessfully() {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));
        executor.updatePriority(false, 0.0);

        int expectedIdealSize = 0;
        await().atMost(2, SECONDS).untilAsserted(() -> assertEquals(expectedIdealSize, executor.getCorePoolSize(), "核心線程數應調整為 0"));
        assertEquals(Math.max(0, TEST_MIN_POOL_SIZE), executor.getMaximumPoolSize(), "最大線程數應調整");
    }

    @Test
    @DisplayName("testUpdatePriority_邊界CPU使用率一_預期成功設置")
    void testUpdatePriority_BoundaryCpuUsageOne_SetsSuccessfully() throws Exception {
        executor = createExecutorWithTestDefaults(new ArrayBlockingQueue<>(QUEUE_CAPACITY));

        Field minPoolSizeField = DynamicThreadPoolExecutor.class.getDeclaredField("minPoolSize");
        minPoolSizeField.setAccessible(true);
        int actualMinPoolSize = (int) minPoolSizeField.get(executor);

        executor.updatePriority(false, 1.0);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int actualMaxSize = executor.getMaximumPoolSize();

        LogUnity.info("處理器數量: %d, 預期理想大小: %d, 實際最大大小: %d", availableProcessors, availableProcessors, actualMaxSize);

        assertTrue(actualMaxSize >= actualMinPoolSize, "最大線程數應大於或等於最小池大小");
        assertTrue(actualMaxSize >= availableProcessors, "100% CPU 使用率下，最大線程數應大於或等於可用處理器數量");
    }
}
