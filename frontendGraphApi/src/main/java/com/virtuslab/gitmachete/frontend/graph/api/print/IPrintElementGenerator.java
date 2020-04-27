package com.virtuslab.gitmachete.frontend.graph.api.print;

import java.util.Collection;

import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;

public interface IPrintElementGenerator {
  Collection<? extends IPrintElement> getPrintElements(@NonNegative int rowIndex);
}
