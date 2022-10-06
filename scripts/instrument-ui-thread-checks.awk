#!/usr/bin/env awk

FNR == 1 {
  expecting_ui_thread_method = 0
  expecting_non_ui_thread_method = 0
}

/@UIEffect/       { expecting_ui_thread_method = 1 }

/@UIThreadUnsafe/ { expecting_non_ui_thread_method = 1 }

{ print }

/protected BaseEnhancedGraphTable.AbstractTableModel model./ || \
/protected BaseGraphTable.TableModel model./ || \
/CreateGitMacheteRepositoryAux\(/ || \
/DiscoverGitMacheteRepositoryAux\(/ || \
/public EnhancedGraphTable.Project project./ || \
/public GitMachetePanel.Project project./ || \
/public GitPushDialog\(/ || \
/public GitRepositoryComboBox.Project project./ || \
/OptionsPanel\(\)/ || \
/MacheteCodeStyleMainPanel.CodeStyleSettings currentSettings, CodeStyleSettings settings./ || \
/MyVcsPushDialog\(/ || \
/PushSwingAction\(\)/ || \
/private SimpleGraphTable.GraphTableModel graphTableModel, boolean shouldDisplayActionToolTips./ {
  expecting_ui_thread_method = 0
}

/;$/ {
  expecting_ui_thread_method = 0
  expecting_non_ui_thread_method = 0
}

/\{$/ {
  if (expecting_ui_thread_method) {
    print "    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {"
    print "      var sw = new java.io.StringWriter();"
    print "      var pw = new java.io.PrintWriter(sw);"
    print "      new Exception().printStackTrace(pw);"
    print "      String stackTrace = sw.toString();"
    print "      System.out.println(\"Expected EDT:\");"
    print "      System.out.println(stackTrace);"
    print "      throw new RuntimeException(\"Expected EDT: \" + stackTrace);"
    print "    }"
    expecting_ui_thread_method = 0
  } else if (expecting_non_ui_thread_method) {
    print "    if (javax.swing.SwingUtilities.isEventDispatchThread()) {"
    print "      var sw = new java.io.StringWriter();"
    print "      var pw = new java.io.PrintWriter(sw);"
    print "      new Exception().printStackTrace(pw);"
    print "      String stackTrace = sw.toString();"
    print "      if (!stackTrace.contains(\"at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed\")) {"
    print "        System.out.println(\"Expected non-EDT:\");"
    print "        System.out.println(stackTrace);"
    print "        throw new RuntimeException(\"Expected EDT: \" + stackTrace);"
    print "      }"
    print "    }"
    expecting_non_ui_thread_method = 0
  }
}
