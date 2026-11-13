package com.virtuslab.gitcore.impl.jgit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SimpleLruCache;
import org.eclipse.jgit.util.SystemReader;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@CustomLog
@SuppressWarnings({"allcheckers:argument", "nullness:argument"})
public final class JGitFileStoreAttributesWorkaround {

  private JGitFileStoreAttributesWorkaround() {}

  @UIThreadUnsafe
  public static void ensureSafeFor(Path path) {
    if (!SystemReader.getInstance().isWindows()) {
      return;
    }
    ensureSafeFor(path, /* fileStoreOverride */ null);
  }

  @UIThreadUnsafe
  @SuppressWarnings("IllegalCatch")
  static void ensureSafeFor(Path path, @Nullable FileStore fileStoreOverride) {
    try {
      Path dir = Files.isDirectory(path) ? path : path.getParent();
      if (dir == null) {
        return;
      }
      FileStore fileStore = fileStoreOverride != null ? fileStoreOverride : Files.getFileStore(dir);
      if (probeFailsWithNonIoException(fileStore)) {
        suppressFileStoreAttributesProbe(dir, fileStore);
      }
    } catch (Throwable ignored) {
      // If querying FileStore fails, JGit falls back to its default attributes.
    }
  }

  @UIThreadUnsafe
  @SuppressWarnings("IllegalCatch")
  private static boolean probeFailsWithNonIoException(FileStore fileStore) {
    try {
      fileStore.getAttribute("volume:vsn");
      return false;
    } catch (IOException e) {
      // IOException is caught cleanly by JGit's FS$FileStoreAttributes#getConfigKey.
      return false;
    } catch (Throwable t) {
      // Non-standard FileStores (like JetBrains' IjentNioFileStore used for WSL on Windows) throw
      // NotImplementedError or UnsupportedOperationException when querying "volume:vsn".
      // JGit's FS$FileStoreAttributes#getConfigKey only catches IOException, letting other Throwables escape
      // into CompletableFuture#supplyAsync and triggering an unhandled IDE fatal error via SLF4J LOG.error.
      return true;
    }
  }

  @UIThreadUnsafe
  @SuppressWarnings({"IllegalCatch", "unchecked"})
  private static void suppressFileStoreAttributesProbe(Path dir, FileStore fileStore) {
    try {
      Class<?> clazz = Class.forName("org.eclipse.jgit.util.FS$FileStoreAttributes");

      Field attributeCacheField = clazz.getDeclaredField("attributeCache");
      attributeCacheField.setAccessible(true);
      val attributeCache = (java.util.Map<FileStore, FS.FileStoreAttributes>) attributeCacheField.get(null);
      if (attributeCache != null) {
        attributeCache.put(fileStore, FS.FileStoreAttributes.FALLBACK_FILESTORE_ATTRIBUTES);
      }

      Field attrCacheByPathField = clazz.getDeclaredField("attrCacheByPath");
      attrCacheByPathField.setAccessible(true);
      val attrCacheByPath = (SimpleLruCache<Path, FS.FileStoreAttributes>) attrCacheByPathField.get(null);
      if (attrCacheByPath != null) {
        attrCacheByPath.put(dir.toAbsolutePath(), FS.FileStoreAttributes.FALLBACK_FILESTORE_ATTRIBUTES);
      }

      LOG.debug(() -> "Pre-populated JGit FileStoreAttributes fallback for ${dir} (FileStore: ${fileStore})");
    } catch (Throwable t) {
      LOG.debug(() -> "Failed to pre-populate JGit FileStoreAttributes fallback for ${dir}: ${t.getMessage()}");
    }
  }
}
