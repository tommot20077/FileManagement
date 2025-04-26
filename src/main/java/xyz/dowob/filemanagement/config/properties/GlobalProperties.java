package xyz.dowob.filemanagement.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 全局設定類，用於設定一些系統級別的參數
 *
 * @author yuan
 * @program FileManagement
 * @ClassName GlobalProperties
 * @create 2025/4/21
 * @Version 1.0
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "global")
public class GlobalProperties {
    /**
     * 請求限制器
     */
    private RequestLimiter requestLimiter = new RequestLimiter();

    /**
     * 請求轉發設定，會根據設定的請求頭來獲取用戶的真實 IP
     */
    private forwarded forwarded = new forwarded();

    @Data
    public static class RequestLimiter {
        /**
         * 是否啟用請求限制
         */
        private LimitType type = LimitType.redis;

        /**
         * 請求限制的上限，當請求次數超過這個值時，會返回 429 Too Many Requests 錯誤
         * 當設置為值小於等於 0 時，則使用預設值: Integer.MAX_VALUE
         */
        private int limit = 500;

        /**
         * 補充的令牌數量，會在 {@link #refillDuration} 內逐漸補充到達上限，默認為 -1 補充全部
         * 當設定為值小於等於 0 時，則補充的令牌數量為 {@link #limit}
         */
        private int refill = -1;

        /**
         * 補充週期，預設為 1 分鐘
         * 請求令牌會在這個週期內逐漸補充到達上限
         * 設置為值需大於 0 時，否則將拋出異常
         */
        private Duration refillDuration = Duration.ofMinutes(1);

        /**
         * 請求限制的清除時間
         * 在超過設定時間後，會清除用戶的請求限制
         * 設置為值需大於 0 時，否則將拋出異常
         */
        private Duration cleanInterval = Duration.ofMinutes(10);

        /**
         * 是否啟用禁止IP
         * 當設置為true時，若此IP在 {@link #banIpDuration} 內請求失敗的次數超過 {@link #failureCount}，則會暫時禁止IP訪問 {@link #banExpireDuration} 時間
         */
        private boolean enableBanIp = true;

        /**
         * 禁止IP的計算時間
         * 在本段時間內，請求失敗的次數若超過 {@link #failureCount}，則會禁止IP訪問
         * 設置為值需大於 0 時，否則將拋出異常
         */
        private Duration banIpDuration = Duration.ofMinutes(10);

        /**
         * 禁止IP的封禁時間
         * 當超過這段時間後，禁止IP的狀態會被清除
         * 設置為值需大於 0 時，否則將拋出異常
         */
        private Duration banExpireDuration = Duration.ofHours(1);

        /**
         * 允許的請求失敗次數
         * 當請求次數超過這個值時，會禁止IP訪問
         * 設置為值需大於 0 時，否則將拋出異常
         */
        private int failureCount = 5;


        /**
         * 限制類型
         */
        private enum LimitType {
            /**
             * 使用 Bucket4j 的 Redis 實現
             */
            redis,

            /**
             * 使用 Bucket4j 的 Local 實現
             */
            local,

            /**
             * 關閉請求限制
             */
            none
        }
    }

    @Data
    public static class forwarded {
        /**
         * 轉發的 IP 的請求頭名稱
         */
        private String xForwardedHeader = "X-Forwarded-For";

        /**
         * 真實的 IP 的請求頭名稱
         */
        private String xRealIpHeader = "X-Real-IP";
    }
}
