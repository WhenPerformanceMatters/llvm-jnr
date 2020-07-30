package net.wpm.llvm;

import org.bytedeco.llvm.LLVM.LLVMModuleRef;


/**
 * This class builds a {@link LLVMModuleRef} and provides a java interface containing the same methods than the module.
 * The {@link LLVMCompiler} compiles the module and creates a {@link LLVMProgram}. The program creates an instance of 
 * the java interface and binds its methods to the LLVM module methods. Calling a method of this java object calls in 
 * return a LLVM function.
 * 
 * @author Nico Hezel
 *
 * @param <T> Invocation interface 
 */
public interface LLVMModuleBuilder<T> {

	/**
	 * Build the LLVMModule
	 * 
	 * @return a LLVM module
	 */
	public LLVMModuleRef build();

	/**
	 * An interface with identical method names and signatures than the LLVM module.
	 * Every method in the interface must exist in the LLVM module and no static or
	 * overloaded methods are allowed.
	 * 
	 * @return a class object to a java interface
	 */
	public Class<T> getInvocationInterface();

}
