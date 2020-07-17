
importClass(java.lang.NoSuchMethodException);
importClass(java.lang.Thread);

function getNonPublicMethod(clazz, methodName) {
  try {
    // `getDeclaredMethod`, as opposed to `getMethod`, gives access to non-public methods...
    return clazz.getDeclaredMethod(methodName);
  } catch (e) {
    if (e.javaException instanceof NoSuchMethodException) {
      // ... but at the expense of not including superclass methods.
      return getNonPublicMethod(clazz.getSuperclass(), methodName);
    } else {
      throw e;
    }
  }
}

// Do not run on the UI thread.
function sleep() {
  Thread.sleep(500);
}
