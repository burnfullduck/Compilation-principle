package symtable;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope{
    private final Scope enclosingScope;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private final Map<String, Symbol> lines = new LinkedHashMap<>();
    private final Map<String, Symbol> rows = new LinkedHashMap<>();
    private String name;

    public BaseScope(String name, Scope enclosingScope){
        this.name = name;
        this.enclosingScope = enclosingScope;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, Symbol> getSymbol() {
        return this.symbols;
    }

    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }

    @Override
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
        //System.out.println("+" + symbol.getName());
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol = symbols.get(name);
        if(symbol != null){
            //System.out.println("*" + name);
            return symbol;
        }
        if(enclosingScope != null){
            return enclosingScope.resolve(name);
        }
        return null;
    }
}
