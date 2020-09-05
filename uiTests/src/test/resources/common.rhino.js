importClass(java.lang.Thread);

// Do not run on the UI thread.
function sleep() {
  Thread.sleep(500);
}
