package com.virtuslab.gitmachete.frontend.ui.impl;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.awt.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.Consumer;
import com.intellij.util.ModalityUiUtil;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.utils.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

@CustomLog
@ExtensionMethod(Arrays.class)
public class GitMacheteErrorReportSubmitter extends ErrorReportSubmitter {

  @Override
  public String getReportActionText() {
    return getString("string.GitMachete.error-report-submitter.report-action-text");
  }

  @Override
  public boolean submit(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo,
      Component parentComponent,
      Consumer<? super SubmittedReportInfo> consumer) {
    try {
      val uri = constructNewGitHubIssueUri(events, additionalInfo);

      ModalityUiUtil.invokeLaterIfNeeded(ModalityState.NON_MODAL, () -> BrowserUtil.browse(uri));
    } catch (URISyntaxException e) {
      LOG.error("Cannot construct URI to open new bug issue!", e);
    }
    return true;
  }

  URI constructNewGitHubIssueUri(IdeaLoggingEvent[] events, @Nullable String additionalInfo) throws URISyntaxException {
    String title = events.stream()
        .map(event -> {
          val throwable = event.getThrowable();
          val exceptionMessage = event.getThrowableText().lines().findFirst().orElse("");
          return (throwable != null ? exceptionMessage : event.getMessage()).stripTrailing();
        })
        .collect(Collectors.joining("; "));
    String reportBody = getReportBody(events, additionalInfo);

    val uriBuilder = new URIBuilder("https://github.com/VirtusLab/git-machete-intellij-plugin/issues/new");
    uriBuilder.addParameter("title", title);
    uriBuilder.addParameter("labels", "bug");
    uriBuilder.addParameter("body", reportBody);
    return uriBuilder.build();
  }

  private String getReportBody(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo) {
    String reportBody = getBugTemplate();
    for (java.util.Map.Entry<String, String> entry : getTemplateVariables(events, additionalInfo).entrySet()) {
      reportBody = reportBody.replace("%${entry.getKey()}%", entry.getValue());
    }
    return reportBody;
  }

  // An error (from a typo in resource name) will be captured by the tests.
  @SneakyThrows
  private String getBugTemplate() {
    return IOUtils.resourceToString("/bug_report.md", StandardCharsets.UTF_8);
  }

  private java.util.Map<String, String> getTemplateVariables(
      IdeaLoggingEvent[] events,
      @Nullable String additionalInfo) {
    val templateVariables = new java.util.HashMap<String, String>();

    // IDE version, ie. Intellij Community 2021.3.1
    templateVariables.put("ide", ApplicationInfo.getInstance().getFullApplicationName());

    // Plugin version, ie. 1.1.1-10-SNAPSHOT+git.c9a0e89-dirty
    IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.virtuslab.git-machete"));
    templateVariables.put("macheteVersion", pluginDescriptor != null ? pluginDescriptor.getVersion() : "<unknown>");

    // OS name and version
    val osName = SystemUtils.OS_NAME != null ? SystemUtils.OS_NAME : "";
    val osVersion = SystemUtils.OS_VERSION != null ? SystemUtils.OS_VERSION : "";
    templateVariables.put("os", osName + " " + osVersion);

    // Additional info about error
    templateVariables.put("additionalInfo", additionalInfo != null ? additionalInfo : "N/A");

    // Messages and stacktraces for events
    val nl = System.lineSeparator();
    String stacktraces = events.stream()
        .map(event -> {
          // This message is distinct from the throwable's message:
          // in `LOG.error(message, throwable)`, it's the first parameter.
          val messagePart = event.getMessage() != null ? (event.getMessage() + nl + nl) : "";
          val throwablePart = shortenExceptionsStack(event.getThrowableText().stripTrailing());
          return "```${nl}${messagePart}${throwablePart}${nl}```";
        })
        .collect(Collectors.joining("${nl}${nl}"));
    templateVariables.put("stacktraces", stacktraces);

    return templateVariables;
  }

  private String shortenExceptionsStack(String stackTrace) {
    val nl = System.lineSeparator();
    val rootCauseIndex = Math.max(
        stackTrace.lastIndexOf("Caused by:"),
        stackTrace.lastIndexOf("\tSuppressed:"));

    if (rootCauseIndex != -1) {
      val rootCauseStackTrace = stackTrace.substring(rootCauseIndex);
      val lines = stackTrace.substring(0, rootCauseIndex).split(nl);

      StringBuilder resultString = new StringBuilder();
      for (int i = 0; i < lines.length; i++) {
        if (lines[i].contains("Caused by:") || lines[i].contains("Suppressed:") || i == 0) {
          resultString.append(lines[i]).append(nl);
          if (i + 1 < lines.length) {
            resultString.append("${lines[i+1]}...").append(nl);
          }
        }
      }
      return resultString.append(rootCauseStackTrace).toString();
    }
    return stackTrace;
  }
}
