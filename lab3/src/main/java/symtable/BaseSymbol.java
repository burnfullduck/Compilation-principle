package symtable;

import java.util.ArrayList;
import java.util.List;

public class BaseSymbol implements Symbol{
    final String name;
    final Type type;
    private final ArrayList<Integer> postion = new ArrayList<>();

    public BaseSymbol(String name, Type type){
        this.name = name;
        this.type = type;
    }

    public void addPos(int line, int row){
        postion.add(line);
        postion.add(row);
    }

    public ArrayList<Integer> getPos(){
        return postion;
    }

    @Override
    public String getName() {
        return name;
    }
    public Type getType(){
        return type;
    }

    @Override
    public String toString() {
        return "BaseSymbol{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
