import org.junit.Test
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0
import kotlin.reflect.full.IllegalCallableAccessException

private class PrivateClass {
  val publicVal = 0
  var publicVar = 0
  fun publicFun() = 0
}

class PublicClass {
  private val privateVal = 0
  fun privateValKProperty(): KProperty0<Int> = ::privateVal

  private var privateVar = 0
  fun privateVarKProperty(): KMutableProperty0<Int> = ::privateVar
}

// The purpose of this test is to:
// * show the risk of IllegalCallableAccessException when using KProperties pointing to properties
//   that are non-public and/or defined in non-public classes,
// * notice once the underlying issue(s) KT-18408, KT-18560, KT-29016 are fixed in Kotlin runtime
// We've once encountered IllegalCallableAccessException in the released plugin,
// see https://github.com/VirtusLab/git-machete-intellij-plugin/issues/1131#issuecomment-1250355090.
// This particular failure is no longer possible once we've migrated to Kotlin UI DSL v2,
// as the problem was coming from `com.intellij.ui.layout.CellKt.createPropertyBinding`.
// We now use `com.intellij.ui.dsl.builder.ButtonKt.bindSelected` which calls `com.intellij.ui.dsl.builder.MutablePropertyKt.toMutableProperty` under the hood,
// which doesn't lead to a fatal `.call()` on the property.
// We no longer ban the use private properties and/or private classes in Kotlin,
// however we need to be aware that the risk of some of IntelliJ Kotlin libs using `.call()` still exists.
class KPropertyTestSuite {
  @Test(expected = IllegalCallableAccessException::class)
  fun calling_public_val_of_private_class_should_fail() {
    val prop: KCallable<Int> = PrivateClass()::publicVal
    prop.call()
  }

  @Test
  fun getting_public_val_of_private_class_should_succeed() {
    val prop: KProperty0<Int> = PrivateClass()::publicVal
    prop.get()
  }

  @Test
  fun invoking_public_val_of_private_class_should_succeed() {
    val prop: () -> Int = PrivateClass()::publicVal
    prop.invoke()
  }

  @Test(expected = IllegalCallableAccessException::class)
  fun calling_public_var_of_private_class_should_fail() {
    val prop: KCallable<Int> = PrivateClass()::publicVar
    prop.call()
  }

  @Test
  fun getting_public_var_of_private_class_should_succeed() {
    val prop: KMutableProperty0<Int> = PrivateClass()::publicVar
    prop.get()
  }

  @Test
  fun invoking_public_var_of_private_class_should_succeed() {
    val prop: () -> Int = PrivateClass()::publicVar
    prop.invoke()
  }

  @Test
  fun invoking_public_fun_of_private_class_should_succeed() {
    val func: () -> Int = PrivateClass()::publicFun
    func.invoke()
  }

  @Test(expected = IllegalCallableAccessException::class)
  fun calling_private_val_of_public_class_should_fail() {
    val prop: KCallable<Int> = PublicClass().privateValKProperty()
    prop.call()
  }

  @Test
  fun getting_private_val_of_public_class_should_succeed() {
    val prop: KProperty0<Int> = PublicClass().privateValKProperty()
    prop.get()
  }

  @Test
  fun invoking_private_val_of_public_class_should_succeed() {
    val prop: () -> Int = PublicClass().privateValKProperty()
    prop.invoke()
  }

  @Test(expected = IllegalCallableAccessException::class)
  fun calling_private_var_of_public_class_should_fail() {
    val prop: KCallable<Int> = PublicClass().privateVarKProperty()
    prop.call()
  }

  @Test
  fun getting_private_var_of_public_class_should_succeed() {
    val prop: KMutableProperty0<Int> = PublicClass().privateVarKProperty()
    prop.get()
  }

  @Test
  fun invoking_private_var_of_public_class_should_succeed() {
    val prop: () -> Int = PublicClass().privateVarKProperty()
    prop.invoke()
  }
}
