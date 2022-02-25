package net.wpm.llvm;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Random;

import org.bytedeco.llvm.global.LLVM;
import org.junit.Assert;
import org.junit.Test;

import net.wpm.llvm.LLVMStoredModuleBuilderTest.MatMulInterface;
import net.wpm.llvm.module.LLVMMatMulTest;

public class LLVMClangModuleBuilderTest {

	public static void main(String[] args) throws InterruptedException, IOException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException {
		
		final LLVMClangModuleBuilderTest test = new LLVMClangModuleBuilderTest();
		test.testCIntrinsic();
		test.testCCodeFile();
		test.testCCode();

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
	 */
	@Test
	public void testCIntrinsic() throws IOException, InterruptedException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException {
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
	public void testCCodeFile() throws IOException, InterruptedException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException {
		final Path cFile = Paths.get(LLVMClangModuleBuilderTest.class.getResource("matmul.c").toURI());
		
		final int M = 20, N = 20, K = 20;
		final Random rand = new Random(7);
		final float[] a = LLVMMatMulTest.createRandomArray(rand, M, K);
		final float[] b = LLVMMatMulTest.createRandomArray(rand, K, N);
		final float[] c = new float[M * N];

		final LLVMModuleBuilder<MatMulInterface> moduleBuilder = new LLVMClangModuleBuilder<>(cFile, MatMulInterface.class);
		final LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder, false)) {			
			long start = System.currentTimeMillis();
			program.invoke().matmul(a, b, c, M, N, K);
			System.out.println("c[0]"+c[0]+" took "+(System.currentTimeMillis()-start)+"ms");
			Assert.assertEquals(c[0], 7.0694447, 0.0002);
		}
	}
	
	@Test
	public void testCCode() throws IOException, InterruptedException, ParseException, NoSuchMethodException, IllegalClassFormatException {
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
