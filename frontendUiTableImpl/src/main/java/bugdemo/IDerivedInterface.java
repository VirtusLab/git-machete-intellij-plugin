package bugdemo;

interface IDerivedInterface extends IBaseInterface { // this could as well be class, same effect can be observed
  // void run(); // will unsuppress the (expected) compilation error in ConcreteClass if uncommented
}
