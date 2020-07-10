
importClass(java.lang.NoSuchMethodException);
importClass(java.lang.Thread);

function getMethod(clazz, methodName) {
  try {
    return clazz.getDeclaredMethod(methodName);
  } catch (e) {
    if (e.javaException instanceof NoSuchMethodException) {
      return getMethod(clazz.getSuperclass(), methodName);
    } else {
      throw e;
    }
  }
}

// Do not run in EDT.
function sleep() {
  Thread.sleep(500);
}
