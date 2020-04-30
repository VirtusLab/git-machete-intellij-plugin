package com.virtuslab.qual.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import com.virtuslab.qual.gitmachete.backend.api.ConfirmedNonRootBranch;
import com.virtuslab.qual.gitmachete.backend.api.ConfirmedRootBranch;
import com.virtuslab.qual.gitmachete.frontend.graph.api.elements.ConfirmedGraphEdge;
import com.virtuslab.qual.gitmachete.frontend.graph.api.elements.ConfirmedGraphNode;
import com.virtuslab.qual.gitmachete.frontend.graph.api.items.ConfirmedBranchItem;
import com.virtuslab.qual.gitmachete.frontend.graph.api.items.ConfirmedCommitItem;

// There needs to be single subtyping hierarchy with single bottom and top annotation,
// otherwise Subtyping Checker would raise an error about multiple top/bottom types.
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@SubtypeOf({
    // Despite having a unified type hierarchy, we're actually doing 3 completely independent checks here.
    ConfirmedRootBranch.class,
    ConfirmedNonRootBranch.class,
    ConfirmedGraphNode.class,
    ConfirmedGraphEdge.class,
    ConfirmedBranchItem.class,
    ConfirmedCommitItem.class,
})
public @interface SubtypingBottom {}
