package com.trabajaya;

import com.trabajaya.jobs.JobRepository;
import com.trabajaya.jobs.JobService;
import com.trabajaya.shared.Db;
import com.trabajaya.shared.Principal;
import com.trabajaya.storage.ImageSanitizer;
import com.trabajaya.storage.MediaService;
import com.trabajaya.storage.ObjectStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MediaCompensationTest {
    @AfterEach void cleanSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void rolledBackTransactionDeletesUploadedObject() {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.provider()).thenReturn("local");
        Db db = mock(Db.class);
        ImageSanitizer sanitizer = mock(ImageSanitizer.class);
        when(sanitizer.sanitize(any())).thenReturn(new byte[] {1, 2});
        var service = new MediaService(storage, db, mock(JobService.class), mock(JobRepository.class), sanitizer);
        TransactionSynchronizationManager.initSynchronization();
        service.upload(new Principal(1, "CLIENTE", "Test"), new MockMultipartFile("file", new byte[] {1}));
        verify(storage).write(anyString(), any(), eq("image/png"));
        verify(storage, never()).delete(anyString());
        var callbacks = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, callbacks.size());
        callbacks.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(storage).delete(anyString());
    }

    @Test void committedTransactionKeepsObject() {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.provider()).thenReturn("local");
        ImageSanitizer sanitizer = mock(ImageSanitizer.class);
        when(sanitizer.sanitize(any())).thenReturn(new byte[] {1});
        var service = new MediaService(storage, mock(Db.class), mock(JobService.class), mock(JobRepository.class), sanitizer);
        TransactionSynchronizationManager.initSynchronization();
        service.upload(new Principal(1, "CLIENTE", "Test"), new MockMultipartFile("file", new byte[] {1}));
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        verify(storage, never()).delete(anyString());
    }

    @Test void directDatabaseFailureAttemptsCompensation() {
        ObjectStorage storage = mock(ObjectStorage.class);
        when(storage.provider()).thenReturn("local");
        Db db = mock(Db.class);
        when(db.update(startsWith("INSERT INTO media"), anyMap())).thenThrow(new IllegalStateException("test failure"));
        ImageSanitizer sanitizer = mock(ImageSanitizer.class);
        when(sanitizer.sanitize(any())).thenReturn(new byte[] {1});
        var service = new MediaService(storage, db, mock(JobService.class), mock(JobRepository.class), sanitizer);
        assertThrows(IllegalStateException.class, () -> service.upload(new Principal(1, "CLIENTE", "Test"), new MockMultipartFile("file", new byte[] {1})));
        verify(storage).delete(anyString());
    }
}
