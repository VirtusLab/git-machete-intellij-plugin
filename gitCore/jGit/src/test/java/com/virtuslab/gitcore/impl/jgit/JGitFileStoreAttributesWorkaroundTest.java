package com.virtuslab.gitcore.impl.jgit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SimpleLruCache;
import org.eclipse.jgit.util.SystemReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings({"allcheckers:argument", "nullness:argument"})
public class JGitFileStoreAttributesWorkaroundTest {

  @Test
  public void shouldPrepopulateFallbackWhenQueryingAttributeThrowsNonIoException(@TempDir Path tempDir) {
    withSimulatedWindows(() -> {
      FileStore unsupportedFileStore = new TestFileStore(new Error("An operation is not implemented: Not yet implemented"));

      assertFalse(isFileStorePrepopulated(unsupportedFileStore));
      assertFalse(isPathPrepopulated(tempDir));

      JGitFileStoreAttributesWorkaround.ensureSafeFor(tempDir, unsupportedFileStore);

      assertTrue(isFileStorePrepopulated(unsupportedFileStore));
      assertTrue(isPathPrepopulated(tempDir));
    });
  }

  @Test
  public void shouldPrepopulateFallbackWhenQueryingAttributeThrowsUnsupportedOperationException(@TempDir Path tempDir) {
    withSimulatedWindows(() -> {
      FileStore unsupportedFileStore = new TestFileStore(new UnsupportedOperationException("volume:vsn not supported"));

      assertFalse(isFileStorePrepopulated(unsupportedFileStore));

      JGitFileStoreAttributesWorkaround.ensureSafeFor(tempDir, unsupportedFileStore);

      assertTrue(isFileStorePrepopulated(unsupportedFileStore));
      assertTrue(isPathPrepopulated(tempDir));
    });
  }

  @Test
  public void shouldNotPrepopulateFallbackWhenQueryingAttributeThrowsIoException(@TempDir Path tempDir) {
    withSimulatedWindows(() -> {
      FileStore ioExceptionFileStore = new TestFileStore(new IOException("Disk error"));

      JGitFileStoreAttributesWorkaround.ensureSafeFor(tempDir, ioExceptionFileStore);

      assertFalse(isFileStorePrepopulated(ioExceptionFileStore));
      assertFalse(isPathPrepopulated(tempDir));
    });
  }

  @Test
  public void shouldNotPrepopulateFallbackWhenQueryingAttributeSucceeds(@TempDir Path tempDir) {
    withSimulatedWindows(() -> {
      FileStore validFileStore = new TestFileStore(/* getAttributeException */ null);

      JGitFileStoreAttributesWorkaround.ensureSafeFor(tempDir, validFileStore);

      assertFalse(isFileStorePrepopulated(validFileStore));
      assertFalse(isPathPrepopulated(tempDir));
    });
  }

  @Test
  public void shouldNoopOnNonWindowsSystems(@TempDir Path tempDir) {
    withSimulatedNonWindows(() -> {
      FileStore unsupportedFileStore = new TestFileStore(new Error("Not yet implemented"));

      JGitFileStoreAttributesWorkaround.ensureSafeFor(tempDir);

      assertFalse(isFileStorePrepopulated(unsupportedFileStore));
      assertFalse(isPathPrepopulated(tempDir));
    });
  }

  @SuppressWarnings({"IllegalCatch", "unchecked"})
  private static boolean isFileStorePrepopulated(FileStore fileStore) {
    try {
      Class<?> clazz = Class.forName("org.eclipse.jgit.util.FS$FileStoreAttributes");
      Field attributeCacheField = clazz.getDeclaredField("attributeCache");
      attributeCacheField.setAccessible(true);
      val attributeCache = (java.util.Map<FileStore, FS.FileStoreAttributes>) attributeCacheField.get(null);
      return attributeCache != null && attributeCache.containsKey(fileStore);
    } catch (Throwable t) {
      return false;
    }
  }

  @SuppressWarnings({"IllegalCatch", "unchecked"})
  private static boolean isPathPrepopulated(Path path) {
    try {
      Class<?> clazz = Class.forName("org.eclipse.jgit.util.FS$FileStoreAttributes");
      Field attrCacheByPathField = clazz.getDeclaredField("attrCacheByPath");
      attrCacheByPathField.setAccessible(true);
      val attrCacheByPath = (SimpleLruCache<Path, FS.FileStoreAttributes>) attrCacheByPathField.get(null);
      return attrCacheByPath != null && attrCacheByPath.get(path.toAbsolutePath()) != null;
    } catch (Throwable t) {
      return false;
    }
  }

  private void withSimulatedWindows(Runnable action) {
    withSimulatedOperatingSystem(/* isWindows */ true, action);
  }

  private void withSimulatedNonWindows(Runnable action) {
    withSimulatedOperatingSystem(/* isWindows */ false, action);
  }

  private void withSimulatedOperatingSystem(boolean isWindows, Runnable action) {
    SystemReader original = SystemReader.getInstance();
    try {
      SystemReader.setInstance(new SystemReader.Delegate(original) {
        @Override
        public boolean isWindows() {
          return isWindows;
        }
      });
      action.run();
    } finally {
      SystemReader.setInstance(original);
    }
  }

  private static final class TestFileStore extends FileStore {
    private final @Nullable Throwable getAttributeException;

    TestFileStore(@Nullable Throwable getAttributeException) {
      this.getAttributeException = getAttributeException;
    }

    @Override
    public String name() {
      return "test-store";
    }

    @Override
    public String type() {
      return "test-type";
    }

    @Override
    public boolean isReadOnly() {
      return false;
    }

    @Override
    public long getTotalSpace() {
      return 1024L;
    }

    @Override
    public long getUsableSpace() {
      return 1024L;
    }

    @Override
    public long getUnallocatedSpace() {
      return 1024L;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
      return false;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
      return false;
    }

    @Override
    public <V extends FileStoreAttributeView> @Nullable V getFileStoreAttributeView(Class<V> type) {
      return null;
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
      if (getAttributeException != null) {
        if (getAttributeException instanceof IOException) {
          throw (IOException) getAttributeException;
        } else if (getAttributeException instanceof RuntimeException) {
          throw (RuntimeException) getAttributeException;
        } else if (getAttributeException instanceof Error) {
          throw (Error) getAttributeException;
        }
      }
      return 12345;
    }
  }
}
