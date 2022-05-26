package net.wpm.llvm;


import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.provider.jffi.LLVMModuleLoader;

/**
 * The class takes a LLVM engine containing compiled LLVM IR code and an invocation interface 
 * with Java methods which have the same names and signatures than the functions in the IR code.
 * 
 * The LLVMProgram creates an implementation of the interface in which calls to the Java methods
 * invoke the corresponding native functions from the LLVM machine code.
 * 
 * If the LLVM program is no longer needed is should be disposed to free the reserved memory of
 * the LLVM program.
 * 
 * @author Nico Hezel
 *
 * @param <T> invocation interface 
 */
public class LLVMProgram<T> implements AutoCloseable {

	protected final LLVMExecutionEngineRef engine;
	protected final LLVMModuleRef module;
	protected final T invocationInterface;
	protected final Map<String, Long> funcNameToAddress;

	/**
	 * A wrapper around the machine code in LLVM execution engine. The interface provides function names and signature 
	 * to the symbols in the engine.
	 * 
	 * @param engine LLVM execution engine containing compiled LLVM machine code
	 * @param invocationInterface invocation interface with method names and signature identical to the functions in the engine
	 * @throws IllegalClassFormatException if the invocation interface has invalid statements like overloaded methods
	 * @throws NoSuchMethodException if the LLVM code does not contain all the functions as in the invocation interface
	 */
	public LLVMProgram(LLVMExecutionEngineRef engine, LLVMModuleRef module, Class<T> invocationInterface) throws NoSuchMethodException, IllegalClassFormatException {
		this.engine = engine;
		this.module = module;

		// check if all methods in the invocation class exist in the LLVM engine
		Collection<String> funcNames = verifyInvocationInterface(engine, invocationInterface);

		// setup a JNR invocation interface
		funcNameToAddress = new HashMap<>();
		for (String funcName : funcNames) {
			long fnAddr = LLVM.LLVMGetFunctionAddress(engine, funcName);
			funcNameToAddress.put(funcName, fnAddr);
		}
		
		// TODO might be possible with LLVMGetNamedFunction and LLVMRunFunction without JNR
		// https://github.com/bytedeco/javacpp-presets/blob/231ec19685f18fdbddaeadeabe07b57f464af4d6/llvm/samples/llvm/EmitBitcode.java#L190
		
		LibraryLoader<T> libraryLoader = new LLVMModuleLoader<T>(invocationInterface, funcNameToAddress);
		this.invocationInterface = libraryLoader.load("llvm");
	}

	/**
	 * Find the symbol in the LLVM machine code and return the native address to this function
	 * 
	 * @param funcName name of a symbol in the LLVM machine code
	 * @return address to the symbol
	 */
	public long getAddress(String funcName) {
		return this.funcNameToAddress.get(funcName);
	}
	
	public LLVMModuleRef getOptimizedModule() {
		return module;
	}

	/**
	 * Implementation of the invocation interface, every call to a method in this class 
	 * will invoke a function with the same name and signature in the native space.
	 * 
	 * @return implementation of the invocation interface 
	 */
	public T invoke() {
		return invocationInterface;
	}

	/**
	 * Dispose the machine code in the LLVM execution engine. The {@link LLVMProgram#invoke()} method will not work anymore afterwards.
	 */
	public void dispose() {
		if(engine != null) {

			// Disposes all modules as well. No need to call LLVMDisposeModule()
			// https://stackoverflow.com/questions/27103943/llvm-api-correct-way-to-create-dispose#comment42836250_27169381
			LLVM.LLVMDisposeExecutionEngine(engine);
		}
	}

	@Override
	public void close() {
		dispose();
	}


	/**
	 * Check if the LLVM module has the same function names and functions signatures than the invocation class.
	 * 
	 * @param <T> invocation interface 
	 * @param engine LLVM execution engine containing compiled LLVM machine code
	 * @param invocationInterface invocation interface with method names and signature identical to the functions in the engine
	 * @return a collection of all valid functions in the native space
	 * @throws IllegalClassFormatException if the invocation interface has invalid statements like overloaded methods
	 * @throws NoSuchMethodException if the LLVM code does not contain all the functions as in the invocation interface
	 */
	protected static <T> Collection<String> verifyInvocationInterface(LLVMExecutionEngineRef engine, Class<T> invocationInterface) throws IllegalClassFormatException, NoSuchMethodException {

		// accept only interfaces
		if(Modifier.isInterface(invocationInterface.getModifiers()) == false)
			throw new IllegalClassFormatException(invocationInterface.getCanonicalName()+" is not an interface.");

		// scan all methods of the interface
		final Set<String> funcNames = new HashSet<>();
		for (Method method : invocationInterface.getMethods()) {
			final String funcName = method.getName();

			// no method overloading is allowed in the invocation class
			if(funcNames.contains(funcName))
				throw new IllegalClassFormatException("Method overloading is allowed in LLVM invocation class got "+invocationInterface.getCanonicalName()+"#"+funcName+" at least twice.");
			funcNames.add(funcName);
			
			// every method in the invocation class must exist in the module
			final LLVMValueRef func = new LLVMValueRef();
			final int error = LLVM.LLVMFindFunction(engine, new BytePointer(funcName), func);
			if(error == 1)
				throw new NoSuchMethodException("Every method in the LLVm invocation class must be in the LLVM IR. Missing "+funcName);

			// the signature of the method must be the same
			LLVMTypeRef funcType = LLVM.LLVMTypeOf(func);

			// get the inside of a possible pointer
			int k = LLVM.LLVMGetTypeKind(funcType);
			while(k == LLVM.LLVMPointerTypeKind) {
				funcType = LLVM.LLVMGetElementType(funcType);
				k = LLVM.LLVMGetTypeKind(funcType);
			}

			// check the type, should be a function
			if(k != LLVM.LLVMFunctionTypeKind)
				throw new IllegalArgumentException("Expected a function in LLVM IR under the name "+funcName+" but got a "+getTypekindName(k));

			// get java input parameters
			final Parameter[] parameters = method.getParameters();

			// get the type count of the input parameters from the LLVM IR function
			final int parameterCount = LLVM.LLVMCountParamTypes(funcType);
			if(parameterCount != parameters.length)
				throw new IllegalArgumentException("Expected the LLVM IR function "+funcName+" to have "+parameters.length+" input parameters, but got "+parameterCount);

			// get the types of the input parameters from the LLVM IR function
			final PointerPointer<LLVMTypeRef> ptr = new PointerPointer<>(new LLVMTypeRef[parameterCount]);
			LLVM.LLVMGetParamTypes(funcType, ptr);				
			for (int i = 0; i < parameterCount; i++) {
				LLVMTypeRef llvmParam = new LLVMTypeRef(ptr.get(i));
				Class<?> javaType = parameters[i].getType();
				if(checkLLVMTypeCompatibility(llvmParam, javaType) == false)
					throw new IllegalArgumentException("Expected the "+i+". input parameter of the LLVM IR function "+funcName+" to be "+javaType+" but got "+getTypekindName(LLVM.LLVMGetTypeKind(llvmParam)));
			}
			
			// check the return type
			final LLVMTypeRef returnType = LLVM.LLVMGetReturnType(funcType);
			if(checkLLVMTypeCompatibility(returnType, method.getReturnType()) == false)
				throw new IllegalArgumentException("Expected the LLVM IR function "+funcName+" to have the return type "+method.getReturnType());

		}

		// return a list of valid function names that exists in the module and the invocation class
		return funcNames;
	}

	/**
	 * Return string name for a LLVMTypeKind. Useful for debugging.
	 * https://github.com/anholt/mesa/blob/master/src/gallium/auxiliary/gallivm/lp_bld_type.c#L287
	 *
	 * @param typeKind LLVM type kind
	 * @return string representation of the LVM type kind
	 */
	protected static String getTypekindName(int typeKind)
	{
		switch (typeKind) {
		case LLVM.LLVMVoidTypeKind:
			return "LLVMVoidTypeKind";
		case LLVM.LLVMHalfTypeKind:
			return "LLVMHalfTypeKind";
		case LLVM.LLVMFloatTypeKind:
			return "LLVMFloatTypeKind";
		case LLVM.LLVMDoubleTypeKind:
			return "LLVMDoubleTypeKind";
		case LLVM.LLVMX86_FP80TypeKind:
			return "LLVMX86_FP80TypeKind";
		case LLVM.LLVMFP128TypeKind:
			return "LLVMFP128TypeKind";
		case LLVM.LLVMPPC_FP128TypeKind:
			return "LLVMPPC_FP128TypeKind";
		case LLVM.LLVMLabelTypeKind:
			return "LLVMLabelTypeKind";
		case LLVM.LLVMIntegerTypeKind:
			return "LLVMIntegerTypeKind";
		case LLVM.LLVMFunctionTypeKind:
			return "LLVMFunctionTypeKind";
		case LLVM.LLVMStructTypeKind:
			return "LLVMStructTypeKind";
		case LLVM.LLVMArrayTypeKind:
			return "LLVMArrayTypeKind";
		case LLVM.LLVMPointerTypeKind:
			return "LLVMPointerTypeKind";
		case LLVM.LLVMVectorTypeKind:
			return "LLVMVectorTypeKind";
		case LLVM.LLVMMetadataTypeKind:
			return "LLVMMetadataTypeKind";
		default:
			return "unknown LLVMTypeKind";
		}
	}

	/**
	 * Check if the LLVM type is compatible with the java type
	 * 
	 * @param llvmType LLVM type
	 * @param javaType Java type
	 * @return are both types compatible
	 */
	protected static boolean checkLLVMTypeCompatibility(LLVMTypeRef llvmType, Class<?> javaType) 
	{
		int typeKind = LLVM.LLVMGetTypeKind(llvmType);

		// check for LLVMVoidTypeKind, LLVMFloatTypeKind, LLVMDoubleTypeKind, LLVMIntegerTypeKind
		if(typeKind == LLVM.LLVMVoidTypeKind && javaType == void.class)
			return true;
		else if(typeKind == LLVM.LLVMFloatTypeKind && javaType == float.class)
			return true;
		else if(typeKind == LLVM.LLVMDoubleTypeKind && javaType == double.class)
			return true;
		else if(typeKind == LLVM.LLVMIntegerTypeKind && (javaType == int.class || javaType == short.class || javaType == byte.class || javaType == boolean.class)) {
			int b = LLVM.LLVMGetIntTypeWidth(llvmType);
			if(b == 32 && javaType == int.class)
				return true;
			else if(b == 16 && javaType == short.class)
				return true;
			else if(b == 8 && javaType == byte.class)
				return true;
			else if(b == 1 && javaType == boolean.class)
				return true;
			return false;
		} else if(typeKind == LLVM.LLVMArrayTypeKind && javaType.isArray()) {
			return checkLLVMTypeCompatibility(LLVM.LLVMGetElementType(llvmType), javaType.getComponentType());
		} else if(typeKind == LLVM.LLVMArrayTypeKind && javaType == Pointer.class) {
			return true;
		} else if(typeKind == LLVM.LLVMPointerTypeKind && javaType.isArray()) {
			return checkLLVMTypeCompatibility(LLVM.LLVMGetElementType(llvmType), javaType.getComponentType());
		} else if(typeKind == LLVM.LLVMPointerTypeKind && javaType == Pointer.class) {
			return true;
		}

		return false;
	}
}
