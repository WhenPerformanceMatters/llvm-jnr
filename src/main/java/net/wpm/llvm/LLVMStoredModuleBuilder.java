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
 * <br><br>
 * In order to get LLVM IR use a clang compiler or this website
 * <a href="http://ellcc.org/demo/index.cgi">http://ellcc.org/demo/index.cgi</a>
 * e.g. paste this c code in there
 * 
 * <pre>
 * {@code
 * void matmul(const float *a, const float *b, float *c, const int M, const int N, const int K)
 * {
 *	for (int m = 0; m < M; m++) {
 *		for (int n = 0; n < N; n++) {
 * 			float s = 0;
 * 			for (int k = 0; k < K; k++) {
 * 				s += a[m * K + k] * b[k * N + n];
 * 			}
 * 			c[m * N + n] = s;
 * 		}
 *	}
 * }
 * }
 * </pre>
 * 
 * @author Nico Hezel
 *
 * @param <T> invocation interface 
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
	 * @param file path to the IR
	 * @return memory filled with the content of the file
	 * @throws FileNotFoundException path to the file is invalid
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
	 * @param memory filled IR code
	 * @return LLVM module of the IR code
	 * @throws ParseException unable to parse the IR code
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
	public Class<T> getInvocationInterface() {
		return this.invocationInterface;
	}
}
