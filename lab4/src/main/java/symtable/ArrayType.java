package symtable;

import java.util.ArrayList;
import java.util.List;

public class ArrayType implements Type{
    private final int dimCount;
    private final ArrayList<Integer> counts;
    private final Type subType;

    public ArrayType(int dimCount, Type subType, ArrayList<Integer> counts) {
        this.dimCount=dimCount;
        this.subType = subType;
        this.counts = counts;
    }

    public int getDimCount() {
        return dimCount;
    }

    public Type getSubType() {
        return subType;
    }

    public ArrayList<Integer> getCounts() {
        return counts;
    }

    @Override
    public String toString() {
        return "ArrayType";
    }
}
