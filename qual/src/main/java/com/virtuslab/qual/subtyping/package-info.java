/**
 * Java 17 allows for pattern matching in switch expressions & statements
 * (https://www.baeldung.com/java-switch-pattern-matching).
 * Still, a quick evaluation shows that Subtyping Checker is more convenient.
 * Let's consider the example of {@code com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot},
 * with its two sub-interfaces {@code IRootManagedBranchSnapshot} and {@code INonRootManagedBranchSnapshot}.
 * <p>
 * With Subtyping Checker, the following is possible:
 * <pre>
 *   if (branch.isRoot()) {
 *     ... branch.asRoot() ...
 *   } else {
 *     ... branch.asNonRoot() ...
 *   }
 * </pre>
 * <p>
 * With {@code instanceof}, it would look like:
 * <pre>
 *   if (branch instanceof IRootManagedBranchSnapshot rootBranch) {
 *     ... rootBranch ...
 *   } else {
 *     ... ((INonRootManagedBranchSnapshot) branch) ...
 *   }
 * </pre>
 * or alternatively
 * <pre>
 *   if (branch instanceof IRootManagedBranchSnapshot rootBranch) {
 *     ... rootBranch ...
 *   } else if (branch instanceof INonRootManagedBranchSnapshot nonRootBranch) {
 *     ... nonRootBranch ..
 *   }
 * </pre>
 * <p>
 * Pattern matching in switch (Java 17 with {@code --enable-preview})
 * won't work for interfaces that are directly extended by an abstract class
 * (and not only by sub-interfaces), as is the case with {@code IManagedBranchSnapshot}.
 * The existence of {@code com.virtuslab.gitmachete.backend.impl.BaseManagedBranchSnapshot} would massively mess up the checks
 * for sealed interface, which are needed for pattern matching to work properly.
 * We can either sacrifice sealedness of {@code IManagedBranchSnapshot}:
 * <pre>
 *   switch (branch) {
 *     case IRootManagedBranchSnapshot rootBranch -> ...
 *     case INonRootManagedBranchSnapshot nonRootBranch -> ...
 *     // WHOOPS compiler sees this match as non-exhaustive
 *   }
 * </pre>
 * or include {@code BaseManagedBranchSnapshot} in {@code permits} clause for {@code IManagedBranchSnapshot},
 * which would also cause the compiler to report a non-exhaustive {@code switch} error.
 * <p>
 * All things consider, as of Java 17, Subtyping Checker remains a cleaner choice for exhaustive matching on subtypes
 * than whatever mechanism built into the language.
 */
package com.virtuslab.qual.subtyping;
