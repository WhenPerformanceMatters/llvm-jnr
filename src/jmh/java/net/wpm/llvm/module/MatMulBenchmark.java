package net.wpm.llvm.module;


import static org.bytedeco.mkl.global.mkl_rt.CblasNoTrans;
import static org.bytedeco.mkl.global.mkl_rt.CblasRowMajor;
import static org.bytedeco.mkl.global.mkl_rt.mkl_cblas_jit_create_sgemm;
import static org.bytedeco.mkl.global.mkl_rt.mkl_jit_destroy;
import static org.bytedeco.mkl.global.mkl_rt.mkl_jit_get_sgemm_ptr;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.LongPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.mkl.global.mkl_rt;
import org.bytedeco.mkl.global.mkl_rt.sgemm_jit_kernel_t;

import net.wpm.llvm.LLVMCompiler;
import net.wpm.llvm.LLVMProgram;
import net.wpm.llvm.LLVMStoredModuleBuilder;


/**
 * Matrix multiply benchmark.
 *
 * To run this sample, execute this command:
 * mvn clean compile exec:java -Djavacpp.platform.host
 *
 * If you set usePollyParallel, you may have to modify the file name of LLVMLoadLibraryPermanently().
 *
 * Note: This code is equivalent to:
 * clang -O3 -march=native -mllvm -polly -mllvm -polly-vectorizer=stripmine
 *
 * Note: Instead of JNA, to obtain maximum performance, FunctionPointer should be used as shown here:
 * But this would require the entire JavaCPP building environment 
 * https://github.com/bytedeco/javacpp/blob/master/src/test/java/org/bytedeco/javacpp/PointerTest.java
 *
 * @author Yu Kobayashi
 * 
 * -------------------
 * 
 * Tests on a Ryzen 2700X
 * M = 2000, N = 2000, K = 2000;
 * usePolly = true;
 * usePollyParallel = false;
 * testIterations = 10;
 * 
 * MKL: 284,999140ms. c[0] = 489,814423
 * LLVM with Polly: 1096,962030ms. c[0] = 489,814575
 * LLVM with Polly: 1092,440860ms. c[0] = 489,814575
 * Pure Java: 16988,667810ms. c[0] = 489,814575
 * 
 * -------------------
 * 
 * Tests on a Ryzen 2700X
 * M = 20, N = 20, K = 20;
 * usePolly = true;
 * usePollyParallel = false;
 * testIterations = 100;
 * 
 * MKL: 0,000847ms. c[0] = 7,069445
 * LLVM with JNR and Polly: 0,000882ms. c[0] = 7,069445
 * LLVM with JNA and Polly: 0,001976ms. c[0] = 7,069445
 * Pure Java: 0,006404ms. c[0] = 7,069445
 * 
 * -------------------
 * 
 * Tests on a Ryzen 2700X
 * M = 20, N = 20, K = 20;
 * usePolly = true;
 * usePollyParallel = false;
 * testIterations = 10000;
 * 
 * MKL: 0,000740ms. c[0] = 7,069445
 * LLVM with JNR and Polly: 0,000710ms. c[0] = 7,069445
 * LLVM with JNA and Polly: 0,001603ms. c[0] = 7,069445
 * Pure Java: 0,006228ms. c[0] = 7,069445
 * 
 */
public class MatMulBenchmark {

	static final int M = 20, N = 20, K = 20;
	static final boolean usePolly = true;
	static final boolean usePollyParallel = false;
	static final boolean printResult = false;
	static final int testIterations = 10000;
	static final int warmupIterations = 100000;

	static final Random rand = new Random(7);


	public static void main(String[] args) throws Throwable {    	
		float[] a = createRandomArray(M, K);
		float[] b = createRandomArray(K, N);
		float[] c = new float[M * N];

		benchmarkMKL(a, b, c);
		benchmarkLLVMJNR(a, b, c);
		benchmarkLLVMJNA(a, b, c);
		benchmarkPureJava(a, b, c);

		System.out.println("Finished.");
	}

	static void benchmarkMKL(float[] a, float[] b, float[] c) {
		Pointer jitter = new Pointer();
		mkl_cblas_jit_create_sgemm(jitter, CblasRowMajor, CblasNoTrans, CblasNoTrans, M, N, K, 1.0f, K, N, 0.0f, N);
		sgemm_jit_kernel_t sgemm = mkl_jit_get_sgemm_ptr(jitter);

		FloatPointer A = new FloatPointer(a);
		FloatPointer B = new FloatPointer(b);
		FloatPointer C = new FloatPointer(c);

		mkl_rt.MKL_Set_Num_Threads(usePollyParallel ? 8 : 1);

		// warm up
		for (int i = 0; i < warmupIterations; i++) 
			sgemm.call(jitter, A, B, C);

		long start = System.nanoTime();
		for (int i = 0; i < testIterations; i++) 
			sgemm.call(jitter, A, B, C);
		long end = System.nanoTime();
		System.out.printf("MKL: %fms. c[0] = %f\n", (end - start) / (testIterations * 1000d * 1000d), C.get(0));

		mkl_jit_destroy(jitter);
	}

	static void benchmarkPureJava(float[] a, float[] b, float[] c) {
		assert a.length == M * K;
		assert b.length == K * N;
		assert c.length == M * N;

		// warm up
		for (int i = 0; i < warmupIterations; i++) 
			sgemmJava(a, b, c, M, N, K);

		long start = System.nanoTime();
		for (int i = 0; i < testIterations; i++) 
			sgemmJava(a, b, c, M, N, K);
		long end = System.nanoTime();
		System.out.printf("Pure Java: %fms. c[0] = %f\n", (end - start) / (testIterations * 1000d * 1000d), c[0]);
		printArray(c);
	}

	static void sgemmJava(float[] a, float[] b, float[] c, int M, int N, int K) {
		for (int m = 0; m < M; m++) {
			for (int n = 0; n < N; n++) {
				float s = 0;
				for (int k = 0; k < K; k++) {
					s += a[m * K + k] * b[k * N + n];
				}
				c[m * N + n] = s;
			}
		}
	}

	static void benchmarkLLVMJNR(float[] a, float[] b, float[] c) throws Throwable {
		assert a.length == M * K;
		assert b.length == K * N;
		assert c.length == M * N;

		jnr.ffi.Runtime runtime = jnr.ffi.Runtime.getSystemRuntime();        
		jnr.ffi.Pointer aPtr = jnr.ffi.Memory.allocateDirect(runtime, a.length * 4, false);
		jnr.ffi.Pointer bPtr = jnr.ffi.Memory.allocateDirect(runtime, b.length * 4, false);
		jnr.ffi.Pointer cPtr = jnr.ffi.Memory.allocateDirect(runtime, c.length * 4, false);

		aPtr.put(0, a, 0, a.length);
		bPtr.put(0, b, 0, b.length);
		cPtr.put(0, c, 0, c.length);

		// The code in the file containing code is for M,N,K = 20.
		Path file = Paths.get(MatMulBenchmark.class.getResource((M == 20) ? "matmul20.ll" : "matmul2000.ll").toURI());
		LLVMStoredModuleBuilder<MatMulInterface> moduleBuilder = new LLVMStoredModuleBuilder<>(file, MatMulInterface.class);
		LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder)) {	

			// warm up
			for (int i = 0; i < warmupIterations; i++) 
				program.invoke().matmul(aPtr, bPtr, cPtr);

			long start = System.nanoTime();
			for (int i = 0; i < testIterations; i++) 
				program.invoke().matmul(aPtr, bPtr, cPtr);
			long end = System.nanoTime();

			cPtr.get(0, c, 0, c.length);
			System.out.printf("LLVM with JNR and %s: %fms. c[0] = %f\n",
							usePolly ? "Polly" : "without Polly",
							(end - start) / (testIterations * 1000d * 1000d),
							c[0]);
			printArray(c);
		}
	}

	static void benchmarkLLVMJNA(float[] a, float[] b, float[] c) throws Throwable {
		assert a.length == M * K;
		assert b.length == K * N;
		assert c.length == M * N;

		com.sun.jna.Memory aPtr = new com.sun.jna.Memory(a.length * 4);
		aPtr.getByteBuffer(0, aPtr.size()).order(ByteOrder.nativeOrder()).asFloatBuffer().put(a);

		com.sun.jna.Memory bPtr = new com.sun.jna.Memory(b.length * 4);
		bPtr.getByteBuffer(0, bPtr.size()).order(ByteOrder.nativeOrder()).asFloatBuffer().put(b);

		com.sun.jna.Memory cPtr = new com.sun.jna.Memory(c.length * 4);
		cPtr.getByteBuffer(0, cPtr.size()).order(ByteOrder.nativeOrder()).asFloatBuffer().put(c);


		// The code in the IR file works only for M,N,K = 20.
		Path file = Paths.get(MatMulBenchmark.class.getResource((M == 20) ? "matmul20.ll" : "matmul2000.ll").toURI());
		LLVMStoredModuleBuilder<MatMulInterface> moduleBuilder = new LLVMStoredModuleBuilder<>(file, MatMulInterface.class);
		LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder)) {	
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(new com.sun.jna.Pointer(program.getAddress("matmul")));

			// warm up
			for (int i = 0; i < warmupIterations; i++) 
				func.invoke(Void.class, new Object[]{aPtr, bPtr, cPtr});

			long start = System.nanoTime();
			for (int i = 0; i < testIterations; i++) 
				func.invoke(Void.class, new Object[]{aPtr, bPtr, cPtr});
			long end = System.nanoTime();

			cPtr.getByteBuffer(0, aPtr.size()).order(ByteOrder.nativeOrder()).asFloatBuffer().get(c);
			System.out.printf("LLVM with JNA and %s: %fms. c[0] = %f\n",
					usePolly ? "Polly" : "without Polly",
							(end - start) / (testIterations * 1000d * 1000d),
							c[0]);
			printArray(c);
		}
	}

	static float[] createRandomArray(int m, int n) {
		float[] ary = new float[m * n];
		for (int i = 0; i < ary.length; i++) {
			ary[i] = rand.nextFloat();
		}
		return ary;
	}

	static void printArray(float[] ary) {
		if (printResult) {
			for (float v : ary) {
				System.out.println(v);
			}
		}
	}

	/**
	 * This is an invocation interface for the LLVM function in the matmul.ll file.
	 * The interface defines the same functions and signatures as those in the file.
	 * 
	 * @author Nico Hezel
	 */
	public static interface MatMulInterface {
		public void matmul(jnr.ffi.Pointer a, jnr.ffi.Pointer b, jnr.ffi.Pointer c);
	}
}