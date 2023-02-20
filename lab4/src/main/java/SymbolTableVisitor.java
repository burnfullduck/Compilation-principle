

import symtable.*;

import java.util.ArrayList;
import java.util.List;

public class SymbolTableVisitor extends SysYParserBaseVisitor<Integer> {
    private final ArrayList<BaseScope> localScopes = new ArrayList<>();
    private int errorCount = 0;
    private GlobalScope globalScope = null;
    private Scope currentScope = null;
    private int localScopeCounter = 0;

    public int getErrorCount(){
        return errorCount;
    }


    /**
     * (1) When/How to start/enter  exit   a new scope?
     */

    @Override
    public Integer visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        super.visitProgram(ctx);
        currentScope = currentScope.getEnclosingScope();
        return null;
    }


    @Override
    public Integer visitFuncDef(SysYParser.FuncDefContext ctx) {
        String typeName = ctx.funcType().getText();
        globalScope.resolve(typeName);

        String funName = ctx.IDENT().getText();

        if (!currentScope.getSymbol().containsKey(funName)){
            FunctionSymbol fun = new FunctionSymbol(funName, currentScope);
            fun.addPos(ctx.start.getLine(), ctx.IDENT().getSymbol().getCharPositionInLine());

            Type retTy = (Type) globalScope.resolve(ctx.funcType().getText());//将funcType放入FuncSymbol
            ArrayList<Type> fparamsType = new ArrayList<>();

            currentScope.define(fun);
            currentScope = fun;
            localScopes.add(fun);

            if(ctx.funcFParams()!=null){
                for(SysYParser.FuncFParamContext funcFP : ctx.funcFParams().funcFParam()){
                    int v = visit(funcFP);
                    if (v==-1)
                        continue;
                    if (v==0)
                        fparamsType.add(new BasicTypeSymbol("int"));
                    else
                        fparamsType.add(new BasicTypeSymbol("ArrayType"));
                }
            }
            fun.setType(new FunctionType(retTy,fparamsType));



//            if (ctx.funcFParams()!=null&&visit(ctx.funcFParams())==null)
//                return null;

            super.visitBlock(ctx.block()); //就剩下block

            currentScope = currentScope.getEnclosingScope();
        }else {
            errorCount++;
            System.err.println("Error type 4 at Line " + ctx.start.getLine() + ": Redefined function: "+ ctx.IDENT().getText() +".");
        }
        return null;


    }

    @Override
    public Integer visitBlock(SysYParser.BlockContext ctx) {
        LocalScope localScope = new LocalScope(currentScope);
        String localScopeName = localScope.getName() + localScopeCounter;
        localScope.setName(localScopeName);
        localScopeCounter++;
        localScopes.add(localScope);

        currentScope = localScope;
        super.visitBlock(ctx);
        currentScope = currentScope.getEnclosingScope();
        return null;
    }

    public ArrayList<Integer> getRenamePos(int line, int row, String replaceName) {
        if (globalScope.getSymbol().containsKey(replaceName)){
            Symbol symbol = globalScope.resolve(replaceName);
            for(int i=0;i<symbol.getPos().size();i+=2){
                if(symbol.getPos().get(i)==line && symbol.getPos().get(i+1)==row)
                    return symbol.getPos();
            }
        }

        for(BaseScope ls :localScopes){
            if (ls.getSymbol().containsKey(replaceName)){
                Symbol symbol = ls.resolve(replaceName);
                for(int i=0;i<symbol.getPos().size();i+=2){
                    if(symbol.getPos().get(i)==line && symbol.getPos().get(i+1)==row)
                        return symbol.getPos();
                }
            }
        }
        return null;
    }


    /**
     * (3) When to define symbols?   进出时都可以，选择了退出
     */


    @Override
    public Integer visitNumberExp(SysYParser.NumberExpContext ctx) {
        return 0;
    }


    @Override
    public Integer visitVarDecl(SysYParser.VarDeclContext ctx) {
        String typeName = ctx.bType().getText();
        Type type = (Type) globalScope.resolve(typeName);

        List<SysYParser.VarDefContext> varNames = ctx.varDef();

        for (SysYParser.VarDefContext varName : varNames){
            String name = varName.IDENT().getText();
            if(!currentScope.getSymbol().containsKey(name)){
                if(varName.L_BRACKT().size()==0){//若没有左括号为变量定义
                    VariableSymbol varSymbol = new VariableSymbol(varName.IDENT().getText(), type);
                    varSymbol.addPos(varName.IDENT().getSymbol().getLine(),varName.IDENT().getSymbol().getCharPositionInLine());//加位置
                    currentScope.define(varSymbol);
                    if(varName.initVal()!=null){//定义时有赋值
                        if(visit(varName.initVal())!=null){//返回值null说明子节点有问题，跳过; 0说明是int；正数说明数组维数, -1说明为函数
                            int value = visit(varName.initVal());
                            if (value!=0){
                                errorCount++;
                                System.err.println("Error type 5 at Line " + varName.start.getLine() + ": Type mismatched for assignment: 0 "+ value +".");
                            }
                        }
                    }
                }else{//数组定义
                    ArrayList<Integer> counts = new ArrayList<>();
                    for(SysYParser.ConstExpContext cec: varName.constExp()){//本实验用不到
                        visit(cec);
                    }
                    ArrayType varType = new ArrayType(varName.L_BRACKT().size(),type ,counts);//ArrayType
                    VariableSymbol varSymbol = new VariableSymbol(varName.IDENT().getText(), varType);
                    varSymbol.addPos(varName.IDENT().getSymbol().getLine(),varName.IDENT().getSymbol().getCharPositionInLine());

                    if(varName.initVal()!=null){//定义时有赋值
                        if(visit(varName.initVal())!=null){//返回值null说明子节点有问题已解决，跳过; 0说明是int；正数说明数组维数, -1说明为函数
                            int value = visit(varName.initVal());
                            if (value!=varName.L_BRACKT().size()){  //维度不同
                                errorCount++;
                                System.err.println("Error type 5 at Line " + varName.start.getLine() + ": Type mismatched for assignment: "+ varName.L_BRACKT().size() +" "+ value +".");
                            }
                        }
                    }
                    currentScope.define(varSymbol);
                }
            }else{
                errorCount++;
                System.err.println("Error type 3 at Line " + varName.start.getLine() + ": Redefined variable: "+ varName.IDENT().getText() +".");
            }
        }
        return null;
    }

//    @Override
//    public Integer visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
//        for (int i=0;i<ctx.funcFParam().size();i++){
//            if(visit(ctx.funcFParam(i))==null)
//                return null;
//        }
//        return 0;
//    }

    @Override
    public Integer visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        String typeName = ctx.bType().getText();
        Type type = (Type) globalScope.resolve(typeName);

        String varName = ctx.IDENT().getText();
        if(!currentScope.getSymbol().containsKey(varName)){
            if(ctx.L_BRACKT().size()==0){
                VariableSymbol varSymbol = new VariableSymbol(varName, type);
                varSymbol.addPos(ctx.IDENT().getSymbol().getLine(),ctx.IDENT().getSymbol().getCharPositionInLine());
                currentScope.define(varSymbol);
                return 0;
            }else {
                ArrayType varType = new ArrayType(1, type, new ArrayList<Integer>());   //参数不为多维,传入形式为int a[]
                VariableSymbol varSymbol = new VariableSymbol(ctx.IDENT().getText(), varType);
                varSymbol.addPos(ctx.IDENT().getSymbol().getLine(), ctx.IDENT().getSymbol().getCharPositionInLine());
                currentScope.define(varSymbol);
                return 1;
            }
        }
        else{
            errorCount++;
            System.err.println("Error type 3 at Line " + ctx.start.getLine() + ": Redefined variable: " + ctx.IDENT().getText() + ".");
            return -1;//说明有报错
        }
    }

    @Override
    public Integer visitPlusExp(SysYParser.PlusExpContext ctx) {
        if( visit(ctx.exp(0)) == null ){
            visit(ctx.exp(1));
        }else if (visit(ctx.exp(1)) != null){
            if(visit(ctx.exp(0)) != 0 || visit(ctx.exp(1)) != 0){  //加减符号只能用在维度为0的变量上
                errorCount++;
                System.err.println("Error type 6 at Line " + ctx.start.getLine() + ": type.Type mismatched for operands.");
            }else
                return visit(ctx.exp(0));
        }
        return null;
    }

    @Override
    public Integer visitMulExp(SysYParser.MulExpContext ctx) {
        if( visit(ctx.exp(0)) == null ){
            visit(ctx.exp(1));
        }else if (visit(ctx.exp(1)) != null){
            if(visit(ctx.exp(0)) != 0 || visit(ctx.exp(1)) != 0){  //乘除符号只能用在维度为0的变量上，同上
                errorCount++;
                System.err.println("Error type 6 at Line " + ctx.start.getLine() + ": type.Type mismatched for operands.");
            }else
                return visit(ctx.exp(0));
        }
        return null;
    }

    @Override
    public Integer visitArrayInitVal(SysYParser.ArrayInitValContext ctx) {//保证数组初始化没有语义错误
//        for (int i=0;i<ctx.initVal().size();i++){
//            if (visit(ctx.initVal(i))==null)
//                return null;
//            if (visit(ctx.initVal(i))!=0){
//                errorCount++;
//                System.err.println("Error type 5 at Line " + ctx.start.getLine() + ": Type mismatched for assignment: 1 "+ visit(ctx.initVal(i)) +".");
//                return null;
//            }
//        }
        return null;
    }

    @Override
    public Integer visitNumber(SysYParser.NumberContext ctx) {
        return 0;
    }

    @Override
    public Integer visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        if (visit(ctx.exp()) == null){
            return null;
        }else {
            if(visit(ctx.exp()) > 0){ //数组报错
                errorCount++;
                System.err.println("Error type 6 at Line " + ctx.start.getLine() + ": type.Type mismatched for operands.");
                return null;
            }else               //!!!不确定function是否也要报错，
                return visit(ctx.exp());
        }

    }

    @Override
    public Integer visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        int line = ctx.start.getLine();
        int row = ctx.start.getCharPositionInLine();
        Symbol symbol = currentScope.resolve(varName);
        if (symbol==null){
            errorCount++;
            System.err.println("Error type 1 at Line " + line + ": Undefined variable: " + varName + ".");
            return null;
        }else {
            symbol.addPos(line,row);
            int dim;
            if(symbol.getType().toString().equals("int")){  //类型是int
                dim = 0;
                if (dim<ctx.L_BRACKT().size()){
                    errorCount++;
                    System.err.println("Error type 9 at Line "+ line +": Not an array: " + varName + ".");//对变量使用下标运算符
                    return null;
                }
            } else if (symbol.getType().toString().equals("ArrayType")){  //类型是array
                ArrayType at = (ArrayType) symbol.getType();
                dim = at.getDimCount();
                if (dim<ctx.L_BRACKT().size()){   //!!!!!!!!!暂定9       int a[3];    a[2][3]
                    errorCount++;
                    System.err.println("Error type 9 at Line "+ line +": Not an array: " + varName + ".");
                    return null;
                }else
                    dim = dim-ctx.L_BRACKT().size();  //数组降维
            }else{//函数类型
                dim = -1;
                if(ctx.L_BRACKT().size()>0){
                    errorCount++;
                    System.err.println("Error type 9 at Line "+ line +": Not an array: " + varName + ".");//对函数使用下标运算符
                    return null;
                }
            }
            return dim;
        }
    }

    @Override
    public Integer visitCallFuncExp(SysYParser.CallFuncExpContext ctx) {
        String funcName = ctx.IDENT().getText();
        int line = ctx.start.getLine();
        int row = ctx.start.getCharPositionInLine();
        Symbol symbol = currentScope.resolve(funcName);
        if (symbol==null){
            errorCount++;
            System.err.println("Error type 2 at Line " + line + ": Undefined function: " + funcName + ".");
            return null;
        }else {
            if(!symbol.getType().toString().equals("FunctionType")){
                errorCount++;
                System.err.println("Error type 10 at Line "+ctx.start.getLine()+": Not a function: "+symbol.getName()+".");
                return null;
            }
            symbol.addPos(line,row);
            FunctionType ft = (FunctionType) symbol.getType();


            if ((ctx.funcRParams()!=null && ctx.funcRParams().param().size()!=ft.getParamsType().size()) || (ctx.funcRParams()==null && ft.getParamsType().size()>0)){//传入参数数目不对
                errorCount++;
                System.err.println("Error type 8 at Line "+ctx.start.getLine()+": Function is not applicable for arguments.");
                return null;
            }
            for (int i=0;i<ft.getParamsType().size();i++){//int
                if (visit(ctx.funcRParams().param(i))==null)
                    return null;
                if (ft.getParamsType().get(i).toString().equals("int")){
                    if(visit(ctx.funcRParams().param(i))!=0){
                        errorCount++;
                        System.err.println("Error type 8 at Line "+ctx.start.getLine()+": Function is not applicable for arguments.");
                        return null;
                    }
                }
                if (ft.getParamsType().get(i).toString().equals("ArrayType")){//本次实验只会传入一阶数组
                    if(visit(ctx.funcRParams().param(i))!=1){
                        errorCount++;
                        System.err.println("Error type 8 at Line "+ctx.start.getLine()+": Function is not applicable for arguments.");
                        return null;
                    }
                }
            }
            if(((FunctionType) symbol.getType()).getRetTy().toString().equals("int"))
                return 0;
            else {//retype : void   -2
                return -2;
            }
        }
    }

    @Override
    public Integer visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        if(currentScope.getName().equals("GlobalScope"))
            return null;
        if(currentScope.getName().length()>10&&currentScope.getName().substring(0,10).equals("LocalScope")){//在非函数block中有返回值，暂不报错
            return null;
        }
        FunctionSymbol fs = (FunctionSymbol) currentScope;
        if(fs.getType().getRetTy().toString().equals("int")){//函数返回值只有int和void
            if (visit(ctx.exp())==null)
                return null;
            if(visit(ctx.exp())!=0){
                errorCount++;
                System.err.println("Error type 7 at Line "+ctx.start.getLine()+": Type mismatched for return.");
            }
        }
        else if (fs.getType().getRetTy().toString().equals("void")){//返回值为void 的函数不应该有返回值，若有直接报错！！！暂定7
            if(ctx.exp()!=null && visit(ctx.exp())!=null){
                errorCount++;
                System.err.println("Error type 7 at Line "+ctx.start.getLine()+": This function don't need return.");
            }
        }
        return null;
    }

    @Override
    public Integer visitAsignStmt(SysYParser.AsignStmtContext ctx) {
        if(visit(ctx.lVal())==null) {
            return null;
        }
        else if(visit(ctx.exp())!=null){
            if(visit(ctx.lVal())==-1){ //Lval is function ;error 11
                errorCount++;
                System.err.println("Error type 11 at Line "+ctx.start.getLine()+": The left-hand side of an assignment must be a variable.");
                return null;
            }

            if((int)visit(ctx.lVal())!=visit(ctx.exp())){
                errorCount++;
                System.err.println("Error type 5 at Line "+ctx.start.getLine()+": type.Type mismatched for assignment.");
            }
        }
        return null;
    }

    @Override
    public Integer visitLtCond(SysYParser.LtCondContext ctx) {
        if (visit(ctx.cond(0))==null){
            return null;
        }else if(visit(ctx.cond(1))!=null){
            if(visit(ctx.cond(0))!=0||visit(ctx.cond(1))!=0){
                errorCount++;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()+": type.Type mismatched for operands.");
            }else {
                return 0;
            }
        }
        return null;
    }

    @Override
    public Integer visitEqCond(SysYParser.EqCondContext ctx) {
        if (visit(ctx.cond(0))==null){
            return null;
        }else if(visit(ctx.cond(1))!=null){
            if(visit(ctx.cond(0))!=0||visit(ctx.cond(1))!=0){
                errorCount++;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()+": type.Type mismatched for operands.");
            }else {
                return 0;
            }
        }
        return null;
    }

    @Override
    public Integer visitAndCond(SysYParser.AndCondContext ctx) {
        if (visit(ctx.cond(0))==null){
            return null;
        }else if(visit(ctx.cond(1))!=null){
            if(visit(ctx.cond(0))!=0||visit(ctx.cond(1))!=0){
                errorCount++;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()+": type.Type mismatched for operands.");
            }else {
                return 0;
            }
        }
        return null;
    }

    @Override
    public Integer visitOrCond(SysYParser.OrCondContext ctx) {
        if (visit(ctx.cond(0))==null){
            return null;
        }else if(visit(ctx.cond(1))!=null){
            if(visit(ctx.cond(0))!=0||visit(ctx.cond(1))!=0){
                errorCount++;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()+": type.Type mismatched for operands.");
            }else {
                return 0;
            }
        }
        return null;
    }

    @Override
    public Integer visitConstDecl(SysYParser.ConstDeclContext ctx) {
        String typeName = ctx.bType().getText();
        Type type = (Type) globalScope.resolve(typeName);

        for (SysYParser.ConstDefContext varName : ctx.constDef()){
            String name = varName.IDENT().getText();
            if(!currentScope.getSymbol().containsKey(name)){
                if(varName.L_BRACKT().size()==0){//若没有左括号为变量定义
                    VariableSymbol varSymbol = new VariableSymbol(varName.IDENT().getText(), type);
                    varSymbol.addPos(varName.IDENT().getSymbol().getLine(),varName.IDENT().getSymbol().getCharPositionInLine());//加位置
                    currentScope.define(varSymbol);
                        if(visit(varName.constInitVal())!=null){//返回值null说明子节点有问题，跳过; 0说明是int；正数说明数组维数, -1说明为函数
                            int value = visit(varName.constInitVal());
                            if (value!=0){
                                errorCount++;
                                System.err.println("Error type 5 at Line " + varName.start.getLine() + ": Type mismatched for assignment: 0 "+ value +".");
                            }
                        }

                }else{//数组定义
                    ArrayList<Integer> counts = new ArrayList<>();
                    for(SysYParser.ConstExpContext cec: varName.constExp()){
                        counts.add(Integer.parseInt(cec.getText()));
                    }
                    ArrayType varType = new ArrayType(varName.L_BRACKT().size(),type ,counts);//ArrayType
                    VariableSymbol varSymbol = new VariableSymbol(varName.IDENT().getText(), varType);
                    varSymbol.addPos(varName.IDENT().getSymbol().getLine(),varName.IDENT().getSymbol().getCharPositionInLine());
                    currentScope.define(varSymbol);
                        if(visit(varName.constInitVal())!=null){//返回值null说明子节点有问题已解决，跳过; 0说明是int；正数说明数组维数, -1说明为函数
                            int value = visit(varName.constInitVal());
                            if (value!=varName.L_BRACKT().size()){  //维度不同
                                errorCount++;
                                System.err.println("Error type 5 at Line " + varName.start.getLine() + ": Type mismatched for assignment: "+ varName.L_BRACKT().size() +" "+ value +".");
                            }
                        }
                    }
            }else{
                errorCount++;
                System.err.println("Error type 3 at Line " + varName.start.getLine() + ": Redefined variable: "+ varName.IDENT().getText() +".");
            }
        }
        return null;
    }
}
