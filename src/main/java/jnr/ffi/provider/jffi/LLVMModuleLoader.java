package jnr.ffi.provider.jffi;

import static jnr.ffi.provider.jffi.Util.getBooleanProperty;

import java.util.Collection;
import java.util.Map;

import jnr.ffi.LibraryOption;

/**
 * Maps arbitrary symbol addresses to the corresponding methods in the interface with the same signature.
 * 
 * @author Nico Hezel
 *
 * @param <T>
 */
public class LLVMModuleLoader<T>  extends jnr.ffi.LibraryLoader<T> {
	static final boolean ASM_ENABLED = getBooleanProperty("jnr.ffi.asm.enabled", true);

	protected final Map<String, Long> funcNameToAddress;

	public LLVMModuleLoader(Class<T> interfaceClass, Map<String, Long> funcNameToAddress) {
		super(interfaceClass);
		this.funcNameToAddress = funcNameToAddress;
	}

	@Override
	public T loadLibrary(Class<T> interfaceClass, Collection<String> libraryNames, Collection<String> searchPaths, Map<LibraryOption, Object> options, boolean failImmediately) {
		final LLVMSymbolLibrary nativeLibrary = new LLVMSymbolLibrary(funcNameToAddress);

		try {
			return ASM_ENABLED
					? new AsmLibraryLoader().loadLibrary(nativeLibrary, interfaceClass, options, failImmediately)
							: new ReflectionLibraryLoader().loadLibrary(nativeLibrary, interfaceClass, options, failImmediately);

		} catch (RuntimeException ex) {
			throw ex;

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}