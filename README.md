# LLVM-JNR library

A small library to compile [LLVM IR](https://llvm.org/docs/LangRef.html#instruction-reference) in Java and invoke any resulting native function with the [Java Native Runtime](https://github.com/jnr/jnr-ffi).  It is possible to create LLVM IR with the LLVM C-API (Java wrappers are provided [here](https://github.com/bytedeco/javacpp-presets/tree/master/llvm)) or to compile C/C++ code to LLVM IR with Clang. It is not necessary to install clang since some [online compiler](http://ellcc.org/demo/index.cgi) are more than sufficient for this task.

## Compiling a file with LLVM IR Code

Reading and compiling LLVM IR code from a file takes three lines of code. We first setup a [LLVMStoredModuleBuilder](https://github.com/WhenPerformanceMatters/llvm-jnr/blob/master/src/main/java/net/wpm/llvm/LLVMStoredModuleBuilder.java) which can load and parse the file. Afterwards an instance of the [LLVMCompiler](https://github.com/WhenPerformanceMatters/llvm-jnr/blob/master/src/main/java/net/wpm/llvm/LLVMCompiler.java) is created. It retrieves the parsed LLVM module and compiles it to an [LLVMProgram](https://github.com/WhenPerformanceMatters/llvm-jnr/blob/master/src/main/java/net/wpm/llvm/LLVMProgram.java).

```java
LLVMStoredModuleBuilder<MatMulInterface> moduleBuilder = new LLVMStoredModuleBuilder<>(file, MatMulInterface.class);
LLVMCompiler compiler = new LLVMCompiler(true, false);
LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder)
```

## Lifecycle of the LLVMProgram

The [LLVMProgram](https://github.com/WhenPerformanceMatters/llvm-jnr/blob/master/src/main/java/net/wpm/llvm/LLVMProgram.java) is a wrapper for the underlying [LLVMExecutionEngine](https://llvm.org/doxygen/group__LLVMCExecutionEngine.html) containing all the machine code. If those functions are no longer needed the program should be disposed. The program class also implements [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) so we can conveniently use a [try-with-resource](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) statement.

```java
try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder)) {
   ...
}
```

## Invoking generated native functions

The native function inside the LLVM engine are invoked with the help of [JNR](https://github.com/jnr/jnr-ffi). It creates an implementation of aany Java interface and maps all the methods to native functions. The [MatMulInterface](https://github.com/WhenPerformanceMatters/llvm-jnr/blob/master/src/test/java/net/wpm/llvm/LLVMStoredModuleBuilderTest.java#L65) from above might look like this:

```java
public static interface MatMulInterface {
   public void matmul(float[] a, float[] b, float[] c, int M, int N, int K);
}
```

The [LLVMProgram](https://github.com/WhenPerformanceMatters/llvm-jnr/blob/master/src/main/java/net/wpm/llvm/LLVMProgram.java) takes the interface and the LLVMExecutionEngine with all the machine code in it and searches for every method in the interface a corresponding native function which have the same name and signature. No method overloading is allowed in the interface and all methods must be present in the LLVMExecutionEngine otherwise an IllegalClassFormatException or NoSuchMethodException is thrown. 

Calling invoke() on the [LLVMProgram](https://github.com/WhenPerformanceMatters/llvm-jnr/blob/master/src/main/java/net/wpm/llvm/LLVMProgram.java) returns the implementation of the interface and all the methods are now calling native functions.

```java
LLVMStoredModuleBuilder<MatMulInterface> moduleBuilder = new LLVMStoredModuleBuilder<>(file, MatMulInterface.class);
LLVMCompiler compiler = new LLVMCompiler(true, false);
try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder)) {
   program.invoke().matmul(a, b, c, M, N, K);
}
```


