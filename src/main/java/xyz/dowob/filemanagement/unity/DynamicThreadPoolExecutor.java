package xyz.dowob.filemanagement.unity;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基於 ThreadPoolExecutor 的動態線程池執行器，根據系統狀態和任務負載自動調整線程池大小。
 * 此實現採用自適應策略，可根據 CPU 使用率和任務佇列負載動態擴展或縮減線程數量，
 * 避免系統負載過重並確保資源的有效利用。
 * <p>
 * 該線程池以較低優先級運行，特別適用於後台任務或非緊急處理場景。
 * 當系統負載較高時會自動縮減線程數量，當任務量增加時則適度擴展線程池。
 * 支援核心線程超時機制，在無任務時可將線程數縮減至零。
 *
 * <p>使用範例：</p>
 * <pre>
 * // 建立一個動態線程池，初始線程數為4，最大線程數為8
 * DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(
 *     4, 8, 60, TimeUnit.SECONDS, new LinkedBlockingQueue&lt;&gt;(100)
 * );
 * 
 * // 提交任務，線程池會自動調整
 * executor.submit(() -&gt; {
 *     // 執行非緊急任務
 * });
 * </pre>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 **/
@SuppressWarnings("all")
public class DynamicThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * 低優先級模式下的 CPU 使用率閾值（30%）
     */
    private static final double LOW_PRIORITY_CPU_USAGE = 0.3;
    /**
     * 高優先級模式下的 CPU 使用率閾值（60%）
     */
    private static final double HIGH_PRIORITY_CPU_USAGE = 0.6;
    /**
     * 任務佇列高負載閾值，超過此比例時擴展線程池（70%）
     */
    private static final double HIGH_TASK_THRESHOLD = 0.7;
    /**
     * 任務佇列低負載閾值，低於此比例時縮減線程池（30%）
     */
    private static final double LOW_TASK_THRESHOLD = 0.3;
    /**
     * 原子計數器，記錄當前正在執行任務的線程數量
     */
    private final AtomicInteger activeThreadCount = new AtomicInteger(0);
    /**
     * 同步鎖，用於線程池大小調整時的併發控制
     */
    private final Object lock = new Object();
    /**
     * 工作佇列的總容量，-1 表示無限容量
     */
    private final int workQueueCapacity;
    /**
     * 系統可用處理器數量，透過 {@link Runtime#availableProcessors()} 取得
     */
    private int totalAvailableProcessors;
    /**
     * 線程池最小大小，預設為可用處理器數量的 10%，至少 2 個線程
     */
    private int minPoolSize;
    /**
     * 線程池最大大小，基於高優先級 CPU 使用率計算
     */
    private int maxPoolSize;
    /**
     * 理想線程池大小，根據當前優先級和 CPU 使用率動態調整
     */
    private int idealPoolSize;

    /**
     * 創建動態線程池執行器，根據系統狀態自動調整線程池大小。
     * <p>
     * 建構子會自動計算最佳的線程池參數設定：
     * <ul>
     *   <li><strong>最小線程數：</strong>系統處理器數量的 10%，至少 2 個線程</li>
     *   <li><strong>最大線程數：</strong>基於高優先級 CPU 使用率（60%）計算</li>
     *   <li><strong>理想線程數：</strong>基於低優先級 CPU 使用率（30%）計算</li>
     *   <li><strong>核心線程超時：</strong>自動啟用，確保空閒線程及時釋放</li>
     * </ul>
     * <p>
     * <strong>自動調整策略：</strong>
     * <ul>
     *   <li>佇列負載 > 70%：擴展線程池（系統資源允許時）</li>
     *   <li>佇列負載 < 30%：縮減線程池（不低於最小值）</li>
     *   <li>系統資源緊張：強制縮減線程池避免過載</li>
     *   <li>無任務時：線程數可縮減至 0</li>
     * </ul>
     * <p>
     * <strong>使用建議：</strong>
     * <ul>
     *   <li>建議使用有界佇列（如 ArrayBlockingQueue）以觸發自動調整</li>
     *   <li>核心線程數應設為預期的平均負載線程數</li>
     *   <li>最大線程數應考慮系統資源限制</li>
     *   <li>keepAliveTime 影響線程釋放速度，較長時間適合穩定負載</li>
     * </ul>
     *
     * @param corePoolSize 初始核心線程數，作為線程池調整的基準點
     * @param maximumPoolSize 最大線程數，系統會根據實際需要動態調整此值
     * @param keepAliveTime 空閒線程存活時間，若小於等於 0 則預設為 30 秒
     * @param unit 存活時間的時間單位，建議使用 SECONDS 或 MINUTES
     * @param workQueue 工作任務佇列，建議使用有界佇列以啟用自動調整功能
     */
    public DynamicThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime <= 0 ? 30 : keepAliveTime, unit, workQueue);
        this.workQueueCapacity = getWorkQueueCapacity(workQueue);
        this.totalAvailableProcessors = Runtime.getRuntime().availableProcessors();
        this.minPoolSize = Math.max(2, (int) (totalAvailableProcessors * 0.1));
        this.maxPoolSize = (int) (totalAvailableProcessors * HIGH_PRIORITY_CPU_USAGE);
        this.idealPoolSize = (int) (totalAvailableProcessors * LOW_PRIORITY_CPU_USAGE);
        setCorePoolSize(idealPoolSize);
        setMaximumPoolSize(maxPoolSize);
        allowCoreThreadTimeOut(true);
    }

    /**
     * 取得工作佇列的總容量。支援 ArrayBlockingQueue 和 LinkedBlockingQueue 類型佇列，
     * 對於無限容量佇列或不支援的佇列類型回傳 -1。
     *
     * @param queue 待檢查的工作佇列
     * @return 佇列總容量，無限容量或不支援時回傳 -1
     */
    static int getWorkQueueCapacity(BlockingQueue<Runnable> queue) {
        if (queue instanceof ArrayBlockingQueue<?> || queue instanceof LinkedBlockingQueue<?>) {
            if (queue.remainingCapacity() + queue.size() != Integer.MAX_VALUE) {
                return queue.size() + queue.remainingCapacity();
            }
        }
        return -1;
    }

    /**
     * 提交可執行任務，在執行前自動根據系統狀況調整線程池大小。
     *
     * @param task 待執行的任務
     * @return 代表任務執行狀態的 Future 對象
     */
    @Override
    public Future<?> submit(@NotNull Runnable task) {
        adjustPoolSize();
        return super.submit(task);
    }

    /**
     * 根據系統資源狀況和任務佇列負載自動調整線程池大小。
     * <p>
     * 此方法是動態線程池的核心邏輯，採用多層次的決策機制：
     * <p>
     * <strong>調整策略優先級：</strong>
     * <ol>
     *   <li><strong>高負載擴展：</strong>佇列負載 > 70% 且系統有餘裕（剩餘線程 > 2）時增加線程</li>
     *   <li><strong>低負載縮減：</strong>佇列負載 < 30% 且超過最小線程數時減少線程</li>
     *   <li><strong>資源保護：</strong>系統資源緊張（剩餘線程 < 2）時強制縮減</li>
     *   <li><strong>空閒清理：</strong>無任務且無活躍線程時將核心線程數設為 0</li>
     * </ol>
     * <p>
     * <strong>調整邏輯說明：</strong>
     * <ul>
     *   <li><strong>佇列負載計算：</strong>當前任務數 / 佇列總容量</li>
     *   <li><strong>系統餘裕評估：</strong>可用處理器數 - 活躍線程數</li>
     *   <li><strong>線程池邊界：</strong>始終維持在 [minPoolSize, maxPoolSize] 範圍內</li>
     *   <li><strong>調整幅度：</strong>每次調整僅增減 1 個線程，避免劇烈波動</li>
     * </ul>
     * <p>
     * <strong>特殊處理：</strong>
     * <ul>
     *   <li>無限容量佇列（如 LinkedBlockingQueue 無界）不觸發調整</li>
     *   <li>使用同步鎖確保調整過程的線程安全</li>
     *   <li>詳細的日誌記錄幫助監控和調試</li>
     * </ul>
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

            LogUnity.trace("當前線程池大小: %s, 當前任務數量: %s, 當前佇列大小: %s, 當前系統可用線程數: %s, 當前系統剩餘線程數: %s",
                           currentPoolSize,
                           activeThreads,
                           taskQueueSize,
                           availableProcessors,
                           remainingSystemThreads
            );

            if (taskQueueSize > workQueueCapacity * HIGH_TASK_THRESHOLD && remainingSystemThreads > 2) {
                int newPoolSize = Math.min(maxPoolSize, currentPoolSize + 1);
                String percentage = String.format("%.2f", queueLoadRatio * 100);
                LogUnity.info("當前任務隊列負載較高 ( %s %%)，增加線程池大小: " + currentPoolSize + " -> " + newPoolSize, percentage);

                setMaximumPoolSize(newPoolSize);
                setCorePoolSize(newPoolSize);
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
     * 執行指定任務，在執行前自動根據系統狀況調整線程池大小。
     *
     * @param command 待執行的任務命令
     */
    @Override
    public void execute(@NotNull Runnable command) {
        adjustPoolSize();
        super.execute(command);
    }

    /**
     * 任務執行前的回調方法，遞增活躍線程計數器。
     *
     * @param t 即將執行任務的線程
     * @param r 即將執行的任務
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        activeThreadCount.incrementAndGet();
        super.beforeExecute(t, r);
    }

    /**
     * 任務執行完成後的回調方法，遞減活躍線程計數器。
     *
     * @param r 已完成執行的任務
     * @param t 任務執行過程中發生的異常，若無異常則為 null
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        activeThreadCount.decrementAndGet();
        super.afterExecute(r, t);
    }

    /**
     * 動態更新線程池優先級和目標 CPU 使用率。根據優先級和指定的 CPU 使用率
     * 重新計算理想線程池大小，並調整核心線程數和最大線程數。
     *
     * @param highPriority       是否設為高優先級模式
     * @param cpuUsagePercentage 自訂 CPU 使用率（0.0-1.0），為 null 時使用預設值
     * @throws IllegalArgumentException 當 CPU 使用率不在 0-1 範圍內時拋出
     */
    public void updatePriority(boolean highPriority, @Nullable Double cpuUsagePercentage) {
        double chooseCpuUsage = cpuUsagePercentage == null ? (highPriority ? HIGH_PRIORITY_CPU_USAGE : LOW_PRIORITY_CPU_USAGE) : cpuUsagePercentage;

        if (chooseCpuUsage < 0 || chooseCpuUsage > 1) {
            throw new IllegalArgumentException("CPU 使用率必須在 0 到 1 之間");
        }

        idealPoolSize = (int) (totalAvailableProcessors * chooseCpuUsage);
        int targetMaxPoolSize = Math.max(idealPoolSize * 2, minPoolSize);

        if (getPoolSize() > idealPoolSize) {
            setCorePoolSize(idealPoolSize);
            setMaximumPoolSize(targetMaxPoolSize);
        } else {
            if (idealPoolSize < getCorePoolSize()) {
                setCorePoolSize(idealPoolSize);
            }

            setMaximumPoolSize(targetMaxPoolSize);
            if (idealPoolSize > getCorePoolSize()) {
                setCorePoolSize(idealPoolSize);
            }
        }
    }
}