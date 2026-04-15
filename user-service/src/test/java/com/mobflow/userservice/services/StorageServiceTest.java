package com.mobflow.userservice.services;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static com.mobflow.userservice.testsupport.UserServiceTestFixtures.avatarFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(minioClient);
        ReflectionTestUtils.setField(storageService, "bucket", "avatars");
        ReflectionTestUtils.setField(storageService, "publicUrl", "http://localhost:9000");
    }

    @Test
    void uploadAvatar_validFile_uploadsAndReturnsPublicUrl() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String url = storageService.uploadAvatar(avatarFile());

        assertThat(url).startsWith("http://localhost:9000/avatars/avatars/");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadAvatar_emptyFile_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storageService.uploadAvatar(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File must not be empty");
    }

    @Test
    void uploadAvatar_disallowedType_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.txt", "text/plain", "avatar".getBytes());

        assertThatThrownBy(() -> storageService.uploadAvatar(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void uploadAvatar_missingBucket_createsBucketAndPolicyBeforeUpload() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        storageService.uploadAvatar(avatarFile());

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void deleteAvatar_validUrl_removesObject() throws Exception {
        storageService.deleteAvatar("http://localhost:9000/avatars/avatars/existing.png");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
