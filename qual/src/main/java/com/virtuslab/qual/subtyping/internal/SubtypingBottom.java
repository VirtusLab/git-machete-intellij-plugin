package com.virtuslab.qual.subtyping.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import com.virtuslab.qual.subtyping.gitmachete.backend.api.ConfirmedLocal;
import com.virtuslab.qual.subtyping.gitmachete.backend.api.ConfirmedNonRoot;
import com.virtuslab.qual.subtyping.gitmachete.backend.api.ConfirmedRemote;
import com.virtuslab.qual.subtyping.gitmachete.backend.api.ConfirmedRoot;
import com.virtuslab.qual.subtyping.gitmachete.frontend.graph.api.elements.ConfirmedGraphEdge;
import com.virtuslab.qual.subtyping.gitmachete.frontend.graph.api.elements.ConfirmedGraphNode;
import com.virtuslab.qual.subtyping.gitmachete.frontend.graph.api.items.ConfirmedBranchItem;
import com.virtuslab.qual.subtyping.gitmachete.frontend.graph.api.items.ConfirmedCommitItem;

/**
 * There needs to be single subtyping hierarchy with single bottom and top annotation.
 * We could theoretically create a separate hierarchy with a dedicated top and bottom type
 * for each pair of annotations from {@link com.virtuslab.qual.subtyping}.* packages,
 * but then <a href="https://checkerframework.org/manual/#subtyping-checker">Subtyping Checker</a>
 * would raise an error about multiple top/bottom types.
 */
@Retention(RetentionPolicy.CLASS)
@SubtypeOf({
    // Despite having a unified type hierarchy, we're actually doing 4 completely independent checks here.
    ConfirmedLocal.class,
    ConfirmedRemote.class,

    ConfirmedRoot.class,
    ConfirmedNonRoot.class,

    ConfirmedGraphNode.class,
    ConfirmedGraphEdge.class,

    ConfirmedBranchItem.class,
    ConfirmedCommitItem.class,
})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface SubtypingBottom {}
