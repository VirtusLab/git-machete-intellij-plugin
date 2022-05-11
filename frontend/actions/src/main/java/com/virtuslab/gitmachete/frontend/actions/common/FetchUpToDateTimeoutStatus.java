package com.virtuslab.gitmachete.frontend.actions.common;

import git4idea.repo.GitRepository;

public final class FetchUpToDateTimeoutStatus {
  private FetchUpToDateTimeoutStatus() {}

  public static final long FETCH_ALL_UP_TO_DATE_TIMEOUT_MILLIS = 60 * 1000;

  @SuppressWarnings("ConstantName")
  private static final java.util.concurrent.ConcurrentMap<String, Long> lastFetchTimeMillisByRepositoryName = new java.util.concurrent.ConcurrentHashMap<>();

  public static boolean isUpToDate(GitRepository gitRepository) {
    String repoName = gitRepository.getRoot().getName();
    long lftm = lastFetchTimeMillisByRepositoryName.getOrDefault(repoName, 0L);
    return System.currentTimeMillis() < lftm + FETCH_ALL_UP_TO_DATE_TIMEOUT_MILLIS;
  }

  public static void update(String repoName) {
    lastFetchTimeMillisByRepositoryName.put(repoName, System.currentTimeMillis());
  }
}
