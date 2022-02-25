package net.wpm.llvm.module;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.bytedeco.llvm.global.LLVM;
import org.junit.Assert;
import org.junit.Test;

import net.wpm.llvm.LLVMCompiler;
import net.wpm.llvm.LLVMProgram;
import net.wpm.llvm.module.LLVMFac.FacInterface;

/**
 * Test the {@link LLVMFac} module builder and print its generated IR code in the console.
 * 
 * @author Nico Hezel
 */
public class LLVMFacTest {
	
	public static void main(String[] args) throws InterruptedException, IOException, ParseException, NoSuchMethodException, IllegalClassFormatException, URISyntaxException {
		
		final LLVMFacTest test = new LLVMFacTest();
		test.testFacModule();

		LLVM.LLVMShutdown();
		System.out.println("Finished");
	}

	@Test
	public void testFacModule() throws NoSuchMethodException, IllegalClassFormatException {

		LLVMFac moduleBuilder = new LLVMFac();
		LLVMCompiler compiler = new LLVMCompiler(true, false);
		try(LLVMProgram<FacInterface> program = compiler.compile(moduleBuilder, true)) {
			int result = program.invoke().fac(10);
			System.out.println(result);
			Assert.assertEquals(result, 3628800);
		}
	}
}
