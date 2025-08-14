/**
 * gRPC 控制器模組，實現基於 Protocol Buffers 的檔案處理服務。
 * <p>
 * 此模組提供完整的 gRPC 服務實現，基於 {@code file_processing.proto} 定義的服務契約，
 * 實現檔案管理系統的核心業務邏輯。透過高效的二進制協議和串流傳輸能力，
 * 為客戶端提供檔案上傳下載、資料夾管理、權限控制等全方位服務。
 * </p>
 * <p>
 * 核心控制器：
 * <ul>
 *   <li>{@link xyz.dowob.filemanagement.grpc.controller.GrpcFileController}：
 *       <ul>
 *         <li><strong>主要服務實現：</strong>繼承 {@link xyz.dowob.filemanagement.grpc.FileProcessingServiceGrpc.FileProcessingServiceImplBase}</li>
 *         <li><strong>檔案操作：</strong>上傳、下載、刪除、移動、複製</li>
 *         <li><strong>資料夾管理：</strong>創建、列表、權限控制</li>
 *         <li><strong>串流支援：</strong>大檔案分塊傳輸和進度追蹤</li>
 *         <li><strong>安全整合：</strong>與攔截器無縫配合，自動驗證用戶權限</li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * 基礎控制器架構：
 * <ul>
 *   <li>{@code BaseGrpcController}：提供通用的 gRPC 服務基礎功能</li>
 *   <li>{@code BaseGrpcFileController}：檔案操作的抽象基類</li>
 *   <li>{@code BaseGrpcFolderController}：資料夾操作的抽象基類</li>
 * </ul>
 * <p>
 * 服務功能覆蓋：
 * </p>
 * <ul>
 *   <li><strong>檔案上傳服務：</strong>
 *     <ul>
 *       <li>{@code uploadFile(StreamObserver)}} - 串流上傳，支援大檔案和斷點續傳</li>
 *       <li>{@code uploadFileSimple(UploadFileRequest)} - 簡單上傳，適合小檔案</li>
 *       <li>MD5 校驗和秒傳機制</li>
 *     </ul>
 *   </li>
 *   <li><strong>檔案下載服務：</strong>
 *     <ul>
 *       <li>{@code getFileContent(GetFileRequest)} - 串流下載</li>
 *       <li>{@code getFileMetadata(GetFileMetadataRequest)} - 檔案元資訊</li>
 *       <li>Range 請求支援，實現斷點續傳</li>
 *     </ul>
 *   </li>
 *   <li><strong>檔案管理服務：</strong>
 *     <ul>
 *       <li>{@code deleteFile(DeleteFileRequest)} - 檔案刪除</li>
 *       <li>{@code moveFile(MoveFileRequest)} - 檔案移動重命名</li>
 *       <li>{@code copyFile(CopyFileRequest)} - 檔案複製</li>
 *     </ul>
 *   </li>
 *   <li><strong>資料夾服務：</strong>
 *     <ul>
 *       <li>{@code listFolder(ListFolderRequest)} - 資料夾內容列表</li>
 *       <li>{@code createFolder(CreateFolderRequest)} - 資料夾創建</li>
 *     </ul>
 *   </li>
 *   <li><strong>認證授權服務：</strong>
 *     <ul>
 *       <li>{@code authenticateUser(AuthenticationRequest)} - 用戶認證</li>
 *       <li>{@code validateToken(TokenValidationRequest)} - 令牌驗證</li>
 *       <li>{@code checkPermission(PermissionRequest)} - 權限檢查</li>
 *     </ul>
 *   </li>
 *   <li><strong>檔案鎖定服務：</strong>
 *     <ul>
 *       <li>{@code lockFile(LockFileRequest)} - 檔案鎖定</li>
 *       <li>{@code unlockFile(UnlockFileRequest)} - 檔案解鎖</li>
 *     </ul>
 *   </li>
 *   <li><strong>搜尋服務：</strong>
 *     <ul>
 *       <li>{@code findFiles(FindFilesRequest)} - 條件查詢和分頁</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * 技術特點：
 * <ul>
 *   <li><strong>反應式設計：</strong>與 Spring WebFlux 整合，非阻塞處理</li>
 *   <li><strong>串流優化：</strong>大檔案傳輸記憶體使用量穩定</li>
 *   <li><strong>錯誤處理：</strong>統一的異常處理和 gRPC Status 映射</li>
 *   <li><strong>快取整合：</strong>多層次快取提升查詢效能</li>
 *   <li><strong>安全透明：</strong>依賴攔截器實現透明的安全驗證</li>
 * </ul>
 * <p>
 * 整合依賴：
 * <ul>
 *   <li><strong>服務層：</strong>依賴 {@code serviceInterface} 包的業務邏輯實現</li>
 *   <li><strong>策略模式：</strong>使用 {@code FileServiceStrategy} 分派不同檔案類型處理</li>
 *   <li><strong>權限控制：</strong>整合 {@code PermissionService} 實現細粒度授權</li>
 *   <li><strong>快取服務：</strong>依賴 {@code JwtCacheService} 和 {@code UserContextCacheService}</li>
 * </ul>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 * @see xyz.dowob.filemanagement.grpc.FileProcessingServiceGrpc
 * @see xyz.dowob.filemanagement.grpc.controller.GrpcFileController
 * @see net.devh.boot.grpc.server.service.GrpcService
 * @see io.grpc.stub.StreamObserver
 */
package xyz.dowob.filemanagement.grpc.controller;