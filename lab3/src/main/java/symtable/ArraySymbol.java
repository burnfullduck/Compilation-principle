package symtable;

public class ArraySymbol extends BaseSymbol{

    public ArraySymbol(String name, ArrayType type) {
        super(name, type);
    }

    @Override
    public ArrayType getType() {
        return (ArrayType) type;
    }
}
