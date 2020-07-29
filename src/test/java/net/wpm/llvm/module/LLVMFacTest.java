package net.wpm.llvm.module;

import java.lang.instrument.IllegalClassFormatException;

import org.bytedeco.llvm.global.LLVM;

import net.wpm.llvm.LLVMCompiler;
import net.wpm.llvm.LLVMProgram;
import net.wpm.llvm.module.LLVMFac.FacInterface;

/**
 * Test the {@link LLVMFac} module builder and print its generated IR code in the console.
 * 
 * @author Nico Hezel
 */
public class LLVMFacTest {

	public static void main(String[] args) throws NoSuchMethodException, IllegalClassFormatException {

		LLVMFac moduleBuilder = new LLVMFac();
		LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<FacInterface> program = compiler.compile(moduleBuilder, true)) {
			int result = program.invoke().fac(10);
			System.out.println(result);
		}

		LLVM.LLVMShutdown();
		System.out.println("finished");
	}
}
