package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

public class ForbiddenFieldsTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void no_classes_should_access_RevSort_TOPO() {
    noClasses()
        .should()
        .accessField(org.eclipse.jgit.revwalk.RevSort.class, "TOPO")
        .orShould()
        .accessField(org.eclipse.jgit.revwalk.RevSort.class, "TOPO_KEEP_BRANCH_TOGETHER")
        .because("as of JGit 6.4.0, these flags lead to performance problems on large repos (100,000s of commits); " +
            "use RevSort#COMMIT_TIME_DESC instead")
        .check(productionClasses);
  }

}
