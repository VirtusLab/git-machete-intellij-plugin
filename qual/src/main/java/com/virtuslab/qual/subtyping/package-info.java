/**
 * Java 17 allows for pattern matching in switch expressions & statements
 * (https://www.baeldung.com/java-switch-pattern-matching).
 * Still, a quick evaluation shows that Subtyping Checker is more convenient:
 * <ol>
 * <li>{@code instanceof} requires introducing a new variable</li>
 * <li>{@code instanceof} only detects one subtype, not the other</li>
 * <li>{@code instanceof} requires sealed classes/interfaces for exclusivity check to work; this is problematic for interfaces like I... coz Base... </li>
 * </ol>
 */
package com.virtuslab.qual.subtyping;
