package symtable;

import java.util.ArrayList;

public class FunctionType implements Type{
    Type retTy;
    ArrayList<Type> paramsType;

    public FunctionType(Type retTy, ArrayList<Type> paramsType){
        this.retTy = retTy;
        this.paramsType = paramsType;
    }
    public String toString(){
        return "FunctionType";
    }

    public Type getRetTy() {
        return retTy;
    }

    public ArrayList<Type> getParamsType() {
        return paramsType;
    }
}
