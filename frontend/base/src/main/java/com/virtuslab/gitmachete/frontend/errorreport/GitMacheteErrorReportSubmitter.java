package com.virtuslab.gitmachete.frontend.errorreport;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.awt.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.util.Consumer;
import com.intellij.util.ModalityUiUtil;
import io.vavr.collection.List;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

@CustomLog
@ExtensionMethod({Arrays.class, Objects.class})
public class GitMacheteErrorReportSubmitter extends ErrorReportSubmitter {

  public static final int MAX_GITHUB_URI_LENGTH = 8192;

  private final PlatformInfoProvider platformInfoProvider;

  public GitMacheteErrorReportSubmitter() {
    this(new PlatformInfoProvider());
  }

  GitMacheteErrorReportSubmitter(PlatformInfoProvider platformInfoProvider) {
    this.platformInfoProvider = platformInfoProvider;
  }

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
    val uriBuilder = new URIBuilder("https://github.com/VirtusLab/git-machete-intellij-plugin/issues/new");

    String title = events.stream()
        .map(event -> {
          if (event.getThrowable() != null) {
            return event.getThrowableText().lines().findFirst().orElse("").stripTrailing();
          }
          val message = event.getMessage();
          return message != null ? message.stripTrailing() : "";
        })
        .collect(Collectors.joining("; "));
    uriBuilder.setParameter("title", title);

    uriBuilder.setParameter("labels", "bug");

    URI uri;
    List<String> reportBodyLines = List.ofAll(getReportBody(events, additionalInfo).lines())
        .append("<placeholder-for-do-while>");
    do {
      // Let's cut the body gradually line-by-line until the resulting URI fits into the GitHub limits.
      // It's hard to predict the perfect exact cut in advance due to URL encoding.
      reportBodyLines = reportBodyLines.dropRight(1);
      uriBuilder.setParameter("body", reportBodyLines.mkString(System.lineSeparator()));
      uri = uriBuilder.build();
    } while (uri.toString().length() > MAX_GITHUB_URI_LENGTH);
    return uri;
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

    templateVariables.put("ide", platformInfoProvider.getIdeApplicationName());

    val pluginVersion = platformInfoProvider.getPluginVersion().requireNonNullElse("<unknown>");
    templateVariables.put("macheteVersion", pluginVersion);

    val osName = platformInfoProvider.getOSName().requireNonNullElse("");
    val osVersion = platformInfoProvider.getOSVersion().requireNonNullElse("");
    templateVariables.put("os", osName + " " + osVersion);

    templateVariables.put("additionalInfo", additionalInfo.requireNonNullElse("N/A"));

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
