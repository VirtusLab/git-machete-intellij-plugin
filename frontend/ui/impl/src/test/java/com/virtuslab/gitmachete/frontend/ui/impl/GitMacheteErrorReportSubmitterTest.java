package com.virtuslab.gitmachete.frontend.ui.impl;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ApplicationInfo.class, PluginManagerCore.class, SystemUtils.class})
public class GitMacheteErrorReportSubmitterTest {

  private GitMacheteErrorReportSubmitter reportSubmitter;
  private static final LambdaLogger errorReportSubmitterLogger = PowerMockito.mock(LambdaLogger.class);

  @BeforeClass
  public static void setUpStatic() {
    Whitebox.setInternalState(GitMacheteErrorReportSubmitter.class, errorReportSubmitterLogger);
    Whitebox.setInternalState(SystemUtils.class, "OS_NAME", "Mock OS X");
    Whitebox.setInternalState(SystemUtils.class, "OS_VERSION", "Hehe");

    PowerMockito.mockStatic(ApplicationInfo.class);
    PowerMockito.mockStatic(PluginManagerCore.class);
  }

  @Before
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

  private IdeaLoggingEvent getMockEvent(String exceptionMessage, int stackLength) {
    val stackTraceElement = new StackTraceElement("com.virtuslab.DeclaringClass", "someMethod", "DeclaringClass.java",
        /* lineNumber */ 1234);
    val stackTrace = new StackTraceElement[stackLength];
    Arrays.fill(stackTrace, stackTraceElement);
    val exception = new RuntimeException(exceptionMessage);
    exception.setStackTrace(stackTrace);
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
    Assert.assertEquals(expectedUri("without_stack_trace"), uri.toString());
  }

  @Test
  public void shouldConstructUriWithStackTrace() throws Exception {
    val event10 = getMockEvent("exception message", 10);

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event10}, /* additionalInfo */ null);
    Assert.assertEquals(expectedUri("with_stack_trace"), uri.toString());
  }

  @Test
  public void shouldConstructUriForMultipleEvents() throws Exception {
    val event0 = getMockEvent("exception message", 0);
    val event10 = getMockEvent("another exception message", 10);

    URI uri = reportSubmitter.constructNewGitHubIssueUri(new IdeaLoggingEvent[]{event0, event10}, /* additionalInfo */ null);
    Assert.assertEquals(expectedUri("for_multiple_events"), uri.toString());
  }
}
