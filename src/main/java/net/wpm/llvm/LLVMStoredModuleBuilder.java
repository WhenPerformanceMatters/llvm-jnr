package net.wpm.llvm;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.text.ParseException;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.global.LLVM;


/**
 * Read and parse the content of a text file containing LLVM IR code.
 * Creates a LLVM module and provides it via the {@link LLVMModuleBuilder#build()} method.
 * 
 * @author Nico Hezel
 *
 * @param <T>
 */
public class LLVMStoredModuleBuilder<T> implements LLVMModuleBuilder<T> {

	protected final LLVMModuleRef outModule;
	protected final Class<T> invocationInterface;
		
	public LLVMStoredModuleBuilder(Path file, Class<T> invocationInterface) throws FileNotFoundException, ParseException {
		LLVMMemoryBufferRef memory = readFile(file);
		this.outModule = parseIR(memory);
		this.invocationInterface = invocationInterface;
	}
	
	/**
	 * Read the content of the file and copy it into a memory buffer
	 * 
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	protected static LLVMMemoryBufferRef readFile(Path file) throws FileNotFoundException {
		BytePointer path = new BytePointer(file.toString());
		LLVMMemoryBufferRef memory = new LLVMMemoryBufferRef();
		BytePointer error = new BytePointer((Pointer) null);
		try {
			if (LLVM.LLVMCreateMemoryBufferWithContentsOfFile(path, memory, error) != 0) {
				throw new FileNotFoundException(error.getString());
			}
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
		return memory;
	}
	
	/**
	 * Read and parse the LLVM IR from the memory buffer to create an in-memory module object.
	 *  
	 * @param memory
	 * @return
	 * @throws ParseException
	 */
	protected static LLVMModuleRef parseIR(LLVMMemoryBufferRef memory) throws ParseException {
		LLVMContextRef context = LLVM.LLVMGetGlobalContext();
		LLVMModuleRef outModule = new LLVMModuleRef();
		BytePointer error = new BytePointer((Pointer) null);
		try {
			if (LLVM.LLVMParseIRInContext(context, memory, outModule, error) != 0) {
				throw new ParseException(error.getString(), 0);
			}
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
		return outModule;
	}
	
	@Override
	public LLVMModuleRef build() {
		return outModule;
	}
	
	@Override
	public Class<T> getInvocationInferace() {
		return this.invocationInterface;
	}
}
