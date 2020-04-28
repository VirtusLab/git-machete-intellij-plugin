package com.virtuslab.gitmachete.frontend.graph.api.print;

import io.vavr.collection.List;
import org.checkerframework.checker.index.qual.NonNegative;

import com.virtuslab.gitmachete.frontend.graph.api.print.elements.IPrintElement;

public interface IPrintElementGenerator {
  List<? extends IPrintElement> getPrintElements(@NonNegative int rowIndex);
}
