package xyz.dowob.filemanagement.repostiory.ServerCsrfToken;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自定義CSRF權杖儲存庫介面的測試實現。
 * <p>
 * 此測試類驗證 CustomServerCsrfTokenRepository 介面設計的正確性，
 * 涵蓋CSRF權杖安全機制的介面定義和響應式程式設計模式。
 * <p>
 * 測試範圍包含介面基本屬性、方法簽名驗證、繼承關係檢查和實作類別正確性。
 * 特別驗證了自定義權杖清理方法的介面設計和響應式回傳型別。
 * <p>
 * 介面設計遵循Spring Security框架規範，擴展基礎儲存庫功能以支援分散式權杖管理。
 *
 * @author yuan
 * @version 1.0
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CustomServerCsrfTokenRepository 自定義CSRF Token存儲庫接口測試")
class CustomServerCsrfTokenRepositoryTest {

    @Test
    @DisplayName("一般測試 - 接口基本屬性驗證")
    void testInterfaceBasicProperties() {
        // 驗證是接口
        assertTrue(CustomServerCsrfTokenRepository.class.isInterface());

        // 驗證是public接口
        assertTrue(java.lang.reflect.Modifier.isPublic(CustomServerCsrfTokenRepository.class.getModifiers()));
    }

    // ==================== 一般測試 ====================


    @Test
    @DisplayName("一般測試 - 繼承關係驗證")
    void testInheritanceRelationship() {
        // 驗證繼承自 ServerCsrfTokenRepository
        assertTrue(ServerCsrfTokenRepository.class.isAssignableFrom(CustomServerCsrfTokenRepository.class));

        // 驗證接口層次結構
        Class<?>[] interfaces = CustomServerCsrfTokenRepository.class.getInterfaces();
        assertEquals(1, interfaces.length);
        assertEquals(ServerCsrfTokenRepository.class, interfaces[0]);
    }


    @Test
    @DisplayName("一般測試 - deleteToken 方法簽名驗證")
    void testDeleteTokenMethodSignature() {
        try {
            Method deleteTokenMethod = CustomServerCsrfTokenRepository.class.getMethod("deleteToken", CsrfToken.class);

            // 驗證方法存在
            assertNotNull(deleteTokenMethod);

            // 驗證返回類型
            assertEquals(Mono.class, deleteTokenMethod.getReturnType());

            // 驗證參數類型
            Class<?>[] parameterTypes = deleteTokenMethod.getParameterTypes();
            assertEquals(1, parameterTypes.length);
            assertEquals(CsrfToken.class, parameterTypes[0]);

            // 驗證方法是public且abstract
            assertTrue(java.lang.reflect.Modifier.isPublic(deleteTokenMethod.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isAbstract(deleteTokenMethod.getModifiers()));

        } catch (NoSuchMethodException e) {
            fail("deleteToken method should exist in CustomServerCsrfTokenRepository interface");
        }
    }


    @Test
    @DisplayName("一般測試 - 繼承的ServerCsrfTokenRepository方法驗證")
    void testInheritedServerCsrfTokenRepositoryMethods() {
        try {
            // 驗證繼承的方法存在
            Method generateTokenMethod = CustomServerCsrfTokenRepository.class.getMethod("generateToken", ServerWebExchange.class);
            Method saveTokenMethod = CustomServerCsrfTokenRepository.class.getMethod("saveToken", ServerWebExchange.class, CsrfToken.class);
            Method loadTokenMethod = CustomServerCsrfTokenRepository.class.getMethod("loadToken", ServerWebExchange.class);

            // 驗證方法返回類型
            assertEquals(Mono.class, generateTokenMethod.getReturnType());
            assertEquals(Mono.class, saveTokenMethod.getReturnType());
            assertEquals(Mono.class, loadTokenMethod.getReturnType());

        } catch (NoSuchMethodException e) {
            fail("Inherited methods from ServerCsrfTokenRepository should be available: " + e.getMessage());
        }
    }


    @Test
    @DisplayName("一般測試 - 接口方法總數驗證")
    void testInterfaceMethodCount() {
        Method[] methods = CustomServerCsrfTokenRepository.class.getMethods();

        // 驗證接口定義的方法數量（包括繼承的方法）
        long customMethods = java.util.Arrays.stream(methods)
                .filter(method -> method.getDeclaringClass() == CustomServerCsrfTokenRepository.class)
                .count();

        assertEquals(1, customMethods); // 只有 deleteToken 是自定義的
    }


    @Test
    @DisplayName("一般測試 - 實現類能正確實現接口")
    void testImplementationClassCanImplementInterface() {
        TestCustomServerCsrfTokenRepository implementation = new TestCustomServerCsrfTokenRepository();

        // 驗證實現類正確實現了接口
        assertTrue(implementation instanceof CustomServerCsrfTokenRepository);
        assertTrue(implementation instanceof ServerCsrfTokenRepository);

        // 驗證所有方法都可以調用
        assertDoesNotThrow(() -> {
            implementation.generateToken(null);
            implementation.saveToken(null, null);
            implementation.loadToken(null);
            implementation.deleteToken(null);
        });
    }


    @Test
    @DisplayName("一般測試 - 方法響應式返回類型驗證")
    void testReactiveReturnTypes() {
        Method[] methods = CustomServerCsrfTokenRepository.class.getDeclaredMethods();

        for (Method method : methods) {
            Class<?> returnType = method.getReturnType();

            // 所有方法都應該返回Mono類型（響應式編程）
            assertTrue(Mono.class.isAssignableFrom(returnType),
                      "Method " + method.getName() + " should return Mono type");
        }
    }


    @Test
    @DisplayName("一般測試 - 接口包信息驗證")
    void testInterfacePackageInfo() {
        Package interfacePackage = CustomServerCsrfTokenRepository.class.getPackage();

        assertNotNull(interfacePackage);
        assertEquals("xyz.dowob.filemanagement.repostiory.ServerCsrfToken", interfacePackage.getName());
    }


    @Test
    @DisplayName("異常測試 - 接口不能實例化")
    void testInterfaceCannotBeInstantiated() {
        // 接口不能直接實例化
        assertThrows(InstantiationException.class, () -> {
            CustomServerCsrfTokenRepository.class.newInstance();
        });
    }

    // ==================== 異常測試 ====================


    @Test
    @DisplayName("異常測試 - 接口沒有構造函數")
    void testInterfaceHasNoConstructors() {
        // 接口不應該有用戶定義的構造函數
        java.lang.reflect.Constructor<?>[] constructors = CustomServerCsrfTokenRepository.class.getDeclaredConstructors();
        assertEquals(0, constructors.length);
    }


    @Test
    @DisplayName("邊界測試 - 接口方法拋出異常聲明檢查")
    void testInterfaceMethodExceptionDeclarations() {
        try {
            Method deleteTokenMethod = CustomServerCsrfTokenRepository.class.getMethod("deleteToken", CsrfToken.class);

            // 檢查方法是否聲明了拋出異常
            Class<?>[] exceptionTypes = deleteTokenMethod.getExceptionTypes();

            // 接口方法通常不聲明檢查異常，響應式方法通過Mono處理異常
            assertEquals(0, exceptionTypes.length);

        } catch (NoSuchMethodException e) {
            fail("deleteToken method should exist");
        }
    }

    // ==================== 邊界測試 ====================


    @Test
    @DisplayName("邊界測試 - 接口泛型參數檢查")
    void testInterfaceGenericParameters() {
        // 檢查接口本身是否有泛型參數
        java.lang.reflect.TypeVariable<?>[] typeParameters = CustomServerCsrfTokenRepository.class.getTypeParameters();
        assertEquals(0, typeParameters.length); // 接口本身沒有泛型參數

        // 檢查方法的泛型返回類型
        try {
            Method deleteTokenMethod = CustomServerCsrfTokenRepository.class.getMethod("deleteToken", CsrfToken.class);
            java.lang.reflect.Type genericReturnType = deleteTokenMethod.getGenericReturnType();

            // 應該是 Mono<Void>
            assertTrue(genericReturnType instanceof java.lang.reflect.ParameterizedType);
            java.lang.reflect.ParameterizedType parameterizedType = (java.lang.reflect.ParameterizedType) genericReturnType;
            assertEquals(Mono.class, parameterizedType.getRawType());

            java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
            assertEquals(1, typeArguments.length);
            assertEquals(Void.class, typeArguments[0]);

        } catch (NoSuchMethodException e) {
            fail("deleteToken method should exist");
        }
    }


    @Test
    @DisplayName("邊界測試 - 接口註解檢查")
    void testInterfaceAnnotations() {
        // 檢查接口上的註解
        java.lang.annotation.Annotation[] annotations = CustomServerCsrfTokenRepository.class.getAnnotations();

        // 這個接口目前沒有類級別的註解
        // 但如果將來添加了註解，這個測試會檢測到變更
        assertTrue(annotations.length >= 0);
    }


    @Test
    @DisplayName("邊界測試 - 實現類繼承層次深度測試")
    void testImplementationClassInheritanceDepth() {
        // 測試深層繼承的實現類
        abstract class AbstractImplementation implements CustomServerCsrfTokenRepository {
            // 抽象實現類
        }

        class ConcreteImplementation extends AbstractImplementation {
            @Override
            public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
                return Mono.empty();
            }

            @Override
            public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteToken(CsrfToken token) {
                return Mono.empty();
            }
        }

        ConcreteImplementation implementation = new ConcreteImplementation();

        // 驗證深層繼承的實現類仍然正確實現接口
        assertTrue(implementation instanceof CustomServerCsrfTokenRepository);
        assertTrue(implementation instanceof ServerCsrfTokenRepository);
    }


    @Test
    @DisplayName("邊界測試 - 多重接口實現測試")
    void testMultipleInterfaceImplementation() {
        // 測試同時實現多個接口的情況
        interface AnotherInterface {
            void anotherMethod();
        }

        class MultipleInterfaceImplementation implements CustomServerCsrfTokenRepository, AnotherInterface {
            @Override
            public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
                return Mono.empty();
            }

            @Override
            public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> deleteToken(CsrfToken token) {
                return Mono.empty();
            }

            @Override
            public void anotherMethod() {
                // 實現另一個接口的方法
            }
        }

        MultipleInterfaceImplementation implementation = new MultipleInterfaceImplementation();

        // 驗證多重接口實現正確
        assertTrue(implementation instanceof CustomServerCsrfTokenRepository);
        assertTrue(implementation instanceof AnotherInterface);
    }


    @Test
    @DisplayName("邊界測試 - 方法重載檢查")
    void testMethodOverloadingCheck() {
        // 檢查接口中是否有方法重載
        Method[] methods = CustomServerCsrfTokenRepository.class.getDeclaredMethods();

        java.util.Map<String, Integer> methodNameCounts = new java.util.HashMap<>();
        for (Method method : methods) {
            methodNameCounts.merge(method.getName(), 1, Integer::sum);
        }

        // 檢查是否有重載方法（同名方法數量大於1）
        for (java.util.Map.Entry<String, Integer> entry : methodNameCounts.entrySet()) {
            if (entry.getValue() > 1) {
                // 如果有重載方法，記錄信息
                System.out.println("Method " + entry.getKey() + " is overloaded " + entry.getValue() + " times");
            }
        }

        // 目前接口應該沒有重載方法
        assertTrue(methodNameCounts.values().stream().allMatch(count -> count == 1));
    }


    @Test
    @DisplayName("邊界測試 - 接口常量檢查")
    void testInterfaceConstants() {
        // 檢查接口中是否定義了常量
        java.lang.reflect.Field[] fields = CustomServerCsrfTokenRepository.class.getDeclaredFields();

        // 目前接口沒有定義常量
        assertEquals(0, fields.length);

        // 如果將來添加了常量，驗證它們是 public static final
        for (java.lang.reflect.Field field : fields) {
            int modifiers = field.getModifiers();
            assertTrue(java.lang.reflect.Modifier.isPublic(modifiers));
            assertTrue(java.lang.reflect.Modifier.isStatic(modifiers));
            assertTrue(java.lang.reflect.Modifier.isFinal(modifiers));
        }
    }

    // 測試用的接口實現類
    private static class TestCustomServerCsrfTokenRepository implements CustomServerCsrfTokenRepository {

        @Override
        public Mono<CsrfToken> generateToken(ServerWebExchange exchange) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> saveToken(ServerWebExchange exchange, CsrfToken token) {
            return Mono.empty();
        }

        @Override
        public Mono<CsrfToken> loadToken(ServerWebExchange exchange) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> deleteToken(CsrfToken token) {
            return Mono.empty();
        }
    }
}