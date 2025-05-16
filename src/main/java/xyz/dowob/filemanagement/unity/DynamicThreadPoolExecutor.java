package xyz.dowob.filemanagement.unity;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 動態線程池執行器，用於根據當前系統狀態和優先級調整線程池大小
 * 這個類繼承自 ThreadPoolExecutor，並加入一些自定義的邏輯
 * 這自定義線程池會以較低的優先級運行，並根據當前系統狀態調整線程池大小
 * 使其不會對系統造成過大的負擔，發生把整個系統拖慢的情況
 * 因此適合使用在一些不需要立即執行的任務上或是優先級較低的任務上
 * <p>
 * 特性：
 * - 自適應調整線程池大小，以避免系統負載過重
 * - 當任務隊列長度過高時，自動擴展線程池
 * - 當系統空閒或沒有任務時，自動縮減線程數量，甚至歸零
 * - 支援動態優先級變更，允許根據 CPU 使用率調整線程數量
 *
 * @author yuan
 * @program FileManagement
 * @ClassName DynamicThreadPoolExecutor
 * @create 2025/3/20
 * @Version 1.0
 **/
@SuppressWarnings("all")
public class DynamicThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * 記錄當前線程池執行中的任務數量
     */
    private final AtomicInteger activeThreadCount = new AtomicInteger(0);

    /**
     * 線程池鎖，用於同步線程池的狀態
     */
    private final Object lock = new Object();

    /**
     * 可用的 CPU 線程數量 {@link Runtime#availableProcessors()}
     */
    private int totalAvailableProcessors;

    /**
     * 低優先級時的 CPU 使用率（30%）
     */
    private static final double LOW_PRIORITY_CPU_USAGE = 0.3;

    /**
     * 高優先級時的 CPU 使用率（60%）
     */
    private static final double HIGH_PRIORITY_CPU_USAGE = 0.6;

    /**
     * 最小線程池大小，預設至少 2 個線程
     */
    private int minPoolSize;

    /**
     * 最大線程池大小，基於 {@link HIGH_PRIORITY_CPU_USAGE} 計算
     */
    private int maxPoolSize;

    /**
     * 理想的線程池大小，根據優先級動態調整
     */
    private int idealPoolSize;

    /**
     * 任務佇列的總容量
     */
    @Getter
    private final int workQueueCapacity;

    /**
     * 任務佇列使用率超過 80% 時擴展
     */
    private static final double HIGH_TASK_THRESHOLD = 0.8;

    /**
     * 任務佇列使用率低於 20% 時縮減
     */
    private static final double LOW_TASK_THRESHOLD = 0.2;

    /**
     * 創建動態線程池執行器，用於根據當前系統狀態和優先級調整線程池大小
     * 開啟核心線程超時機制，以避免空閒線程占用資源
     *
     * @param corePoolSize    初始核心線程數
     * @param maximumPoolSize 最大線程數
     * @param keepAliveTime   空閒線程的存活時間
     * @param unit            存活時間的單位
     * @param workQueue       任務佇列
     */
    public DynamicThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int keepAliveTime,
                                     @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime <= 0 ? 30 : keepAliveTime, unit, workQueue);
        this.workQueueCapacity = getWorkQueueCapacity(workQueue);
        this.totalAvailableProcessors = Runtime.getRuntime().availableProcessors();
        this.minPoolSize = Math.max(2, (int) (totalAvailableProcessors * 0.1));
        this.idealPoolSize = (int) (totalAvailableProcessors * LOW_PRIORITY_CPU_USAGE);
        this.maxPoolSize = (int) (totalAvailableProcessors * HIGH_PRIORITY_CPU_USAGE);
        setCorePoolSize(idealPoolSize);
        setMaximumPoolSize(maxPoolSize);
        allowCoreThreadTimeOut(true);
    }

    /**
     * 提交任務，並根據當前系統狀況自動調整線程池大小
     *
     * @param task 要執行的任務
     *
     * @return 返回 Future 對象
     */
    @Override
    public Future<?> submit(@NotNull Runnable task) {
        adjustPoolSize();
        return super.submit(task);
    }


    /**
     * 執行任務，並根據當前系統狀況自動調整線程池大小
     *
     * @param command 要執行的任務
     */
    @Override
    public void execute(@NotNull Runnable command) {
        adjustPoolSize();
        super.execute(command);
    }


    /**
     * 執行任務前的回調，記錄當前執行中的線程數
     *
     * @param t 執行任務的線程
     * @param r 即將執行的任務
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        activeThreadCount.incrementAndGet();
        super.beforeExecute(t, r);
    }


    /**
     * 執行任務後的回調，減少當前執行中的線程數
     *
     * @param r 執行完畢的任務
     * @param t 若任務執行時發生異常，則傳遞異常
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        activeThreadCount.decrementAndGet();
        super.afterExecute(r, t);
    }


    /**
     * 調整線程池大小，根據當前系統資源與任務佇列進行自動擴展與縮減
     * 當任務列隊的使用率過高時，自動擴展線程池(僅限有限對列，無限對列不會進行調整)
     */
    private void adjustPoolSize() {
        synchronized (lock) {
            if (workQueueCapacity == -1) {
                return;
            }

            int activeThreads = activeThreadCount.get();
            int currentPoolSize = getPoolSize();
            int taskQueueSize = getQueue().size();
            int availableProcessors = Runtime.getRuntime().availableProcessors();

            int idleThreads = currentPoolSize - activeThreads;
            int remainingSystemThreads = Math.max(availableProcessors - activeThreads, 1);

            double queueLoadRatio = (double) taskQueueSize / workQueueCapacity;


            if (taskQueueSize > workQueueCapacity * HIGH_TASK_THRESHOLD && remainingSystemThreads > 2) {
                int newPoolSize = Math.min(maxPoolSize, currentPoolSize + 1);
                String percentage = String.format("%.2f", queueLoadRatio * 100);
                LogUnity.info("當前任務隊列負載較高 ( %s %%)，增加線程池大小: " + currentPoolSize + " -> " + newPoolSize, percentage);
                setCorePoolSize(newPoolSize);
                setMaximumPoolSize(newPoolSize);
                return;
            }

            if (taskQueueSize < workQueueCapacity * LOW_TASK_THRESHOLD && currentPoolSize > minPoolSize) {
                int newPoolSize = Math.max(currentPoolSize - 1, minPoolSize);
                String percentage = String.format("%.2f", queueLoadRatio * 100);
                LogUnity.info("當前任務隊列負載較低 ( %s%%)，減少線程池大小: " + currentPoolSize + " -> " + newPoolSize, percentage);
                setCorePoolSize(newPoolSize);
                setMaximumPoolSize(newPoolSize);
                return;
            }

            if (remainingSystemThreads < 2 && taskQueueSize > workQueueCapacity * (HIGH_TASK_THRESHOLD + LOW_TASK_THRESHOLD) / 2) {
                int newPoolSize = Math.max(currentPoolSize - 1, minPoolSize);

                String format = "當前系統資源緊張，剩餘可用線程數: %s，減少線程池大小: " + currentPoolSize + " -> " + newPoolSize;
                LogUnity.warn(format, remainingSystemThreads);
                setCorePoolSize(newPoolSize);
                setMaximumPoolSize(newPoolSize);
                return;
            }

            if (taskQueueSize == 0 && activeThreads == 0) {
                LogUnity.debug("當前任務隊列為空，且沒有任務在執行，將線程池大小調整為 0");
                setCorePoolSize(0);
                setMaximumPoolSize(minPoolSize);
            }
        }
    }


    /**
     * 更新線程池的優先級與 CPU 使用率
     *
     * @param highPriority       是否提高優先級
     * @param cpuUsagePercentage 設定 CPU 使用率 (可選)
     */
    public void updatePriority(boolean highPriority, @Nullable Double cpuUsagePercentage) {
        double chooseCpuUsage = cpuUsagePercentage == null ? (highPriority ? HIGH_PRIORITY_CPU_USAGE : LOW_PRIORITY_CPU_USAGE) : cpuUsagePercentage;
        if (chooseCpuUsage < 0 || chooseCpuUsage > 1) {
            throw new IllegalArgumentException("CPU 使用率必須在 0 到 1 之間");
        }
        idealPoolSize = (int) (totalAvailableProcessors * chooseCpuUsage);

        if (getPoolSize() > idealPoolSize) {
            setCorePoolSize(idealPoolSize);
            setMaximumPoolSize(Math.max(idealPoolSize * 2, minPoolSize));
        }
    }


    /**
     * 取得佇列的總容量
     *
     * @param queue 任務佇列
     *
     * @return 佇列的總容量，若無法計算則回傳 -1
     */
    private static int getWorkQueueCapacity(BlockingQueue<Runnable> queue) {
        if (queue instanceof ArrayBlockingQueue<?> || queue instanceof LinkedBlockingQueue<?>) {
            return queue.size() + queue.remainingCapacity();
        }
        return -1;
    }
}