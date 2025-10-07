package com.xiamen.metro.message.service;

import com.xiamen.metro.message.entity.FileEntity;
import com.xiamen.metro.message.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 文件管理服务测试
 *
 * @author Xiamen Metro System
 */
@ExtendWith(MockitoExtension.class)
class FileManagementServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private FileParseService fileParseService;

    @InjectMocks
    private FileManagementService fileManagementService;

    private FileEntity testFileEntity;
    private MultipartFile testMultipartFile;

    @BeforeEach
    void setUp() {
        testFileEntity = new FileEntity();
        testFileEntity.setId(1L);
        testFileEntity.setFileName("test_123456789.xlsx");
        testFileEntity.setOriginalFileName("test.xlsx");
        testFileEntity.setFileExtension("xlsx");
        testFileEntity.setFileSize(1024L);
        testFileEntity.setFileType(FileEntity.FileType.EXCEL);
        testFileEntity.setUploadStatus(FileEntity.UploadStatus.COMPLETED);
        testFileEntity.setProcessStatus(FileEntity.ProcessStatus.PENDING);

        testMultipartFile = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "test content".getBytes()
        );
    }

    @Test
    void testUploadFile_Success() {
        // Given
        when(fileRepository.findByFileHash(anyString())).thenReturn(Optional.empty());
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFileEntity);
        when(minioService.generateObjectName(anyString(), anyString())).thenReturn("test/path.xlsx");
        when(minioService.uploadFile(any(), anyString(), anyString(), anyLong())).thenReturn("http://minio.test/file.xlsx");

        // When
        FileEntity result = fileManagementService.uploadFile(testMultipartFile, 1L);

        // Then
        assertNotNull(result);
        assertEquals("test.xlsx", result.getOriginalFileName());
        assertEquals(FileEntity.FileType.EXCEL, result.getFileType());
        verify(fileRepository).save(any(FileEntity.class));
        verify(minioService).uploadFile(any(), anyString(), anyString(), anyLong());
    }

    @Test
    void testUploadFile_FileAlreadyExists() {
        // Given
        when(fileRepository.findByFileHash(anyString())).thenReturn(Optional.of(testFileEntity));

        // When
        FileEntity result = fileManagementService.uploadFile(testMultipartFile, 1L);

        // Then
        assertNotNull(result);
        assertEquals(testFileEntity.getId(), result.getId());
        verify(fileRepository, never()).save(any(FileEntity.class));
        verify(minioService, never()).uploadFile(any(), anyString(), anyString(), anyLong());
    }

    @Test
    void testUploadFile_InvalidFileType() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            fileManagementService.uploadFile(invalidFile, 1L);
        });
    }

    @Test
    void testUploadFile_FileTooLarge() {
        // Given
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[200 * 1024 * 1024] // 200MB
        );

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            fileManagementService.uploadFile(largeFile, 1L);
        });
    }

    @Test
    void testGetFileById_Success() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFileEntity));

        // When
        Optional<FileEntity> result = fileManagementService.getFileById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testFileEntity.getId(), result.get().getId());
        verify(fileRepository).findById(1L);
    }

    @Test
    void testGetFileById_NotFound() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<FileEntity> result = fileManagementService.getFileById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(fileRepository).findById(1L);
    }

    @Test
    void testDeleteFile_Success() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFileEntity));
        when(minioService.downloadFile(anyString())).thenReturn(new ByteArrayInputStream("test".getBytes()));

        // When
        assertDoesNotThrow(() -> {
            fileManagementService.deleteFile(1L);
        });

        // Then
        verify(minioService).deleteFile(anyString());
        verify(fileRepository).delete(testFileEntity);
    }

    @Test
    void testDeleteFile_NotFound() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            fileManagementService.deleteFile(1L);
        });
    }

    @Test
    void testProcessFileAsync_Success() {
        // Given
        FileEntity processingFile = new FileEntity();
        processingFile.setId(1L);
        processingFile.setFileName("test.xlsx");
        processingFile.setOriginalFileName("test.xlsx");
        processingFile.setFileType(FileEntity.FileType.EXCEL);
        processingFile.setStoragePath("http://minio.test/path");
        processingFile.setProcessStatus(FileEntity.ProcessStatus.PENDING);

        when(fileRepository.findById(1L)).thenReturn(Optional.of(processingFile));
        when(minioService.downloadFile(anyString())).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(fileParseService.parseExcel(any(), anyString())).thenReturn(
                new FileParseService.ParseResult(java.util.List.of(), java.util.List.of(), 100)
        );

        // When
        fileManagementService.processFileAsync(processingFile);

        // Then
        verify(fileRepository, atLeastOnce()).save(any(FileEntity.class));
        assertEquals(FileEntity.ProcessStatus.COMPLETED, processingFile.getProcessStatus());
        assertEquals(100, processingFile.getDataRowCount());
    }

    @Test
    void testProcessFileAsync_ParseError() {
        // Given
        FileEntity processingFile = new FileEntity();
        processingFile.setId(1L);
        processingFile.setFileName("test.xlsx");
        processingFile.setOriginalFileName("test.xlsx");
        processingFile.setFileType(FileEntity.FileType.EXCEL);
        processingFile.setStoragePath("http://minio.test/path");
        processingFile.setProcessStatus(FileEntity.ProcessStatus.PENDING);

        when(fileRepository.findById(1L)).thenReturn(Optional.of(processingFile));
        when(minioService.downloadFile(anyString())).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(fileParseService.parseExcel(any(), anyString()))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        fileManagementService.processFileAsync(processingFile);

        // Then
        verify(fileRepository, atLeastOnce()).save(any(FileEntity.class));
        assertEquals(FileEntity.ProcessStatus.FAILED, processingFile.getProcessStatus());
        assertTrue(processingFile.getErrorMessage().contains("处理失败"));
    }
}