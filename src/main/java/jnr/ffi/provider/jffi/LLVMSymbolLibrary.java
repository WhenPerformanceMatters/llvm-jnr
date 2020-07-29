package jnr.ffi.provider.jffi;

import java.util.Collections;
import java.util.Map;

/**
 * Maps arbitrary symbol names to memory addresses
 * 
 * @author Nico Hezel
 */
public class LLVMSymbolLibrary extends NativeLibrary {

	protected final Map<String, Long> funcNameToAddress;

	LLVMSymbolLibrary(Map<String, Long> funcNameToAddress) {
		super(Collections.emptyList(), Collections.emptyList());
		this.funcNameToAddress = funcNameToAddress;
	}

	public long getSymbolAddress(String name) {
		return funcNameToAddress.getOrDefault(name, -1L);
	}

	long findSymbolAddress(String name) {
		return funcNameToAddress.getOrDefault(name, -1L);
	}
}

