package net.wpm.llvm;

import java.io.FileNotFoundException;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import org.bytedeco.llvm.global.LLVM;

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

	public static void main(String[] args) throws NoSuchMethodException, IllegalClassFormatException, FileNotFoundException, ParseException, URISyntaxException {

		final int M = 20, N = 20, K = 20;
		float[] a = LLVMMatMulTest.createRandomArray(M, K);
		float[] b = LLVMMatMulTest.createRandomArray(K, N);
		float[] c = new float[M * N];

		Path file = Paths.get(LLVMStoredModuleBuilderTest.class.getResource("matmul.ir").toURI());
		LLVMStoredModuleBuilder<MatMulInterface> moduleBuilder = new LLVMStoredModuleBuilder<>(file, MatMulInterface.class);
		LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder, true)) {
			program.invoke().matmul(a, b, c, M, N, K);
			System.out.println("c[0]"+c[0]);
		}

		LLVM.LLVMShutdown();
		System.out.println("finished");
	}

	/**
	 * This is a invocation interface for the LLVM function in the matmul.ir file.
	 * 
	 * @author Nico Hezel
	 */
	public static interface MatMulInterface {
		public void matmul(float[] a, float[] b, float[] c, int M, int N, int K);
	}
}
