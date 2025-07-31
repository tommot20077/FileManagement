package xyz.dowob.filemanagement.component.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.annotation.RequirePermission;
import xyz.dowob.filemanagement.customenum.PermissionEnum;
import xyz.dowob.filemanagement.customenum.RoleEnum;
import xyz.dowob.filemanagement.exception.ValidationException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * PermissionAspect 權限切面的單元測試類別。
 *
 * <p>此測試類全面驗證 PermissionAspect 切面的權限檢查機制，涵蓋多種場景和角色權限驗證。</p>
 *
 * <p>測試目標：
 * 
 *   - 驗證不同角色的權限檢查邏輯
 *   - 測試 Mono 和 Flux 響應式流的權限處理
 *   - 確保異常情況和邊界條件的正確處理
 * 
 * </p>
 *
 * <p>測試重點：
 * 
 *   - 權限檢查成功流程
 *   - 權限檢查失敗流程
 *   - 不同角色（管理員、普通用戶、訪客）的權限驗證
 *   - 異常和錯誤流的正確傳播
 * 
 * </p>
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@DisplayName("PermissionAspect 權限切面測試")
@ExtendWith(MockitoExtension.class)
class PermissionAspectTest {

    @InjectMocks
    private PermissionAspect permissionAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private RequirePermission requirePermission;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private GrantedAuthority grantedAuthority;

    private MockedStatic<ReactiveSecurityContextHolder> securityContextHolderMockedStatic;

    @BeforeEach
    void setUp() {
        securityContextHolderMockedStatic = Mockito.mockStatic(ReactiveSecurityContextHolder.class);
    }
    

    @Test
    @DisplayName("測試 Mono 返回值且管理員角色 - 權限檢查成功")
    void checkPermission_withMonoAndAdminRole_shouldSucceed() throws Throwable {
        setupDefaultSecurityContext();
        // 設置測試數據
        String testResult = "test result";
        Mono<String> monoResult = Mono.just(testResult);

        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});

        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectNext(testResult)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
    }


    private void setupDefaultSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.ADMIN.name());
    }


    @Test
    @DisplayName("測試 Flux 返回值且管理員角色 - 權限檢查成功")
    void checkPermission_withFluxAndAdminRole_shouldSucceed() throws Throwable {
        setupDefaultSecurityContext();
        // 設置測試數據
        String testResult1 = "test result 1";
        String testResult2 = "test result 2";
        Flux<String> fluxResult = Flux.just(testResult1, testResult2);
        
        when(proceedingJoinPoint.proceed()).thenReturn(fluxResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Flux<String>) result)
                .expectNext(testResult1)
                .expectNext(testResult2)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試 Mono 返回值且普通用戶缺乏權限 - 權限檢查失敗")
    void checkPermission_withMonoAndUserRoleWithoutPermission_shouldFail() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.MANAGE}); // USER角色沒有MANAGE權限
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.USER.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試 Flux 返回值且普通用戶缺乏權限 - 權限檢查失敗")
    void checkPermission_withFluxAndUserRoleWithoutPermission_shouldFail() throws Throwable {
        // 設置測試數據
        Flux<String> fluxResult = Flux.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(fluxResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.MANAGE}); // USER角色沒有MANAGE權限
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.USER.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Flux<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試訪客角色具有讀取權限 - 權限檢查成功")
    void checkPermission_withVisitorRole_shouldSucceedForReadPermission() throws Throwable {
        // 設置測試數據
        String testResult = "test result";
        Mono<String> monoResult = Mono.just(testResult);
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.VISITOR.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectNext(testResult)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試訪客角色缺乏寫入權限 - 權限檢查失敗")
    void checkPermission_withVisitorRole_shouldFailForWritePermission() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.WRITE});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.VISITOR.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試多重權限且管理員具有所有權限 - 權限檢查成功")
    void checkPermission_withMultiplePermissions_shouldSucceed() throws Throwable {
        // 設置測試數據
        String testResult = "test result";
        Mono<String> monoResult = Mono.just(testResult);
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ, PermissionEnum.WRITE});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.ADMIN.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectNext(testResult)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試多重權限但缺乏其中一個 - 權限檢查失敗")
    void checkPermission_withMultiplePermissions_shouldFailIfMissingOne() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ, PermissionEnum.MANAGE});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.USER.name()); // USER沒有MANAGE權限
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試空安全上下文且需要寫入權限 - 降級為訪客角色後失敗")
    void checkPermission_withEmptySecurityContext_shouldFailWithVisitorRole() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.WRITE});
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.empty());

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試空安全上下文且只需讀取權限 - 降級為訪客角色後成功")
    void checkPermission_withEmptySecurityContext_shouldSucceedForReadPermission() throws Throwable {
        // 設置測試數據
        String testResult = "test result";
        Mono<String> monoResult = Mono.just(testResult);
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.empty());

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectNext(testResult)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試空權限列表且需要寫入權限 - 降級為訪客角色後失敗")
    void checkPermission_withEmptyAuthorities_shouldFailWithVisitorRole() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.WRITE});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList()).when(authentication).getAuthorities();
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試不支持的返回類型 - 拋出執行時異常")
    void checkPermission_withUnsupportedReturnType_shouldThrowException() throws Throwable {
        // 設置測試數據
        String nonReactiveResult = "test result";
        
        when(proceedingJoinPoint.proceed()).thenReturn(nonReactiveResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});

        // 執行測試並驗證異常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            permissionAspect.checkPermission(proceedingJoinPoint, requirePermission)
        );

        assertEquals("權限檢查失敗", exception.getMessage());
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試方法執行拋出異常 - 包裝為執行時異常")
    void checkPermission_withProceedThrowsException_shouldThrowRuntimeException() throws Throwable {
        // 設置測試數據
        Exception originalException = new Exception("Original exception");
        
        when(proceedingJoinPoint.proceed()).thenThrow(originalException);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});

        // 執行測試並驗證異常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            permissionAspect.checkPermission(proceedingJoinPoint, requirePermission)
        );

        assertEquals("權限檢查失敗", exception.getMessage());
        assertEquals(originalException, exception.getCause());
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試進階用戶角色具有分享權限 - 權限檢查成功")
    void checkPermission_withAdvancedUserRole_shouldSucceedForSharePermission() throws Throwable {
        // 設置測試數據
        String testResult = "test result";
        Mono<String> monoResult = Mono.just(testResult);
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.SHARE});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.ADVANCED_USER.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectNext(testResult)
                .verifyComplete();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試進階用戶角色缺乏管理權限 - 權限檢查失敗")
    void checkPermission_withAdvancedUserRole_shouldFailForManagePermission() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.MANAGE});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.ADVANCED_USER.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試匿名用戶角色缺乏任何權限 - 權限檢查失敗")
    void checkPermission_withAnonymousRole_shouldFailForAnyPermission() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn(RoleEnum.ANONYMOUS.name());
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(ValidationException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試無效角色名稱 - 拋出異常")
    void checkPermission_withInvalidRoleName_shouldFailWithVisitorRole() throws Throwable {
        // 設置測試數據
        Mono<String> monoResult = Mono.just("test result");
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.WRITE});
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(Arrays.<GrantedAuthority>asList(grantedAuthority)).when(authentication).getAuthorities();
        when(grantedAuthority.getAuthority()).thenReturn("INVALID_ROLE");
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果 - 無效角色名稱會導致 IllegalArgumentException
        StepVerifier.create((Mono<String>) result)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試 Mono 錯誤流 - 正確傳播異常")
    void checkPermission_withMonoError_shouldPropagateError() throws Throwable {
        setupDefaultSecurityContext();
        // 設置測試數據
        RuntimeException originalError = new RuntimeException("Original error");
        Mono<String> monoResult = Mono.error(originalError);
        
        when(proceedingJoinPoint.proceed()).thenReturn(monoResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Mono<String>) result)
                .expectError(RuntimeException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("測試 Flux 錯誤流 - 正確傳播異常")
    void checkPermission_withFluxError_shouldPropagateError() throws Throwable {
        setupDefaultSecurityContext();
        // 設置測試數據
        RuntimeException originalError = new RuntimeException("Original error");
        Flux<String> fluxResult = Flux.error(originalError);
        
        when(proceedingJoinPoint.proceed()).thenReturn(fluxResult);
        when(requirePermission.value()).thenReturn(new PermissionEnum[]{PermissionEnum.READ});
        
        securityContextHolderMockedStatic.when(ReactiveSecurityContextHolder::getContext)
                .thenReturn(Mono.just(securityContext));

        // 執行測試
        Object result = permissionAspect.checkPermission(proceedingJoinPoint, requirePermission);

        // 驗證結果
        StepVerifier.create((Flux<String>) result)
                .expectError(RuntimeException.class)
                .verify();

        verify(proceedingJoinPoint).proceed();
    }

    @AfterEach
    void tearDown() {
        securityContextHolderMockedStatic.close();
    }
}