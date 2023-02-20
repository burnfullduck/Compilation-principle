
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.Map;

interface Scope {
    public void setName(String name);
    public Map<String, LLVMValueRef> getLLVM();
    public Scope getEnclosingScope();
    public void define(String name, LLVMValueRef llvmValueRef);
    public String getName();
    public LLVMValueRef resolve(String name);
}