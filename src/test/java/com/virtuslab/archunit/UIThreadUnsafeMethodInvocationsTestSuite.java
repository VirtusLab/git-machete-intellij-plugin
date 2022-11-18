package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import java.util.Arrays;

import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.vavr.collection.List;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.junit.Test;

import com.virtuslab.qual.guieffect.IgnoreUIThreadUnsafeCalls;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(Arrays.class)
public class UIThreadUnsafeMethodInvocationsTestSuite extends BaseArchUnitTestSuite {

  private static final String UIThreadUnsafeName = "@" + UIThreadUnsafe.class.getSimpleName();

  @Test
  public void ui_thread_unsafe_methods_should_not_be_uieffect() {
    methods()
        .that()
        .areAnnotatedWith(UIThreadUnsafe.class)
        .should()
        .notBeAnnotatedWith(UIEffect.class)
        .check(importedClasses);
  }

  private static List<String> extractWhitelistedMethodsFromAnnotation(JavaMethod method) {
    if (method.isAnnotatedWith(IgnoreUIThreadUnsafeCalls.class)) {
      return List.of(method.getAnnotationOfType(IgnoreUIThreadUnsafeCalls.class).value());
    } else {
      return List.empty();
    }
  }

  @Test
  public void only_ui_thread_unsafe_methods_should_call_other_ui_thread_unsafe_methods() {
    methods()
        .that()
        .areNotAnnotatedWith(UIThreadUnsafe.class)
        .should(new ArchCondition<JavaMethod>("never call any ${UIThreadUnsafeName} methods") {
          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            // This makes the check somewhat unsound (some non-UI-safe calls can slip under the radar in lambdas),
            // but annotating lambda methods isn't possible without expanding lambda into anonymous class...
            // which would in turn heavily reduce readability, hence potentially leading to bugs in the long run.
            if (method.getName().equals("$deserializeLambda$")) {
              return;
            }

            // AspectJ puts the original method body within an `<original-method-name>_aroundBodyX` synthetic method,
            // while the original method body is replaced with AspectJ-generated code
            // that includes the woven-in action (e.g. logging, in case of @Loggable)
            // and calls the actual logic in `..._aroundBodyX`.
            if (method.getName().matches("^.*_aroundBody[0-9]$")) {
              // An `..._aroundBodyX` method does NOT inherit the annotations of the original method,
              // so it won't be filtered out by `.areNotAnnotatedWith(UIThreadUnsafe.class)` condition.
              // This might cause a false positive: if the original method is annotated with @UIThreadUnsafe
              // AND its body has a call to an @UIThreadUnsafe-annotated method,
              // then ArchUnit will incorrectly report an error.

              // To prevent such cases, let's get the original method...
              Class<?>[] parameterClasses = method.getRawParameterTypes().stream()
                  .map(JavaClass::reflect)
                  // Let's skip `this` param
                  .skip(1)
                  // ...and the final param, both added by AspectJ to what's in the original method
                  .filter(clazz -> !clazz.getName().equals("org.aspectj.lang.JoinPoint"))
                  .toArray(Class[]::new);

              JavaMethod originalMethod = method.getOwner()
                  .tryGetMethod(method.getName().replaceAll("_aroundBody[0-9]$", ""), parameterClasses).orElse(null);

              // ...and check if the original method qualifies under `.areNotAnnotatedWith(UIThreadUnsafe.class)` condition.
              if (originalMethod != null && originalMethod.isAnnotatedWith(UIThreadUnsafe.class)) {
                return;
              }
            }

            val whitelistedMethodsFromAnnotation = extractWhitelistedMethodsFromAnnotation(method);
            method.getCallsFromSelf().forEach(call -> {
              AccessTarget calledMethod = call.getTarget();
              String calledMethodFullName = calledMethod.getFullName();
              if (calledMethod.isAnnotatedWith(UIThreadUnsafe.class)
                  && !whitelistedMethodsFromAnnotation.contains(calledMethodFullName)) {
                String message = "a non-${UIThreadUnsafeName} method ${method.getFullName()} " +
                    "calls a ${UIThreadUnsafeName} method ${calledMethodFullName}";
                events.add(SimpleConditionEvent.violated(method, message));
              }
            });
          }
        })
        .check(importedClasses);
  }

  private static final String[] whitelistedMethodFullNames_git4idea = {
      "git4idea.GitLocalBranch.getName()",
      "git4idea.GitRemoteBranch.getName()",
      "git4idea.GitUtil.findGitDir(com.intellij.openapi.vfs.VirtualFile)",
      "git4idea.GitUtil.getRepositories(com.intellij.openapi.project.Project)",
      "git4idea.GitUtil.getRepositoryManager(com.intellij.openapi.project.Project)",
      "git4idea.branch.GitBranchUtil.sortBranchNames(java.util.Collection)",
      "git4idea.branch.GitBrancher.createBranch(java.lang.String, java.util.Map)",
      "git4idea.branch.GitBrancher.getInstance(com.intellij.openapi.project.Project)",
      "git4idea.branch.GitBranchesCollection.findLocalBranch(java.lang.String)",
      "git4idea.branch.GitBranchesCollection.findRemoteBranch(java.lang.String)",
      "git4idea.branch.GitBranchesCollection.getLocalBranches()",
      "git4idea.branch.GitNewBranchDialog.<init>(com.intellij.openapi.project.Project, java.util.Collection, java.lang.String, java.lang.String, boolean, boolean, boolean)",
      "git4idea.branch.GitNewBranchDialog.showAndGetOptions()",
      "git4idea.branch.GitNewBranchOptions.getName()",
      "git4idea.branch.GitNewBranchOptions.shouldCheckout()",
      "git4idea.config.GitSharedSettings.getInstance(com.intellij.openapi.project.Project)",
      "git4idea.config.GitSharedSettings.isBranchProtected(java.lang.String)",
      "git4idea.config.GitVcsSettings.getInstance(com.intellij.openapi.project.Project)",
      "git4idea.config.GitVcsSettings.getRecentRootPath()",
      "git4idea.fetch.GitFetchResult.showNotification()",
      "git4idea.fetch.GitFetchSupport.fetchSupport(com.intellij.openapi.project.Project)",
      "git4idea.fetch.GitFetchSupport.isFetchRunning()",
      "git4idea.push.GitPushSource.create(git4idea.GitLocalBranch)",
      "git4idea.repo.GitRemote.getName()",
      "git4idea.repo.GitRepository.getBranches()",
      "git4idea.repo.GitRepository.getCurrentBranch()",
      "git4idea.repo.GitRepository.getProject()",
      "git4idea.repo.GitRepository.getRemotes()",
      "git4idea.repo.GitRepository.getRoot()",
      "git4idea.repo.GitRepository.getState()",
      "git4idea.repo.GitRepository.getVcs()",
      "git4idea.ui.ComboBoxWithAutoCompletion.<init>(javax.swing.ComboBoxModel, com.intellij.openapi.project.Project)",
      "git4idea.ui.ComboBoxWithAutoCompletion.addDocumentListener(com.intellij.openapi.editor.event.DocumentListener)",
      "git4idea.ui.ComboBoxWithAutoCompletion.getModel()",
      "git4idea.ui.ComboBoxWithAutoCompletion.getText()",
      "git4idea.ui.ComboBoxWithAutoCompletion.selectAll()",
      "git4idea.ui.ComboBoxWithAutoCompletion.setPlaceholder(java.lang.String)",
      "git4idea.ui.ComboBoxWithAutoCompletion.setPrototypeDisplayValue(java.lang.Object)",
      "git4idea.ui.ComboBoxWithAutoCompletion.setUI(javax.swing.plaf.ComboBoxUI)",
      "git4idea.validators.GitBranchValidatorKt.checkRefName(java.lang.String)"
  };

  // Some of these methods might actually access the filesystem;
  // still, they're lightweight enough so that we can give them a free pass.
  private static final String[] whitelistedMethodFullNames_java_io = {
      "java.io.File.canExecute()",
      "java.io.File.getAbsolutePath()",
      "java.io.File.isFile()",
      "java.io.File.toString()",
  };

  private static final String[] whitelistedMethodFullNames_java_nio = {
      "java.nio.file.Files.isRegularFile(java.nio.file.Path, [Ljava.nio.file.LinkOption;)",
      "java.nio.file.Files.readAttributes(java.nio.file.Path, java.lang.Class, [Ljava.nio.file.LinkOption;)",
      "java.nio.file.Files.setLastModifiedTime(java.nio.file.Path, java.nio.file.attribute.FileTime)",
      "java.nio.file.Path.getFileName()",
      "java.nio.file.Path.getParent()",
      "java.nio.file.Path.resolve(java.lang.String)",
      "java.nio.file.Path.toAbsolutePath()",
      "java.nio.file.Path.toFile()",
      "java.nio.file.Path.toString()",
      "java.nio.file.Paths.get(java.lang.String, [Ljava.lang.String;)",
      "java.nio.file.attribute.BasicFileAttributes.lastModifiedTime()",
      "java.nio.file.attribute.FileTime.fromMillis(long)",
      "java.nio.file.attribute.FileTime.toMillis()",
  };

  @Test
  public void only_ui_thread_unsafe_method_should_call_git4idea_or_io_methods() {
    methods()
        .that()
        .areNotAnnotatedWith(UIThreadUnsafe.class)
        .and()
        .areNotAnnotatedWith(IgnoreUIThreadUnsafeCalls.class)
        .should(new ArchCondition<JavaMethod>("never call any heavyweight git4idea or IO methods") {
          private void checkCallAgainstPackagePrefix(JavaMethod method, JavaCall<?> call, String packagePrefix,
              String[] whitelistedMethodsOfPackage, ConditionEvents events) {
            AccessTarget.CodeUnitCallTarget callTarget = call.getTarget();
            String calledMethodPackageName = callTarget.getOwner().getPackageName();
            String calledMethodFullName = callTarget.getFullName();

            val whitelistedMethodsFromAnnotation = extractWhitelistedMethodsFromAnnotation(method);
            if (calledMethodPackageName.startsWith(packagePrefix) &&
                !whitelistedMethodsOfPackage.asList().contains(calledMethodFullName) &&
                !whitelistedMethodsFromAnnotation.contains(calledMethodFullName)) {
              String message = "a non-${UIThreadUnsafeName} method ${method.getFullName()} " +
                  "calls method ${calledMethodFullName} from ${packagePrefix}";
              events.add(SimpleConditionEvent.violated(method, message));
            }
          }

          @Override
          public void check(JavaMethod method, ConditionEvents events) {
            if (method.getName().equals("$deserializeLambda$")) {
              return;
            }

            method.getCallsFromSelf().forEach(call -> {
              checkCallAgainstPackagePrefix(method, call, "git4idea", whitelistedMethodFullNames_git4idea, events);
              checkCallAgainstPackagePrefix(method, call, "java.io", whitelistedMethodFullNames_java_io, events);
              checkCallAgainstPackagePrefix(method, call, "java.nio", whitelistedMethodFullNames_java_nio, events);
            });
          }
        })
        .check(importedClasses);
  }
}
