package com.virtuslab.gitmachete.frontend.graph.print;

import java.util.Collection;

import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.print.elements.impl.PrintElementWithGraphElement;

public interface IPrintElementGenerator {
  Collection<PrintElementWithGraphElement> getPrintElements(@NonNegative int rowIndex);
}
