import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    private final Scope enclosingScope;
    private final Map<String, LLVMValueRef> LLVMs = new LinkedHashMap<>();
    private String name;

    public BaseScope(String name, Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
        this.name = name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, LLVMValueRef> getLLVM() {
        return LLVMs;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(String name, LLVMValueRef llvmValueRef) {
        LLVMs.put(name,llvmValueRef);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LLVMValueRef resolve(String name) {
        LLVMValueRef result = LLVMs.get(name);
        if(result != null){
            //System.out.println("*" + name);
            return result;
        }
        if(enclosingScope != null){
            return enclosingScope.resolve(name);
        }
        return null;
    }
}
