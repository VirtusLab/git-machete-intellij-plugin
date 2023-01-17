package com.virtuslab.gitmachete.frontend.ui.impl;

import static com.virtuslab.gitmachete.frontend.ui.impl.GitMacheteErrorReportSubmitter.MAX_GITHUB_URI_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
@PrepareForTest({ApplicationInfo.class, PluginManagerCore.class, SystemUtils.class})
public class GitMacheteErrorReportSubmitterTest {

  private GitMacheteErrorReportSubmitter reportSubmitter;
  private static final LambdaLogger errorReportSubmitterLogger = PowerMockito.mock(LambdaLogger.class);

  @BeforeAll
  public static void setUpStatic() {
    Whitebox.setInternalState(GitMacheteErrorReportSubmitter.class, errorReportSubmitterLogger);
    Whitebox.setInternalState(SystemUtils.class, "OS_NAME", "Mock OS X");
    Whitebox.setInternalState(SystemUtils.class, "OS_VERSION", "Hehe");

    PowerMockito.mockStatic(ApplicationInfo.class);
    PowerMockito.mockStatic(PluginManagerCore.class);
  }

  @BeforeEach
  public void setUp() {
    reportSubmitter = new GitMacheteErrorReportSubmitter();

    val applicationInfo = PowerMockito.mock(ApplicationInfo.class);
    PowerMockito.when(applicationInfo.getFullApplicationName()).thenReturn("mocked IntelliJ idea");
    PowerMockito.stub(PowerMockito.method(ApplicationInfo.class, "getInstance"))
        .toReturn(applicationInfo);

    val ideaPluginDescriptor = PowerMockito.mock(IdeaPluginDescriptor.class);
    PowerMockito.when(ideaPluginDescriptor.getVersion()).thenReturn("mock plugin version");
    PowerMockito.stub(PowerMockito.method(PluginManagerCore.class, "getPlugin"))
        .toReturn(ideaPluginDescriptor);
  }

  private StackTraceElement[] getMockStackTrace(int stackLength) {
    val stackTraceElement = new StackTraceElement("com.virtuslab.DeclaringClass", "someMethod", "DeclaringClass.java",
        /* lineNumber */ 1234);
    val stackTrace = new StackTraceElement[stackLength];
    Arrays.fill(stackTrace, stackTraceElement);

    return stackTrace;
  }

  private IdeaLoggingEvent getMockEvent(String exceptionMessage, int stackLength) {
    val exception = new RuntimeException(exceptionMessage);
    exception.setStackTrace(getMockStackTrace(stackLength));
    return new IdeaLoggingEvent("some message", exception);
  }

  private Exception getWrappedExceptions(Exception wrappedException, int exceptionNumber) {
    if (exceptionNumber == 0)
      return wrappedException;

    val furtherWrappedException = new RuntimeException("WrappedExceptionMessage", wrappedException);
    furtherWrappedException.setStackTrace(getMockStackTrace(30 + 2 * exceptionNumber));

    return getWrappedExceptions(furtherWrappedException, exceptionNumber - 1);
  }

  private IdeaLoggingEvent getWrappedExceptionsEvent(int exceptionNumber) {
    val rootCauseException = new RuntimeException("RootCauseMessage");
    rootCauseException.setStackTrace(getMockStackTrace(55));
    return new IdeaLoggingEvent("some message", getWrappedExceptions(rootCauseException, exceptionNumber));
  }

  private IdeaLoggingEvent getSuppressedExceptionsEvent() {
    val suppressedException1 = new RuntimeException("SuppressedExceptionMessage");
    suppressedException1.setStackTrace(getMockStackTrace(63));

    val suppressedException2 = new RuntimeException("SuppressedExceptionMessage");
    suppressedException2.setStackTrace(getMockStackTrace(68));

    val exception = new RuntimeException("ExceptionMessage");
    exception.setStackTrace(getMockStackTrace(57));
    exception.addSuppressed(suppressedException1);
    exception.addSuppressed(suppressedException2);

    return new IdeaLoggingEvent("some message", exception);
  }

  @SneakyThrows
  private static String expectedUri(String baseName) {
    return IOUtils.resourceToString("/expected-error-report-uris/${baseName}.txt", StandardCharsets.UTF_8).replace("\n", "");
  }

  @Test
  public void shouldConstructUriWithoutStackTrace() throws Exception {
    val event0 = getMockEvent("exception message", 0);

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event0}, /* additionalInfo */ null);
    assertEquals(expectedUri("without_stack_trace"), uri.toString());
  }

  @Test
  public void shouldConstructUriWithStackTrace() throws Exception {
    val event10 = getMockEvent("exception message", 10);

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event10}, /* additionalInfo */ null);
    assertEquals(expectedUri("with_stack_trace"), uri.toString());
  }

  @Test
  public void shouldConstructUriForMultipleEvents() throws Exception {
    val event0 = getMockEvent("exception message", 0);
    val event10 = getMockEvent("another exception message", 10);

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event0, event10}, /* additionalInfo */ null);
    assertEquals(expectedUri("for_multiple_events"), uri.toString());
  }

  @Test
  public void shouldShortenWrappedExceptionStacktrace() throws Exception {
    val event = getWrappedExceptionsEvent(5);

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event}, /* additionalInfo */ null);
    assertEquals(expectedUri("with_wrapped_exceptions"), uri.toString());
  }

  @Test
  public void shouldShortenSuppressedExceptionStacktrace() throws Exception {
    val event = getSuppressedExceptionsEvent();

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event}, /* additionalInfo */ null);
    assertEquals(expectedUri("with_suppressed_exceptions"), uri.toString());
  }

  @Test
  public void shouldNotConstructUriLongerThanGitHubLimit() throws Exception {
    val event = getMockEvent("exception message", 1000);

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event}, /* additionalInfo */ null);
    assertEquals(expectedUri("long_uri"), uri.toString());
    assertTrue(uri.toString().length() <= MAX_GITHUB_URI_LENGTH, "URI is longer than ${MAX_GITHUB_URI_LENGTH} bytes");
  }
}
