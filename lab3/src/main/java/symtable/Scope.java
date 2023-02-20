package symtable;

import java.util.Map;

public interface Scope {
    public void setName(String name);
    public Map<String, Symbol> getSymbol();
    public Scope getEnclosingScope();
    public void define(Symbol symbol);
    public String getName();
    public Symbol resolve(String name);
}
