package net.wpm.llvm.module;


import static org.bytedeco.llvm.global.LLVM.LLVMAddFunction;
import static org.bytedeco.llvm.global.LLVM.LLVMAddIncoming;
import static org.bytedeco.llvm.global.LLVM.LLVMAppendBasicBlock;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildAdd;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildBr;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildCondBr;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildFAdd;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildFMul;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildICmp;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildInBoundsGEP;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildLoad;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildMul;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildPhi;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildRetVoid;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildStore;
import static org.bytedeco.llvm.global.LLVM.LLVMCCallConv;
import static org.bytedeco.llvm.global.LLVM.LLVMConstInt;
import static org.bytedeco.llvm.global.LLVM.LLVMConstReal;
import static org.bytedeco.llvm.global.LLVM.LLVMCreateBuilder;
import static org.bytedeco.llvm.global.LLVM.LLVMDisposeBuilder;
import static org.bytedeco.llvm.global.LLVM.LLVMFloatType;
import static org.bytedeco.llvm.global.LLVM.LLVMFunctionType;
import static org.bytedeco.llvm.global.LLVM.LLVMGetInsertBlock;
import static org.bytedeco.llvm.global.LLVM.LLVMGetParam;
import static org.bytedeco.llvm.global.LLVM.LLVMInt32Type;
import static org.bytedeco.llvm.global.LLVM.LLVMIntEQ;
import static org.bytedeco.llvm.global.LLVM.LLVMModuleCreateWithName;
import static org.bytedeco.llvm.global.LLVM.LLVMPointerType;
import static org.bytedeco.llvm.global.LLVM.LLVMPositionBuilderAtEnd;
import static org.bytedeco.llvm.global.LLVM.LLVMSetFunctionCallConv;
import static org.bytedeco.llvm.global.LLVM.LLVMVoidType;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import jnr.ffi.Pointer;
import net.wpm.llvm.LLVMModuleBuilder;


/**
 * Matrix Multiplication example from Yu Kobayashi
 * https://github.com/bytedeco/javacpp-presets/blob/master/llvm/samples/polly/MatMulBenchmark.java
 * 
 * This class build a matrix multiplication function for a specific set of K,M,N.
 * 
 * @author Nico Hezel
 */
public class LLVMMatMul implements LLVMModuleBuilder<LLVMMatMul.MatMulInterface> {

	protected final LLVMTypeRef llvmVoidType;
	protected final LLVMTypeRef llvmInt32Type;
	protected final LLVMTypeRef llvmFloatType;
	protected final LLVMTypeRef llvmFloatPointerType;

	protected final int K;
	protected final int M;
	protected final int N;


	public LLVMMatMul(int K, int M, int N) {
		this.K = K;
		this.M = M;
		this.N = N;

		llvmVoidType = LLVMVoidType();
		llvmInt32Type = LLVMInt32Type();
		llvmFloatType = LLVMFloatType();

		// Sequential types represents "arrays" of types.
		// This is a super class for array, vector, and pointer types.
		// https://llvm.org/doxygen/group__LLVMCCoreTypeSequential.html
		llvmFloatPointerType = LLVMPointerType(llvmFloatType, 0);
	}

	@Override
	public LLVMModuleRef build() {
		LLVMBuilderRef builder = LLVMCreateBuilder();

		LLVMModuleRef module = LLVMModuleCreateWithName("matmulModule");

		// Create function
		// https://llvm.org/doxygen/group__LLVMCCoreTypeFunction.html#ga8b0c32e7322e5c6c1bf7eb95b0961707
		LLVMTypeRef funcType = LLVMFunctionType(
				llvmVoidType,
				new PointerPointer<>(llvmFloatPointerType, llvmFloatPointerType, llvmFloatPointerType),
				3,
				0);
		LLVMValueRef func = LLVMAddFunction(module, "matmul", funcType);
		LLVMSetFunctionCallConv(func, LLVMCCallConv);

		LLVMValueRef paramA = LLVMGetParam(func, 0);
		LLVMValueRef paramB = LLVMGetParam(func, 1);
		LLVMValueRef paramC = LLVMGetParam(func, 2);

		// entry basic block
		LLVMBasicBlockRef entryBB = LLVMAppendBasicBlock(func, "entry");	// root block
		LLVMPositionBuilderAtEnd(builder, entryBB);

		// loop M basic block
		LLVMBasicBlockRef loopMBB = LLVMAppendBasicBlock(func, "loopM");
		LLVMBuildBr(builder, loopMBB);					// flow control jump to block https://llvm.org/docs/LangRef.html#br-instruction
		LLVMPositionBuilderAtEnd(builder, loopMBB);		// end of block

		// loop M index variable
		LLVMValueRef loopMIdx = LLVMBuildPhi(builder, llvmInt32Type, "m"); // https://llvm.org/docs/LangRef.html#i-phi
		LLVMAddIncoming(loopMIdx, toConstInt(0), entryBB, 1);

		// loop N basic block
		LLVMBasicBlockRef loopNBB = LLVMAppendBasicBlock(func, "loopN");
		LLVMBuildBr(builder, loopNBB);
		LLVMPositionBuilderAtEnd(builder, loopNBB);

		// loop N index variable
		LLVMValueRef loopNIdx = LLVMBuildPhi(builder, llvmInt32Type, "n");
		LLVMAddIncoming(loopNIdx, toConstInt(0), loopMBB, 1);

		// loop K basic block
		LLVMBasicBlockRef loopKBB = LLVMAppendBasicBlock(func, "loopK");
		LLVMBuildBr(builder, loopKBB);
		LLVMPositionBuilderAtEnd(builder, loopKBB);

		// loop K index variable
		LLVMValueRef loopKIdx = LLVMBuildPhi(builder, llvmInt32Type, "k");
		LLVMAddIncoming(loopKIdx, toConstInt(0), loopNBB, 1);

		// s = 0
		LLVMValueRef s = LLVMBuildPhi(builder, llvmFloatType, "s");
		LLVMAddIncoming(s, toConstFloat(0), loopNBB, 1);

		// s += a[m * K + k] * b[k * N + n]
		LLVMValueRef mMulK = LLVMBuildMul(builder, loopMIdx, toConstInt(K), "m * K");
		LLVMValueRef mMulKAddK = LLVMBuildAdd(builder, mMulK, loopKIdx, "m * K + k");
		LLVMValueRef aAryPtr = LLVMBuildInBoundsGEP(builder, paramA, mMulKAddK, 1, new BytePointer("&a[m * K + k]"));
		LLVMValueRef aAryValue = LLVMBuildLoad(builder, aAryPtr, "a[m * K + k]");
		LLVMValueRef kMulN = LLVMBuildMul(builder, loopKIdx, toConstInt(N), "k * N");
		LLVMValueRef kMulNAddN = LLVMBuildAdd(builder, kMulN, loopNIdx, "k * N + n");
		LLVMValueRef bAryPtr = LLVMBuildInBoundsGEP(builder, paramB, kMulNAddN, 1, new BytePointer("&b[k * N + n]"));
		LLVMValueRef bAryValue = LLVMBuildLoad(builder, bAryPtr, "b[k * N + n]");
		LLVMValueRef aMulB = LLVMBuildFMul(builder, aAryValue, bAryValue, "a[m * K + k] * b[k * N + n]");
		LLVMValueRef sAddAMulB = LLVMBuildFAdd(builder, s, aMulB, "s + a[m * K + k] * b[k * N + n]");

		// k++
		LLVMValueRef nextLoopKIdx = LLVMBuildAdd(builder, loopKIdx, toConstInt(1), "k + 1");

		// k == K
		LLVMValueRef kEndCond = LLVMBuildICmp(builder, LLVMIntEQ, nextLoopKIdx, toConstInt(K), "k == K");

		LLVMBasicBlockRef loopKEndBB = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef afterKBB = LLVMAppendBasicBlock(func, "afterK");
		LLVMBuildCondBr(builder, kEndCond, afterKBB, loopKBB);
		LLVMPositionBuilderAtEnd(builder, afterKBB);
		LLVMAddIncoming(loopKIdx, nextLoopKIdx, loopKEndBB, 1);
		LLVMAddIncoming(s, sAddAMulB, loopKEndBB, 1);

		// c[m * N + n] = s
		LLVMValueRef mMulN = LLVMBuildMul(builder, loopMIdx, toConstInt(N), "m * N");
		LLVMValueRef mMulNAddN = LLVMBuildAdd(builder, mMulN, loopNIdx, "m * N + n");
		LLVMValueRef cAryPtr = LLVMBuildInBoundsGEP(builder, paramC, mMulNAddN, 1, new BytePointer("&c[m * N + n]"));
		LLVMBuildStore(builder, sAddAMulB, cAryPtr);

		// n++
		LLVMValueRef nextLoopNIdx = LLVMBuildAdd(builder, loopNIdx, toConstInt(1), "n + 1");

		// n == N
		LLVMValueRef nEndCond = LLVMBuildICmp(builder, LLVMIntEQ, nextLoopNIdx, toConstInt(N), "n == N");

		LLVMBasicBlockRef loopNEndBB = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef afterNBB = LLVMAppendBasicBlock(func, "afterN");
		LLVMBuildCondBr(builder, nEndCond, afterNBB, loopNBB);
		LLVMPositionBuilderAtEnd(builder, afterNBB);
		LLVMAddIncoming(loopNIdx, nextLoopNIdx, loopNEndBB, 1);

		// m++
		LLVMValueRef nextLoopMIdx = LLVMBuildAdd(builder, loopMIdx, toConstInt(1), "m + 1");

		// m == M
		LLVMValueRef mEndCond = LLVMBuildICmp(builder, LLVMIntEQ, nextLoopMIdx, toConstInt(M), "m == M");

		LLVMBasicBlockRef loopMEndBB = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef afterMBB = LLVMAppendBasicBlock(func, "afterM");
		LLVMBuildCondBr(builder, mEndCond, afterMBB, loopMBB);
		LLVMPositionBuilderAtEnd(builder, afterMBB);
		LLVMAddIncoming(loopMIdx, nextLoopMIdx, loopMEndBB, 1);

		// return
		LLVMBuildRetVoid(builder);

		LLVMDisposeBuilder(builder);

		return module;
	}

	protected LLVMValueRef toConstInt(int v) {
		return LLVMConstInt(llvmInt32Type, v, 0);
	}

	protected LLVMValueRef toConstFloat(float v) {
		return LLVMConstReal(llvmFloatType, v);
	}

	public static interface MatMulInterface {
		//		public void matmul(float[] a, float[] b, float[] c);
		public void matmul(Pointer a, Pointer b, Pointer c);
	}

	@Override
	public Class<MatMulInterface> getInvocationInterface() {
		return MatMulInterface.class;
	}
}