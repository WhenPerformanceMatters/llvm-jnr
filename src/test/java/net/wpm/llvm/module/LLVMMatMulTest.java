package net.wpm.llvm.module;

import java.lang.instrument.IllegalClassFormatException;
import java.util.Random;

import org.bytedeco.llvm.global.LLVM;

import com.kenai.jffi.MemoryIO;
import com.kenai.jffi.Platform;

import net.wpm.llvm.LLVMCompiler;
import net.wpm.llvm.LLVMProgram;
import net.wpm.llvm.module.LLVMMatMul.MatMulInterface;

/**
 * Test the {@link LLVMMatMul} module builder and print its generated IR code in the console.
 * 
 * @author Nico Hezel *
 */
public class LLVMMatMulTest {

	static final int M = 20, N = 20, K = 20;
	static final boolean usePolly = true;
	static final boolean usePollyParallel = false;
	static final boolean dumpLLVMCode = false;
	static final boolean printResult = false;
	static final int testIterations = 100;


	public static void main(String[] args) throws NoSuchMethodException, IllegalClassFormatException  {

		int SIZE = Platform.getPlatform().addressSize();
		System.out.println("SIZE"+SIZE);
		long MASK = Platform.getPlatform().addressMask();
		System.out.println("MASK"+MASK);

		final Random rand = new Random(7);
		float[] a = createRandomArray(rand, M, K);
		float[] b = createRandomArray(rand, K, N);
		float[] c = new float[M * N];

		MemoryIO IO = MemoryIO.getInstance();        
		long aAddress = IO.allocateMemory(a.length * 4, false);
		IO.putFloatArray(aAddress, a, 0, a.length);
		long bAddress = IO.allocateMemory(b.length * 4, false);
		IO.putFloatArray(bAddress, b, 0, b.length);
		long cAddress = IO.allocateMemory(c.length * 4, false);
		IO.putFloatArray(cAddress, c, 0, c.length);

		jnr.ffi.Runtime runtime = jnr.ffi.Runtime.getSystemRuntime();
		jnr.ffi.Pointer aPtr = jnr.ffi.Pointer.wrap(runtime, aAddress);
		jnr.ffi.Pointer bPtr = jnr.ffi.Pointer.wrap(runtime, bAddress);
		jnr.ffi.Pointer cPtr = jnr.ffi.Pointer.wrap(runtime, cAddress);

		LLVMMatMul moduleBuilder = new LLVMMatMul(K, M, N);
		LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<MatMulInterface> program = compiler.compile(moduleBuilder, true)) {

			// warm up
			program.invoke().matmul(aPtr, bPtr, cPtr);

			// run tests
			final long start = System.nanoTime();
			for (int i = 0; i < testIterations; i++) 
				program.invoke().matmul(aPtr, bPtr, cPtr);
			final long end = System.nanoTime();

			// print
			IO.getFloatArray(cAddress, c, 0, c.length);
			System.out.printf("LLVM%s: %fms. c[0] = %f\n",
					usePolly ? " with Polly" : " without Polly",
							(end - start) / (testIterations * 1000d * 1000d),
							c[0]);
			printArray(c);
		}

		LLVM.LLVMShutdown();
		System.out.println("finished");
	}

	public static float[] createRandomArray(Random rand, int m, int n) {
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
}
