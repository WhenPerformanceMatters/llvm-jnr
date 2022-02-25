package jnr.ffi.provider.jffi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jnr.ffi.LibraryOption;

/**
 * Maps arbitrary symbol names to memory addresses
 * 
 * @author Nico Hezel
 */
public class LLVMSymbolLibrary extends NativeLibrary {

	protected static final Map<LibraryOption, Object> options = new HashMap<>();
	
	protected final Map<String, Long> funcNameToAddress;

	LLVMSymbolLibrary(Map<String, Long> funcNameToAddress) {
		super(Collections.emptyList(), Collections.emptyList(), options);
		this.funcNameToAddress = funcNameToAddress;
	}

	public long getSymbolAddress(String name) {
		return funcNameToAddress.getOrDefault(name, -1L);
	}

	long findSymbolAddress(String name) {
		return funcNameToAddress.getOrDefault(name, -1L);
	}
}

