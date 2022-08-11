package com.virtuslab.gitmachete.frontend.ui.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.extensions.PluginId;
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
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GitMacheteErrorReportSubmitter.class, ApplicationInfo.class, PluginId.class, System.class, SystemUtils.class})
public class GitMacheteErrorReportSubmitterTest {

  // Used spy(...) here to still keep the original behaviour of the class and be able to stub getTemplateVariables method
  // PowerMock API documentation Section 13 [https://javadoc.io/static/org.mockito/mockito-core/4.6.1/org/mockito/Mockito.html]
  private static final GitMacheteErrorReportSubmitter reportSubmitter = PowerMockito
      .spy(new GitMacheteErrorReportSubmitter());
  private static final LambdaLogger errorReportSubmitterLogger = PowerMockito.mock(LambdaLogger.class);
  private static String defaultErrorReport;
  private static String defaultErrorReportWithTemplate;
  private static String errorReportWithStackTraceAndTitle;
  private static String systemLineSeparator = "\n";

  @BeforeClass
  @SneakyThrows
  public static void setUpStatic() {
    defaultErrorReport = IOUtils.resourceToString("/error_report_uri.txt", StandardCharsets.UTF_8).strip();
    defaultErrorReportWithTemplate = IOUtils.resourceToString("/error_report_uri_with_template.txt",
        StandardCharsets.UTF_8).strip();
    errorReportWithStackTraceAndTitle = IOUtils.resourceToString("/error_report_uri_with_stacktrace_title.txt",
        StandardCharsets.UTF_8).strip();
  }

  @Before
  public void setUp() {
    Whitebox.setInternalState(GitMacheteErrorReportSubmitter.class, errorReportSubmitterLogger);

    PowerMockito.mockStatic(ApplicationInfo.class);
    val applicationInfo = PowerMockito.mock(ApplicationInfo.class);
    PowerMockito.when(applicationInfo.getFullApplicationName()).thenReturn("mocked IntelliJ idea");
    PowerMockito.stub(PowerMockito.method(ApplicationInfo.class, "getInstance"))
        .toReturn(applicationInfo);

    PowerMockito.mockStatic(PluginId.class);
    PowerMockito.stub(PowerMockito.method(System.class, "lineSeparator"))
        .toReturn(systemLineSeparator);
    Whitebox.setInternalState(SystemUtils.class, "OS_NAME", "Mock OS X");
    Whitebox.setInternalState(SystemUtils.class, "OS_VERSION", "Hehe");
  }

  private IdeaLoggingEvent[] getMockEvents(String throwableText) {
    val customThrowable = new Throwable() {
      @Override
      public void printStackTrace(PrintWriter s) {
        s.print(throwableText);
      }
    };

    return new IdeaLoggingEvent[]{
        new IdeaLoggingEvent(/* message */ "", customThrowable)
    };
  }

  private String invokeGetReportBody(GitMacheteErrorReportSubmitter errorReportSubmitter, IdeaLoggingEvent[] events)
      throws Exception {
    String reportBody = Whitebox.invokeMethod(errorReportSubmitter, "getReportBody", events, /* additionalInfo */ null);

    Mockito
        .verify(errorReportSubmitterLogger,
            never().description("Reading the resource file with the bug template should NOT throw exceptions"))
        .error(anyString(), (Throwable) any());

    return reportBody;
  }

  @Test
  public void shouldConstructUriWithTemplate() throws Exception {
    val events = getMockEvents(/* throwableText */ "");
    val reportBody = invokeGetReportBody(reportSubmitter, events);

    URI uriWithReport = Whitebox.invokeMethod(reportSubmitter, "constructNewGitHubIssueUri", events, reportBody);
    Assert.assertEquals("URI should match default with report body", defaultErrorReportWithTemplate, uriWithReport.toString());
  }

  @Test
  public void shouldConstructUriWithNoTemplate() throws Exception {
    val events = getMockEvents(/* throwableText */ "");

    URI uriNoReport = Whitebox.invokeMethod(reportSubmitter, "constructNewGitHubIssueUri", events, /* report Body */ null);
    Assert.assertEquals("URI should match default with NO report body", defaultErrorReport, uriNoReport.toString());
  }

  @Test
  public void shouldConstructUriWithTitleAndStackTrace() throws Exception {
    String throwableText = "Title - top of the stacktrace${systemLineSeparator}stacktrace${systemLineSeparator}stacktrace";
    val events = getMockEvents(throwableText);
    val reportBody = invokeGetReportBody(reportSubmitter, events);

    URI uri = Whitebox.invokeMethod(reportSubmitter, "constructNewGitHubIssueUri", events, reportBody);
    Assert.assertEquals("URI should match report template with stacktrace and title", errorReportWithStackTraceAndTitle,
        uri.toString());
  }
}
