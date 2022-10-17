package net.wpm.llvm;


import java.lang.instrument.IllegalClassFormatException;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.global.LLVM;


/**
 * Compiles a LLVM module for the host cpu using polly for code optimizations.
 * https://polly.llvm.org/
 * 
 * @author Nico Hezel
 */
public class LLVMCompiler {

	protected final BytePointer device;	

	/**
	 * Setup the compiler and decide if polly should be used for loop optimizations.
	 * 
	 * @param usePolly use polly optimization or not
	 * @param usePollyParallel use polly parallel optimization or not
	 */
	public LLVMCompiler(boolean usePolly, boolean usePollyParallel) {
		initialize(usePolly, usePollyParallel);
		device = LLVM.LLVMGetHostCPUName();
	}

	
	/**
	 * Build the LLVM module from the moduleBuilder and optimize its code.
	 * Compile all functions in the module and make those accessible which
	 * are defined in the invocation interface see {@link LLVMModuleBuilder#getInvocationInterface()}.
	 * 
	 * This methods returns an {@link LLVMProgram} containing the LLVM module and java code to call 
	 * native functions inside of this module. The program must be disposed when no longer used. 
	 *  
	 * @param <T> invocation interface 
	 * @param moduleBuilder module builder 
	 * @return the {@link LLVMProgram} provides access to the LLVM functions and should be disposed when no longer needed.
	 * @throws IllegalClassFormatException if the invocation interface has invalid statements like overloaded methods
	 * @throws NoSuchMethodException if the LLVM code does not contain all the functions as in the invocation interface
	 */
	public <T> LLVMProgram<T> compile(LLVMModuleBuilder<T> moduleBuilder) throws NoSuchMethodException, IllegalClassFormatException {
		return compile(moduleBuilder.build(), moduleBuilder.getInvocationInterface(), false);
	}
	
	/**
	 * Build the LLVM module from the moduleBuilder and optimize its code.
	 * Compile all functions in the module and make those accessible which
	 * are defined in the invocation interface see {@link LLVMModuleBuilder#getInvocationInterface()}.
	 * 
	 * This methods returns an {@link LLVMProgram} containing the LLVM module and java code to call 
	 * native functions inside of this module. The program must be disposed when no longer used. 
	 *  
	 * @param <T> invocation interface 
	 * @param moduleBuilder module builder
	 * @param isOptimized is this module already optimized
	 * @return the {@link LLVMProgram} provides access to the LLVM functions and should be disposed when no longer needed.
	 * @throws IllegalClassFormatException if the invocation interface has invalid statements like overloaded methods
	 * @throws NoSuchMethodException if the LLVM code does not contain all the functions as in the invocation interface
	 */
	public <T> LLVMProgram<T> compile(LLVMModuleBuilder<T> moduleBuilder, boolean isOptimized) throws NoSuchMethodException, IllegalClassFormatException {
		return compile(moduleBuilder.build(), moduleBuilder.getInvocationInterface(), isOptimized);
	}
		
	/**
	 * Build the LLVM module from the module and optimize its code.
	 * Compile all functions in the module and make those accessible which
	 * are defined in the invocation interface see {@link LLVMModuleBuilder#getInvocationInterface()}.
	 * 
	 * This methods returns an {@link LLVMProgram} containing the LLVM module and java code to call 
	 * native functions inside of this module. The program must be disposed when no longer used. 
	 * 
	 * @param <T> invocation interface 
	 * @param module containing llvm assembly
	 * @param invocationInterface class
	 * @param isOptimized is this module already optimized
	 * @return the {@link LLVMProgram} provides access to the LLVM functions and should be disposed when no longer needed.
	 * @throws IllegalClassFormatException if the invocation interface has invalid statements like overloaded methods
	 * @throws NoSuchMethodException if the LLVM code does not contain all the functions as in the invocation interface
	 */
	public <T> LLVMProgram<T> compile(LLVMModuleRef module, Class<T> invocationInterface, boolean isOptimized) throws NoSuchMethodException, IllegalClassFormatException {
		
		// verify the LLVM code
		verifyModule(module);

		// create an execution engine to run the module
		final LLVMModuleRef optModule = LLVM.LLVMCloneModule(module);
		final LLVMExecutionEngineRef engine = createExecutionEngine(optModule);
		if(isOptimized == false) {
			optimizeModule(optModule, device);
			jitCompileModule(engine, optModule, device);
		}

		return new LLVMProgram<>(engine, optModule, invocationInterface);
	}

	public static LLVMExecutionEngineRef createExecutionEngine(LLVMModuleRef module) {
		LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();
		BytePointer error = new BytePointer((Pointer) null);
		try {
			// https://lowlevelbits.org/how-to-use-llvm-api-with-swift.-addendum/
			// https://wiki.aalto.fi/display/t1065450/LLVM+Execution+and+Interpretation+%3A+LLVM+Execution+Engine%2C+Just-In-Time+Compiler+and+Interpreter
//			if (LLVM.LLVMCreateInterpreterForModule(engine, module, error) != 0) 		// needs jitCompileModule
//			if (LLVM.LLVMCreateJITCompilerForModule(engine, module, 3, error) != 0) 
			if (LLVM.LLVMCreateExecutionEngineForModule(engine, module, error) != 0)	// general engine 
				throw new RuntimeException(error.getString());
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
		return engine;
	}

	protected static void verifyModule(LLVMModuleRef module) {
		BytePointer error = new BytePointer((Pointer) null);
		try {
			if (LLVM.LLVMVerifyModule(module, LLVM.LLVMPrintMessageAction, error) != 0) {
				throw new RuntimeException(error.getString());
			}
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
	}

	public static void optimizeModule(LLVMModuleRef module, BytePointer device) {
		LLVM.optimizeModule(module, device, 3, 0);
	}

	public static void jitCompileModule(LLVMExecutionEngineRef engine, LLVMModuleRef module, BytePointer device) {
		LLVM.createOptimizedJITCompilerForModule(engine, module, device, 3);
	}

	/**
	 * Initialize compiler chain
	 * 
	 * @param usePolly
	 * @param usePollyParallel
	 */
	protected static void initialize(boolean usePolly, boolean usePollyParallel) {
		if (usePolly) {
			if (usePollyParallel) {
				String platform = Loader.getPlatform();
				String omplib = platform.startsWith("linux") ? "libiomp5.so"
						: platform.startsWith("macosx") ? "libiomp5.dylib"
								: platform.startsWith("windows") ? "libiomp5md.dll"
										: null;
				if (omplib != null) {
					int error = LLVM.LLVMLoadLibraryPermanently(omplib);
					if(error == 1)

						System.out.println("openmp error"+ error);
				}
				setLLVMCommandLineOptions("",
						"-mllvm", "-polly",
						"-mllvm", "-polly-parallel",
						"-mllvm", "-polly-vectorizer=stripmine");
			} else {
				// https://polly.llvm.org/docs/Architecture.html
				setLLVMCommandLineOptions("",
						"-mllvm", "-polly",
						"-mllvm", "-polly-vectorizer=stripmine");
			}
		}
		
		// TODO maybe a pass manager is better
		// Create a pass pipeline using the legacy pass manager
		// https://github.com/bytedeco/javacpp-presets/blob/master/llvm/samples/llvm/Factorial.java#L116
		// New one might be better
		// https://llvm.org/docs/NewPassManager.html#just-tell-me-how-to-run-the-default-optimization-pipeline-with-the-new-pass-manager
		// e.g. https://github.com/crystal-lang/crystal/pull/12116

		// TODO Execute using OrcJIT
		// https://github.com/bytedeco/javacpp-presets/blob/master/llvm/samples/llvm/OrcJit.java
		
		
		// Initialize the LLVM libraries and MCJIT back-end 
		// https://www.doof.me.uk/2017/05/11/using-orc-with-llvms-c-api/
		LLVM.LLVMLinkInMCJIT();
//		LLVM.LLVMLinkInInterpreter();

		// The main program should call this function to initialize the printer for the native target corresponding to the host.
		LLVM.LLVMInitializeNativeAsmPrinter();

		// The main program should call this function to initialize the parser for the native target corresponding to the host.
		LLVM.LLVMInitializeNativeAsmParser();

		// The main program should call this function to initialize the disassembler for the native target corresponding to the host.
		LLVM.LLVMInitializeNativeDisassembler();

		// The main program should call this function to initialize the native target corresponding to the host.
		LLVM.LLVMInitializeNativeTarget();
	}

	protected static void setLLVMCommandLineOptions(String... args) {
		LLVM.LLVMParseCommandLineOptions(args.length, new PointerPointer<>(args), null);
	}
}
