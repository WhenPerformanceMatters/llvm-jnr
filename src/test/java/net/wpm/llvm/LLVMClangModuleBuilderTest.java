package net.wpm.llvm;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Random;

import org.bytedeco.llvm.global.LLVM;
import org.junit.Assert;
import org.junit.Test;

import net.wpm.llvm.LLVMStoredModuleBuilderTest.MatMulInterface;
import net.wpm.llvm.module.LLVMMatMulTest;

public class LLVMClangModuleBuilderTest {

	public static void main(String[] args) throws Exception {
		
		final LLVMClangModuleBuilderTest test = new LLVMClangModuleBuilderTest();
//		test.testCIntrinsic();
//		test.testCCodeFile();
		test.testCachedFile();
//		test.testCCode();

		LLVM.LLVMShutdown();
		System.out.println("Finished");
	}

	
	/**
	 * https://clang.llvm.org/docs/LanguageExtensions.html#builtin-functions
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ParseException
	 * @throws NoSuchMethodException
	 * @throws IllegalClassFormatException
	 * @throws URISyntaxException
	 * @throws NoSuchAlgorithmException 
	 */
	@Test
	public void testCIntrinsic() throws IOException, InterruptedException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException, NoSuchAlgorithmException {
		final String cCode = "" +
				"int compute_abs(const int val)\n" + 
				"{\n" + 
				"   return __builtin_abs(val);\n"+
				"}\n";
		
		final LLVMModuleBuilder<AbsInterface> moduleBuilder = new LLVMClangModuleBuilder<>(cCode, AbsInterface.class);
		final LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<AbsInterface> program = compiler.compile(moduleBuilder, true)) {
			int result = program.invoke().compute_abs(-200);
			System.out.println("result: "+200);
			Assert.assertEquals(result, 200);
		}
	}
	
	@Test
	public void testCCodeFile() throws IOException, InterruptedException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException, NoSuchAlgorithmException {
		final Path cFile = Paths.get(LLVMClangModuleBuilderTest.class.getResource("matmul.c").toURI());
		
		final int M = 20, N = 20, K = 20;
		final Random rand = new Random(7);
		final float[] a = LLVMMatMulTest.createRandomArray(rand, M, K);
		final float[] b = LLVMMatMulTest.createRandomArray(rand, K, N);
		final float[] c = new float[M * N];
		
		final LLVMModuleBuilder<MatMulInterface> moduleBuilder = new LLVMClangModuleBuilder<>(cFile, MatMulInterface.class);
		final LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder, false, false)) {	
			long start = System.currentTimeMillis();
			program.invoke().matmul(a, b, c, M, N, K);
			System.out.println("c[0]"+c[0]+" took "+(System.currentTimeMillis()-start)+"ms");
			Assert.assertEquals(c[0], 7.0694447, 0.0002);
		}
	}
	
	@Test
	public void testCachedFile() throws IOException, InterruptedException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException, NoSuchAlgorithmException {
		final Path cFile = Paths.get(LLVMClangModuleBuilderTest.class.getResource("matmul.c").toURI());
		final Path cacheDir = Paths.get("i:\\V3C\\generated\\");
		
		final int M = 2000, N = 2000, K = 2000;
		final Random rand = new Random(7);
		final float[] a = LLVMMatMulTest.createRandomArray(rand, M, K);
		final float[] b = LLVMMatMulTest.createRandomArray(rand, K, N);
		final float[] c = new float[M * N];
		
		long start = System.currentTimeMillis();
		final LLVMCompiler compiler = new LLVMCompiler(true, false);
		System.out.println("llvm compiler setup after "+(System.currentTimeMillis()-start)+"ms");
		
		final LLVMClangModuleBuilder<MatMulInterface> moduleBuilder = new LLVMClangModuleBuilder<>(cFile, cacheDir, MatMulInterface.class);
		final boolean isOptimized = Files.exists(moduleBuilder.getLLVMFile());
		System.out.println("transpile after "+(System.currentTimeMillis()-start)+"ms");
				
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder, isOptimized, false, false)) {	
			System.out.println("llvm compile after "+(System.currentTimeMillis()-start)+"ms");
			
			long calcStart = System.currentTimeMillis();
			program.invoke().matmul(a, b, c, M, N, K);
			System.out.println("c[0]"+c[0]+" took "+(System.currentTimeMillis()-calcStart)+"ms");
			
			// store and optimized version
			if(isOptimized == false) {
				String filename = moduleBuilder.getLLVMFile().getFileName().toString();
				String bcFilename = filename.substring(0, filename.lastIndexOf('.')) + ".bc";			
				LLVMStoredModuleBuilder.storeBitcode(program.getOptimizedModule(), cacheDir.resolve(bcFilename));
				System.out.println("storeBitcode after "+(System.currentTimeMillis()-start)+"ms");
			}
		}
	}
	
	@Test
	public void testCCode() throws IOException, InterruptedException, ParseException, NoSuchMethodException, IllegalClassFormatException, NoSuchAlgorithmException {
		final String cCode = "" +
				"void matmul(const float *a, const float *b, float *c, const int M, const int N, const int K)\n" + 
				"{\n" + 
				"   for (int m = 0; m < M; m++) {\n" + 
				"  		for (int n = 0; n < N; n++) {\n" + 
				"   		float s = 0;\n" + 
				"   		for (int k = 0; k < K; k++) {\n" + 
				"   			s += a[m * K + k] * b[k * N + n];\n" + 
				"   		}\n" + 
				"   		c[m * N + n] = s;\n" + 
				"   	}\n" + 
				"  	}\n" + 
				"}";
		
		final int M = 20, N = 20, K = 20;
		final Random rand = new Random(7);
		final float[] a = LLVMMatMulTest.createRandomArray(rand, M, K);
		final float[] b = LLVMMatMulTest.createRandomArray(rand, K, N);
		final float[] c = new float[M * N];

		final LLVMModuleBuilder<MatMulInterface> moduleBuilder = new LLVMClangModuleBuilder<>(cCode, MatMulInterface.class);
		final LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder, false)) {
			program.invoke().matmul(a, b, c, M, N, K);
			System.out.println("c[0]"+c[0]);
			Assert.assertEquals(c[0], 7.0694447, 0.0002);
		}
	}
	
	/**
	 * This is a invocation interface for the LLVM function compute_abs
	 * 
	 * @author Nico Hezel
	 */
	public static interface AbsInterface {
		public int compute_abs(int val);
	}
}
