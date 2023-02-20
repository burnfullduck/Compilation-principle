package symtable;

import java.util.ArrayList;
import java.util.List;

public interface Symbol {
    public String getName();
    public Type getType();
    public void addPos(int line, int row);
    public ArrayList<Integer> getPos();
}
