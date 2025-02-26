package xyz.dowob.filemanagement.service.serviceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import xyz.dowob.filemanagement.customenum.FileEnum;
import xyz.dowob.filemanagement.data.file.dto.FileEditDTO;
import xyz.dowob.filemanagement.data.file.dto.FileMetadataDTO;
import xyz.dowob.filemanagement.data.user.dto.RegisterDTO;
import xyz.dowob.filemanagement.data.user.dto.ResetPasswordDTO;
import xyz.dowob.filemanagement.entity.User;
import xyz.dowob.filemanagement.entity.UserFileMetadata;
import xyz.dowob.filemanagement.exception.ValidationException;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        // 創建匿名實現類，只實現必要的方法
        validationService = new ValidationService() {
            @Override
            public Mono<Void> validateRegisterDTO(RegisterDTO registerDTO) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateResetPasswordDTO(ResetPasswordDTO resetPasswordDTO) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateFileMetadataDTO(FileMetadataDTO fileMetadataDTO, User user) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> validateEditFileDTO(FileEditDTO fileEditDTO, boolean isFolder) {
                return Mono.empty();
            }

            @Override
            public Mono<UserFileMetadata> validateFileType(UserFileMetadata fileMetadata, FileEnum... fileType) {
                return null;
            }
        };
    }

    // 測試用的 DTO 類
    private static class TestDTO {
        private final String name;

        public TestDTO(String name) {
            this.name = name;
        }
    }

    private static class TestNullDTO {

    }

    @Nested
    @DisplayName("驗證非空對象測試")
    class validateNotNullTests {
        @Test
        @DisplayName("驗證空的傳輸數據對象，應該拋出空數據對象異常")
        void validateNotNull_WithNullDTO_ShouldReturnError() {
            // when
            Mono<Void> result = validationService.validateNotNull(null);

            // then
            StepVerifier.create(result).expectError(ValidationException.class).verify();
        }


        @Test
        @DisplayName("驗證非空的傳輸數據對象，應該正常完成")
        void validateNotNull_WithValidDTO_ShouldComplete() {
            // given
            TestDTO dto = new TestDTO("test");

            // when
            Mono<Void> result = validationService.validateNotNull(dto);

            // then
            StepVerifier.create(result).verifyComplete();
        }
    }

    @Nested
    @DisplayName("驗證指定欄位測試")
    class validSpecifyColumnTests {
        @Test
        @DisplayName("當傳輸對象為空時，應該拋出空數據對象異常")
        void validSpecifyColumn_WithNullDTO_ShouldReturnError() {
            // when
            Mono<Void> result = validationService.validSpecifyColumns(null, "name");

            // then
            StepVerifier
                    .create(result)
                    .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                    .verify();
        }

        @Test
        @DisplayName("當指定的字段不存在時，應該拋出字段不存在異常")
        void validSpecifyColumn_WithNonExistentColumn_ShouldReturnError() {
            // given
            TestDTO dto = new TestDTO("test");

            // when
            Mono<Void> result = validationService.validSpecifyColumns(dto, "nonexistent");

            // then
            StepVerifier
                    .create(result)
                    .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.COLUMN_NOT_FOUND)
                    .verify();
        }

        @Test
        @DisplayName("當傳輸對象內部無任何字段時，應該拋出空數據對象異常")
        void validSpecifyColumn_WithNullDeclaredFields_ShouldReturnError() {
            // given
            TestNullDTO dto = new TestNullDTO();

            // when
            Mono<Void> result = validationService.validSpecifyColumns(dto, "name");

            // then
            StepVerifier
                    .create(result)
                    .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.NULL_DTO)
                    .verify();
        }

        @Test
        @DisplayName("當傳輸對象找不到指定字段時，應該拋出字段不存在異常")
        void validSpecifyColumn_WithMissColumns_ShouldReturnError() {
            // given
            TestDTO dto = new TestDTO("test");

            // when
            Mono<Void> result = validationService.validSpecifyColumns(dto, "name", "age");

            // then
            StepVerifier
                    .create(result)
                    .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.COLUMN_NOT_FOUND)
                    .verify();
        }

        @Test
        @DisplayName("當指定字段為去空格後為空時，應該拋出空字段異常")
        void validSpecifyColumn_WithBlankField_ShouldReturnError() {
            // given
            TestDTO dto = new TestDTO("");
            TestDTO dto2 = new TestDTO(" ");

            // when
            Mono<Void> result = validationService.validSpecifyColumns(dto, "name");
            Mono<Void> result2 = validationService.validSpecifyColumns(dto2, "name");
            // then
            StepVerifier
                    .create(result)
                    .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.BLANK_FIELD)
                    .verify();
            StepVerifier
                    .create(result2)
                    .expectErrorMatches(throwable -> throwable instanceof ValidationException && ((ValidationException) throwable).getErrorCode() == ValidationException.ErrorCode.BLANK_FIELD)
                    .verify();

        }

        @Test
        @DisplayName("當找到指定字段時並字段不為空時，應該正常完成")
        void validSpecifyColumn_WithValidField_ShouldComplete() {
            // given
            TestDTO dto = new TestDTO("test");

            // when
            Mono<Void> result = validationService.validSpecifyColumns(dto, "name");

            // then
            StepVerifier.create(result).verifyComplete();
        }


    }
}