package net.wpm.llvm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.text.ParseException;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.llvm.LLVM.LLVMTargetRef;
import org.bytedeco.llvm.LLVM.LLVMTargetMachineRef;
import org.bytedeco.llvm.LLVM.LLVMBinaryRef;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMSectionIteratorRef;
import org.bytedeco.llvm.LLVM.LLVMSymbolIteratorRef;
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

	protected final Path file;
	protected final Class<T> invocationInterface;
	
	/**
	 * LLVM module stored in a LLVM file (containing IR code or bitcode) 
	 * 
	 * @param file file path to the IR
	 * @param invocationInterface a class object to a java interface
	 */
	public LLVMStoredModuleBuilder(Path file, Class<T> invocationInterface) {
		this.file = file;
		this.invocationInterface = invocationInterface;
	}
	
	public Path getLLVMFile() {
		return file;
	}
	
	/**
	 * Read the content of the file and copy it into a memory buffer
	 * 
	 * @param file path to the IR
	 * @return memory filled with the content of the file
	 * @throws FileNotFoundException path to the file is invalid
	 */
	protected static LLVMMemoryBufferRef readFile(Path file) throws FileNotFoundException {
		final BytePointer path = new BytePointer(file.toString());
		final LLVMMemoryBufferRef memory = new LLVMMemoryBufferRef();
		final BytePointer error = new BytePointer((Pointer) null);
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
	 * @param memory filled IR code, gets consumed
	 * @return LLVM module of the IR code
	 * @throws ParseException unable to parse the IR code
	 */
	protected static LLVMModuleRef parseIR(LLVMMemoryBufferRef memory) throws ParseException {
		final LLVMContextRef context = LLVM.LLVMGetGlobalContext();
		final LLVMModuleRef outModule = new LLVMModuleRef();
		final BytePointer error = new BytePointer((Pointer) null);

		try {
			// consumes memory, no need to dispose it 
			if (LLVM.LLVMParseIRInContext(context, memory, outModule, error) != 0) 
				throw new ParseException(error.getString(), 0);
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}

		return outModule;
	}
	
	protected static LLVMModuleRef parseBitcode(LLVMMemoryBufferRef memory) throws ParseException {
		final LLVMContextRef context = LLVM.LLVMGetGlobalContext();
		final LLVMModuleRef outModule = new LLVMModuleRef();
		final BytePointer error = new BytePointer((Pointer) null);
		try {
			// consumes memory, no need to dispose it 
			if (LLVM.LLVMParseBitcodeInContext2(context, memory, outModule) != 0) {
				throw new ParseException(error.getString(), 0);
			}
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
		return outModule;
	}	
	
	@Override
	public LLVMModuleRef build() {			
		try {			

			final LLVMMemoryBufferRef memory = readFile(file);
			if(file.toString().endsWith(".bc"))
				return parseBitcode(memory);
			else
				return parseIR(memory);
			
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public Class<T> getInvocationInterface() {
		return this.invocationInterface;
	}
	
	
	
	
	
	
	// --------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- Helper Functions ---------------------------------------------------
	// --------------------------------------------------------------------------------------------------------------

	/**
	 * Store the intermediate representation language of LLVM as a text file with the extension .ll
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static void storeIR(LLVMModuleRef module, Path file) throws IOException {		
		final BytePointer error = new BytePointer((Pointer) null);
		try {
			
			if (LLVM.LLVMPrintModuleToFile(module, file.toString(), error)!= 0) {
				throw new IOException(error.getString());
			}
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
		
	}

	public static void storeBinary(LLVMModuleRef module, Path file) throws UnsupportedEncodingException {
		// https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm-c/Object.h#L115
		// https://github.com/bytedeco/javacpp-presets/blob/231ec19685f18fdbddaeadeabe07b57f464af4d6/llvm/samples/llvm/EmitBitcode.java
		
		// Create the relocatable object file
		final BytePointer triple = LLVM.LLVMGetDefaultTargetTriple();
		final LLVMTargetRef target = new LLVMTargetRef();
		{
			final BytePointer error = new BytePointer((Pointer) null);
			try {
		        if (LLVM.LLVMGetTargetFromTriple(triple, target, error) != 0) 
		        	throw new RuntimeException("Failed to get target from triple: " + error.getString());
			} finally {
				LLVM.LLVMDisposeMessage(error);
			}
		}
//		System.out.println("target of module "+LLVM.LLVMGetTarget(module).getString("UTF-8"));
//		System.out.println("first target "+LLVM.LLVMGetTargetName(LLVM.LLVMGetFirstTarget()).getString("UTF-8"));
//		System.out.println(triple.getString());
//		System.out.println(LLVM.LLVMGetTargetName(target).getString());
//		System.out.println(LLVM.LLVMGetTargetDescription(target).getString());
		
		// "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87"  "tune-cpu"="generic"
		final String tuneCpu = "generic";
		final String targetFeatures = "";
		final int optimizationLevel = 3;
		final int reloc = LLVM.LLVMRelocDefault;
		final int codeModel = LLVM.LLVMCodeModelDefault;
		final LLVMTargetMachineRef tm = LLVM.LLVMCreateTargetMachine(target, triple.getString(), tuneCpu, targetFeatures, optimizationLevel, reloc, codeModel);
        
		// store object file
		{
			final BytePointer outputFile = new BytePointer(file.toString());
			final BytePointer error = new BytePointer((Pointer) null);
			final int fileType = LLVM.LLVMObjectFile;
			try {
		        if (LLVM.LLVMTargetMachineEmitToFile(tm, module, outputFile, fileType, error) != 0)
		        	throw new RuntimeException("Failed to get target from triple: " + error.getString());
			} finally {
				LLVM.LLVMDisposeMessage(error);
			}
		}
        
		LLVM.LLVMDisposeMessage(triple);
	}
	
	public static LLVMModuleRef readBitcodeFile(Path file) throws FileNotFoundException, UnsupportedEncodingException, ParseException {
		BytePointer error = new BytePointer((Pointer) null);
		try {
			final LLVMMemoryBufferRef memory = readFile(file);
			final LLVMModuleRef module = parseBitcode(memory);			
			return module;
			
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
	}
	
	public static LLVMBinaryRef readBinaryFile(Path file) throws FileNotFoundException, UnsupportedEncodingException, ParseException {
		BytePointer error = new BytePointer((Pointer) null);
		try {
			
			final LLVMMemoryBufferRef memory = readFile(file);
			final LLVMBinaryRef binaryRef = createBinary(memory);
			
			final int type = LLVM.LLVMBinaryGetType(binaryRef);
			System.out.println("binary type (5=LLVM.LLVMBinaryTypeCOFF):  "+type);

			if(type != LLVM.LLVMBinaryTypeCOFF) 
				throw new UnsupportedEncodingException("Content of file "+file+" is not an object file");
				
			// print symbols
			System.out.println("\nSymbols:");
			final LLVMSymbolIteratorRef symbolIt = LLVM.LLVMObjectFileCopySymbolIterator(binaryRef);			
			while(LLVM.LLVMObjectFileIsSymbolIteratorAtEnd(binaryRef, symbolIt) == 0) {
				final BytePointer name = LLVM.LLVMGetSymbolName(symbolIt);
				final long address = LLVM.LLVMGetSymbolAddress(symbolIt);
				final long size = LLVM.LLVMGetSymbolSize(symbolIt);
				System.out.println(name.getString("UTF-8")+" size: "+size+" address:"+address);
				LLVM.LLVMMoveToNextSymbol(symbolIt);
			}

			// print sections
			System.out.println("\nSections:");
			final LLVMSectionIteratorRef sectionIt = LLVM.LLVMObjectFileCopySectionIterator(binaryRef);			
			while(LLVM.LLVMObjectFileIsSectionIteratorAtEnd(binaryRef, sectionIt) == 0) {
				final BytePointer name = LLVM.LLVMGetSectionName(sectionIt);
				final long address = LLVM.LLVMGetSectionAddress(sectionIt);
				final long size = LLVM.LLVMGetSectionSize(sectionIt);
				final String contentStr = size == 0 ? "" : LLVM.LLVMGetSectionContents(sectionIt).getString("US-ASCII");
				System.out.println(name.getString("US-ASCII")+":"+contentStr+" size: "+size+" address:"+address);
				LLVM.LLVMMoveToNextSection(sectionIt);
			}
			System.out.println();
			
//			LLVMBinaryRef archBinaryRef = LLVM.LLVMMachOUniversalBinaryCopyObjectForArch(binaryRef, "x64", 3, error);
//			final int archType = LLVM.LLVMBinaryGetType(archBinaryRef);
//			System.out.println("archType (13=LLVM.LLVMBinaryTypeMachO64B):  "+archType);
			
			LLVM.LLVMDisposeSectionIterator(sectionIt);
			LLVM.LLVMDisposeSymbolIterator(symbolIt);
			
			return binaryRef;
		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
	}

	public static void storeBitcode(LLVMModuleRef module, Path file) throws IOException {
		// ll (text) to bc (binary)
		// https://stackoverflow.com/questions/14107743/llvm-and-compiler-nomenclature
//		final LLVMContextRef context = LLVM.LLVMGetGlobalContext();
		final BytePointer error = new BytePointer((Pointer) null);
		try {
			final String fileStr = file.toString();
			if(LLVM.LLVMWriteBitcodeToFile(module, fileStr) != 0) 
				throw new IOException("Could not create file" + fileStr);
			
			// more complex version with error handling
//			LLVM.LLVMSetSourceFileName(module, fileStr+".ll", fileStr.length()+3);
//			final LLVMMemoryBufferRef memory = LLVM.LLVMWriteBitcodeToMemoryBuffer(module);
//			final LLVMBinaryRef binaryRef = LLVM.LLVMCreateBinary(memory, context, error);
//			if(binaryRef == null)
//				throw new RuntimeException(error.getString());		
//			LLVM.LLVMDisposeMemoryBuffer(memory);

		} finally {
			LLVM.LLVMDisposeMessage(error);
		}
	}
	
	protected static LLVMBinaryRef createBinary(LLVMMemoryBufferRef memory) throws ParseException {
		final LLVMContextRef context = LLVM.LLVMGetGlobalContext();
		final BytePointer error = new BytePointer((Pointer) null);
		try {
			
			final LLVMBinaryRef binaryRef = LLVM.LLVMCreateBinary(memory, context, error);
			if(binaryRef == null)
				throw new RuntimeException(error.getString());			
			return binaryRef;
		} finally {
			LLVM.LLVMDisposeMemoryBuffer(memory);
			LLVM.LLVMDisposeMessage(error);
		}
	}
}
