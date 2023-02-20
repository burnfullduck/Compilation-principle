package symtable;

import java.util.ArrayList;
import java.util.List;

public class FunctionSymbol extends BaseScope implements Symbol{
    private final ArrayList<Integer> postion = new ArrayList<>();
    private FunctionType type;

    @Override
    public FunctionType getType() {
        return type;
    }

    public void addPos(int line, int row){
        postion.add(line);
        postion.add(row);
    }

    public ArrayList<Integer> getPos(){
        return postion;
    }

    public FunctionSymbol(String name, Scope enclosingScope) {
        super(name, enclosingScope);
    }
    public void setType(FunctionType type){
        this.type=type;
    }
}
