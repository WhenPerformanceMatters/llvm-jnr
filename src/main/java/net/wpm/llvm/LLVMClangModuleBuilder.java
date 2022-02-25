package net.wpm.llvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import org.bytedeco.javacpp.Loader;


/**
 * Compile C code to a LLVM module and provides it via the {@link LLVMModuleBuilder#build()} method.  
 * 
 * @author Nico Hezel
 *
 * @param <T> invocation interface 
 */
public class LLVMClangModuleBuilder<T> extends LLVMStoredModuleBuilder<T> {

	protected static String clang;
	
	/**
	 * Compile a c file to a LLVM module
	 * 
	 * @param cFile containing c code
	 * @param invocationInterface invocation interface 
	 * @param clangParams additional commands for clang
	 * @throws ParseException unable to parse the IR code
	 * @throws InterruptedException clang compiler error 
	 * @throws IOException could not store the c file 
	 */
	public LLVMClangModuleBuilder(Path cFile, Class<T> invocationInterface, String ... clangParams) throws IOException, InterruptedException, ParseException {
		super(transpile(cFile, clangParams), invocationInterface);
	}
	
	/**
	 * Store and compile the c code to a LLVM module
	 * 
	 * @param cCode string with c code
	 * @param invocationInterface invocation interface 
	 * @param clangParams additional commands for clang
	 * @throws ParseException unable to parse the IR code
	 * @throws InterruptedException clang compiler error 
	 * @throws IOException could not store the c file
	 */
	public LLVMClangModuleBuilder(String cCode, Class<T> invocationInterface, String ... clangParams) throws IOException, InterruptedException, ParseException {
		super(transpile(cCode, clangParams), invocationInterface);
	}

	/**
	 * Store the c code to a file and compile it into a LLVM module
	 * 
	 * @param cCode string with c code
	 * @param clangParams additional commands for clang
	 * @return path to the LLVM IR file
	 * @throws IOException could not store the c file
	 * @throws InterruptedException clang compiler error 
	 */
	protected static Path transpile(String cCode, String ... clangParams) throws IOException, InterruptedException {
		
		// store c code to file
		final Path tmpDir = Files.createTempDirectory("llvm_jnr");
		final Path cFile = Files.createTempFile(tmpDir, "", ".c");
		System.out.println("Write c file to "+ cFile);
		Files.write(cFile, cCode.getBytes());
		
		// where should the output file be stored
		String filename = cFile.getFileName().toString();
		filename = filename.substring(0, filename.lastIndexOf('.')) + ".ll";
		String outputPath = tmpDir.resolve(filename).toString();
		
		// compile the code
		runClang(cFile.toString(), outputPath, clangParams);

		return Paths.get(outputPath);
	}
	
	/**
	 * Compile a c file to a LLVM module
	 * 
	 * @param cFile path to the c file
	 * @param clangParams additional commands for clang
	 * @return path to the LLVM IR file
	 * @throws IOException could not store the c file
	 * @throws InterruptedException clang compiler error
	 */
	protected static Path transpile(Path cFile, String ... clangParams) throws IOException, InterruptedException {
		
		// where should the output file be stored
		String filename = cFile.getFileName().toString();
		filename = filename.substring(0, filename.lastIndexOf('.')) + ".ll";
		String outputPath = Files.createTempDirectory("llvm_jnr").resolve(filename).toString();
		
		// compile the code
		runClang(cFile.toString(), outputPath, clangParams);
		
		return Paths.get(outputPath);
	}
	
	/**
	 * Compile the input c file into a LLVM IR file.
	 * 
	 * @param inputFile path to the c file
	 * @param outputFile to the LLVM IR file
	 * @param clangParams additional commands for clang
	 * @throws InterruptedException clang compiler error
	 * @throws IOException could not store the c file
	 */
	protected static void runClang(String inputFile, String outputFile, String ... clangParams) throws InterruptedException, IOException {

		// lazy initialization of clang
		if(clang == null)
			clang = Loader.load(org.bytedeco.llvm.program.clang.class);
		
		// no parameters, add at least optimization options
		// https://clang.llvm.org/docs/genindex.html
		// "-march=native", "-O3" produces sometimes runtime errors
		if(clangParams.length == 0)
			clangParams = new String[] { "-O3" };
		
		// translate c code to LLVM IR
		String[] commands = new String[clangParams.length+6];
		commands[0] = clang;
		commands[1] = "-S";
		commands[2] = "-emit-llvm";
		commands[3] = inputFile;
		commands[4] = "-o";
		commands[5] = outputFile;
		System.arraycopy(clangParams, 0, commands, 6, clangParams.length);
		System.out.println(String.join(" ", commands));
		ProcessBuilder pb = new ProcessBuilder(commands);
		pb.inheritIO().start().waitFor();
	}
}
