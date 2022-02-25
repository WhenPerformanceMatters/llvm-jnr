package net.wpm.llvm;

import java.io.FileNotFoundException;
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

import net.wpm.llvm.module.LLVMMatMulTest;

/**
 * Create LLVM assembly with this website
 * http://ellcc.org/demo/index.cgi
 * e.g. paste this c code in there
 * 
 * void matmul(const float *a, const float *b, float *c, const int M, const int N, const int K)
 * {
 *		for (int m = 0; m < M; m++) {
 *   		for (int n = 0; n < N; n++) {
 *   			float s = 0;
 *   			for (int k = 0; k < K; k++) {
 *   				s += a[m * K + k] * b[k * N + n];
 *   			}
 *   			c[m * N + n] = s;
 *   		}
 *		}
 * }
 * 
 * Sometimes it is necessary to clean up the method signature like this:
 * define void @matmul(float* %0, float* %1, float* %2, i32 %3, i32 %4, i32 %5)
 * 
 * 
 * @author Nico Hezel
 *
 */
public class LLVMStoredModuleBuilderTest {

	public static void main(String[] args) throws InterruptedException, IOException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException {
		
		final LLVMStoredModuleBuilderTest test = new LLVMStoredModuleBuilderTest();
		test.testStoredModuleBuilder();

		LLVM.LLVMShutdown();
		System.out.println("Finished");
	}

	@Test
	public void testStoredModuleBuilder() throws NoSuchMethodException, IllegalClassFormatException, FileNotFoundException, ParseException, URISyntaxException {
		final Path file = Paths.get(LLVMStoredModuleBuilderTest.class.getResource("matmul.ll").toURI());

		final int M = 20, N = 20, K = 20;
		final Random rand = new Random(7);
		final float[] a = LLVMMatMulTest.createRandomArray(rand, M, K);
		final float[] b = LLVMMatMulTest.createRandomArray(rand, K, N);
		final float[] c = new float[M * N];

		final LLVMModuleBuilder<MatMulInterface> moduleBuilder = new LLVMStoredModuleBuilder<>(file, MatMulInterface.class);
		final LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder, true)) {
			System.out.println("compiled");
			program.invoke().matmul(a, b, c, M, N, K);
			System.out.println("c[0]"+c[0]);
			Assert.assertEquals(c[0], 7.0694447, 0.0002);
		}
	}

	/**
	 * This is a invocation interface for the LLVM function in the matmul.ll file.
	 * 
	 * @author Nico Hezel
	 */
	public static interface MatMulInterface {
		public void matmul(float[] a, float[] b, float[] c, int M, int N, int K);
	}
}
