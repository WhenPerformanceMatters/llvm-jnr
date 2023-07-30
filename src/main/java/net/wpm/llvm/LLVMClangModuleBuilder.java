package net.wpm.llvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;


/**
 * Compile C code to a LLVM module and provides it via the {@link LLVMModuleBuilder#build()} method.  
 * 
 * @author Nico Hezel
 *
 * @param <T> invocation interface 
 */
public class LLVMClangModuleBuilder<T> extends LLVMStoredModuleBuilder<T> {

	protected static String clang;
	
	protected final Path cFile;
	protected final String[] clangParams;
	
	/**
	 * Compile a c file to a LLVM module
	 * 
	 * @param cFile containing c code
	 * @param invocationInterface invocation interface 
	 * @param clangParams additional commands for clang
	 * @throws NoSuchAlgorithmException unable to create hash code for file
	 * @throws IOException could not read the c file 
	 */
	public LLVMClangModuleBuilder(Path cFile, Class<T> invocationInterface, String ... clangParams) throws NoSuchAlgorithmException, IOException {
		this(cFile, null, invocationInterface, clangParams);
	}
	
	public LLVMClangModuleBuilder(Path cFile, Path cacheDir, Class<T> invocationInterface, String ... clangParams) throws NoSuchAlgorithmException, IOException {
		this(cFile, computeFileHash(cFile), cacheDir, invocationInterface, clangParams);
	}
	
	public LLVMClangModuleBuilder(Path cFile, String fileHash, Path cacheDir, Class<T> invocationInterface, String ... clangParams) throws NoSuchAlgorithmException, IOException  {
		super(llvmFile(cFile, fileHash, cacheDir), invocationInterface);
		this.cFile = cFile;
		this.clangParams = clangParams;
	}
	
	/**
	 * Store and compile the c code to a LLVM module
	 * 
	 * @param cCode string with c code
	 * @param invocationInterface invocation interface 
	 * @param clangParams additional commands for clang
	 * @throws NoSuchAlgorithmException 
	 * @throws IOException could not store the c file
	 */
	public LLVMClangModuleBuilder(String cCode, Class<T> invocationInterface, String ... clangParams) throws NoSuchAlgorithmException, IOException {
		this(cCode, null, invocationInterface, clangParams);
	}
	
	public LLVMClangModuleBuilder(String cCode, Path cacheDir, Class<T> invocationInterface, String ... clangParams) throws NoSuchAlgorithmException, IOException  {
		this(toCFile(cCode, cacheDir), "", cacheDir, invocationInterface);
	}
	
	public Path getCFile() {
		return cFile;
	}
	
	/**
	 * Get output llvm file name
	 * 
	 * @param cFile file with c-code
	 * @param cacheDir output directory
	 * @return output file containing llvm assembly
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static Path llvmFile(Path cFile, String fileHash, Path cacheDir) throws IOException, NoSuchAlgorithmException {
		
		// without a cache directory use a temporary directory
		if(cacheDir == null)
			cacheDir = Files.createTempDirectory("llvm_jnr");

		// where should the output file be stored
		String filename = cFile.getFileName().toString();
		
		// check if a binary version already exists
		String bcFilename = filename.substring(0, filename.lastIndexOf('.')) + fileHash + ".bc";
		final Path bcOutputPath = cacheDir.resolve(bcFilename);	
		if(Files.exists(bcOutputPath))
			return bcOutputPath;
		
		// check if a text version already exists
		String irFilename = filename.substring(0, filename.lastIndexOf('.')) + fileHash + ".ll";		
		return cacheDir.resolve(irFilename);
	}
	
	/**
	 * Store c code to file
	 * 
	 * @param cCode
	 * @param cacheDir
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static Path toCFile(String cCode, Path cacheDir) throws IOException, NoSuchAlgorithmException {
		String hash = computeHash(cCode);
		
		// without a cache directory use a temporary directory
		if(cacheDir == null)
			cacheDir = Files.createTempDirectory("llvm_jnr");

		// where should the output file be stored
		final Path cFile = cacheDir.resolve(hash + ".c");
		Files.write(cFile, cCode.getBytes());
		
		return cFile;
	}
	
	/**
	 * Compute the md5 hash of the given file
	 * 
	 * @param cFile
	 * @return md5 hash
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	protected static String computeFileHash(Path cFile) throws NoSuchAlgorithmException, IOException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(Files.readAllBytes(cFile));
		byte[] digest = md.digest();
		return bytesToHex(digest);	
	}
	
	/**
	 * Compute the md5 hash of the given string
	 * 
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	protected static String computeHash(String text) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(text.getBytes());
		byte[] digest = md.digest();
		return bytesToHex(digest);	
	}
	
	/**
	 * Convert to HEX values
	 */
	private static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	
	
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
	
	@Override
	public LLVMModuleRef build() {
		
		if(Files.exists(super.getLLVMFile()) == false) {
			try {
				runClang(cFile.toString(), super.getLLVMFile().toString(), clangParams);			
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		
		return super.build();
	}
}