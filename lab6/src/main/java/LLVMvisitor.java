
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMConstInt;

public class LLVMvisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    public static final BytePointer error = new BytePointer();
    private GlobalScope globalScope;
    private Scope currentScope;
    private final LLVMModuleRef module;
    private final LLVMBuilderRef builder;
    private final LLVMTypeRef i32Type = LLVMInt32Type();
    private final LLVMTypeRef voidtype;
    private final LLVMValueRef zero = LLVMConstInt(i32Type,0,0);
    private final LLVMValueRef one = LLVMConstInt(i32Type,1,0);
    private LLVMValueRef func = null;
    private final LLVMTypeRef i1Type = LLVMInt1Type();
    private final LLVMValueRef t1 = LLVMConstInt(i1Type,1,0);
    private final LLVMValueRef f0 = LLVMConstInt(i1Type,0,0);


    public void consoleIR(){
        LLVMDumpModule(module);
    }

    public void getIR(String outputPath){
        if (LLVMPrintModuleToFile(module, outputPath, error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope = new GlobalScope(null);
        currentScope = globalScope;
        super.visitProgram(ctx);
        currentScope = currentScope.getEnclosingScope();
        return null;
    }

    public LLVMvisitor(){
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

        //创建module
        module = LLVMModuleCreateWithName("moudle");

        //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
        builder = LLVMCreateBuilder();

        //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用

        voidtype = LLVMVoidType();

    }


    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        int fparamSize = 0;
        if (ctx.funcFParams()!=null){
            fparamSize = ctx.funcFParams().funcFParam().size();
        }//形参数

        LLVMTypeRef returnType;//返回值
        if (ctx.funcType().getText().equals("int")){
            returnType = i32Type;
        }else
            returnType = voidtype;

        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(fparamSize); //main函数没有参数
        for (int i=0;i<fparamSize;i++){
            argumentTypes.put(i,i32Type);
        }
        //生成函数类型
        LLVMTypeRef ft;
        ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ fparamSize, /* isVariadic */ 0);

        //生成函数，即向之前创建的module中添加函数
        String funcName = ctx.IDENT().getText();
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/funcName, ft);
        func = function;
        currentScope.define(funcName,function);
        currentScope = new BaseScope(funcName,currentScope);


        LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/funcName+"Entry");

        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block1);//后续生成的指令将追加在block1的后面

        for (int i = 0; i < fparamSize; ++i) {
            SysYParser.FuncFParamContext funcFParamContext = ctx.funcFParams().funcFParam(i);
            String varName = funcFParamContext.IDENT().getText();

            LLVMValueRef varPointer = LLVMBuildAlloca(builder, i32Type, "pointer" + varName);//本次实验只有int型参数(不算void)
            currentScope.define(varName, varPointer);
            LLVMValueRef argValue = LLVMGetParam(function, i);
            LLVMBuildStore(builder, argValue, varPointer);
        }

        visit(ctx.block());
        if (returnType == voidtype)
            LLVMBuildRetVoid(builder);  //void没有return语句

        currentScope = currentScope.getEnclosingScope();
        func = null;
        return function;
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        if (currentScope.getName().equals("GlobalScope")){//全局变量
            for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {
                String varName = constDefContext.IDENT().getText();
                SysYParser.ConstExpContext constExpContext = constDefContext.constExp(0);


                if (constExpContext != null) {//数组
                    int initValCount = 0;
                    initValCount = constDefContext.constInitVal().constInitVal().size();

                    int arrayLength = Integer.parseInt(constDefContext.constExp(0).getText());

                    //创建可存放n个int的vector类型
                    LLVMTypeRef arrayType = LLVMArrayType(i32Type, arrayLength);
                    //创建全局变量
                    LLVMValueRef globalArray = LLVMAddGlobal(module, arrayType, /*globalVarName:String*/constDefContext.IDENT().getText());


                    LLVMValueRef[] initArray = new LLVMValueRef[arrayLength];//仅为一维数组
                    // fill in initArray
                    for (int i=0;i<initValCount;i++){
                        initArray[i] = visit(constDefContext.constInitVal().constInitVal(i));//返回值应为
                    }
                    for (int i=initValCount;i<arrayLength;i++){
                        initArray[i] = LLVMConstInt(i32Type,0,0);
                    }

                    PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(initArray);
                    LLVMValueRef constArray = LLVMConstArray(i32Type,indexPointer,arrayLength);
                    LLVMSetInitializer(globalArray, /* constantVal:LLVMValueRef*/constArray);
                    currentScope.define(varName, globalArray);

                }
                else {//变量
                    //创建名为globalVar的全局变量
                    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/constDefContext.IDENT().getText());

                    LLVMValueRef value = visit(constDefContext.constInitVal().constExp());
                        //为全局变量设置初始化器
                    LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/value);

                    currentScope.define(varName, globalVar);

                }

            }
            return null;
        }

        for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {
            String varName = constDefContext.IDENT().getText();
            SysYParser.ConstExpContext constExpContext = constDefContext.constInitVal().constExp();
            if (constExpContext != null) {
                LLVMValueRef varPointer = LLVMBuildAlloca(builder, i32Type, "pointer" + varName);//变量指针
                LLVMValueRef initVal = visit(constExpContext);
                LLVMBuildStore(builder, initVal, varPointer);
                currentScope.define(varName, varPointer);
            }else {
                int initValCount = constDefContext.constInitVal().constInitVal().size();
                int arrayLength = Integer.parseInt(constDefContext.constExp(0).getText());

                //创建可存放n个int的vector类型
                LLVMTypeRef vectorType = LLVMVectorType(i32Type, arrayLength);

                //申请一个可存放该vector类型的内存
                LLVMValueRef vectorPointer = LLVMBuildAlloca(builder, vectorType, "vectorPointor");

                LLVMValueRef[] initArray = new LLVMValueRef[arrayLength];//仅为一维数组
                // fill in initArray
                for (int i=0;i<initValCount;i++){
                    initArray[i] = visit(constDefContext.constInitVal().constInitVal(i));//返回值应为
                }
                for (int i=initValCount;i<arrayLength;i++){
                    initArray[i] = LLVMConstInt(i32Type,0,0);
                }
                buildGEP(arrayLength, vectorPointer, initArray);
                currentScope.define(varName, vectorPointer);
            }
        }
        return null;
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        if (currentScope.getName().equals("GlobalScope")){//全局变量
            for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
                String varName = varDefContext.IDENT().getText();
                SysYParser.ConstExpContext constExpContext = varDefContext.constExp(0);

                if (constExpContext != null) {//数组
                    int initValCount = 0;
                    if (varDefContext.initVal()!=null){
                        initValCount = varDefContext.initVal().initVal().size();
                    }
                    int arrayLength = Integer.parseInt(varDefContext.constExp(0).getText());

                    //创建可存放n个int的vector类型
                    LLVMTypeRef arrayType = LLVMArrayType(i32Type, arrayLength);
                    //创建全局变量
                    LLVMValueRef globalArray = LLVMAddGlobal(module, arrayType, /*globalVarName:String*/varDefContext.IDENT().getText());


                    LLVMValueRef[] initArray = new LLVMValueRef[arrayLength];//仅为一维数组
                    // fill in initArray
                    for (int i=0;i<initValCount;i++){
                        initArray[i] = visit(varDefContext.initVal().initVal(i));//返回值应为
                    }
                    for (int i=initValCount;i<arrayLength;i++){
                        initArray[i] = LLVMConstInt(i32Type,0,0);
                    }

                    PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(initArray);
                    LLVMValueRef constArray = LLVMConstArray(i32Type,indexPointer,arrayLength);
                    LLVMSetInitializer(globalArray, /* constantVal:LLVMValueRef*/constArray);
                    currentScope.define(varName, globalArray);

                }
                else {//变量
                    //创建名为globalVar的全局变量

                    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varDefContext.IDENT().getText());

                    if (varDefContext.ASSIGN() != null){//需初始化
                        LLVMValueRef value = visit(varDefContext.initVal().exp());
                        //为全局变量设置初始化器
                        LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/value);
                    }

                    currentScope.define(varName, globalVar);
                }

            }
            return null;
        }

        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            String varName = varDefContext.IDENT().getText();

            LLVMValueRef varPointer = LLVMBuildAlloca(builder, i32Type, "pointer" + varName);//变量指针

            if (varDefContext.ASSIGN() != null) {
                SysYParser.ExpContext expContext = varDefContext.initVal().exp();
                if (expContext != null) {
                    LLVMValueRef initVal = visit(expContext);
                    LLVMBuildStore(builder, initVal, varPointer);
                } else {
                    int initValCount = varDefContext.initVal().initVal().size();

                    int arrayLength = Integer.parseInt(varDefContext.constExp(0).getText());

                    //创建可存放n个int的vector类型
                    LLVMTypeRef vectorType = LLVMVectorType(i32Type, arrayLength);

                    //申请一个可存放该vector类型的内存
                    LLVMValueRef vectorPointer = LLVMBuildAlloca(builder, vectorType, "vectorPointor");

                    LLVMValueRef[] initArray = new LLVMValueRef[arrayLength];//仅为一维数组
                    // fill in initArray
                    for (int i=0;i<initValCount;i++){
                        initArray[i] = visit(varDefContext.initVal().initVal(i));//返回值应为
                    }
                    for (int i=initValCount;i<arrayLength;i++){
                        initArray[i] = LLVMConstInt(i32Type,0,0);
                    }
                    buildGEP(arrayLength, vectorPointer, initArray);
                    currentScope.define(varName, vectorPointer);
                    continue;
                }
            }

            currentScope.define(varName, varPointer);
        }

        return null;
    }



    private void buildGEP(int elementCount, LLVMValueRef varPointer, LLVMValueRef[] initArray) {
        // 这里展示的是方法的全部代码
        LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
        arrayPointer[0] = LLVMConstInt(i32Type,0, 0);
        for (int i = 0; i < elementCount; i++) {
            arrayPointer[1] = LLVMConstInt(i32Type, i, 0);
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
            LLVMValueRef elementPtr = LLVMBuildGEP(builder, varPointer, indexPointer, 2, "GEP_" + i);
            LLVMBuildStore(builder, initArray[i], elementPtr);
        }
    }

    @Override
    public LLVMValueRef visitLvalExp(SysYParser.LvalExpContext ctx) {

        LLVMValueRef lValPointer = this.visitLVal(ctx.lVal());
//        if (lValPointer==null)
//            return LLVMGetNamedGlobal(module,ctx.lVal().IDENT().getText());
        return LLVMBuildLoad(builder, lValPointer, ctx.lVal().getText());//取数
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        if (ctx.exp().size()==0){//var
            LLVMValueRef re = currentScope.resolve(ctx.IDENT().getText());
//            if (re!=null)
//                return re;
//            else return LLVMGetNamedGlobal(module,ctx.IDENT().getText());
            return re;
        }

        else {                      //array
            LLVMValueRef arrayPtr = currentScope.resolve(ctx.IDENT().getText());
            LLVMValueRef[] arrayPointer = new LLVMValueRef[2];//索引
            arrayPointer[0] = LLVMConstInt(i32Type,0, 0);
            arrayPointer[1] = visit(ctx.exp(0));//只有一维数组
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
            return LLVMBuildGEP(builder, arrayPtr, indexPointer, 2, "array");  //返回的都是指针
        }
    }

    @Override
    public LLVMValueRef visitCallFuncExp(SysYParser.CallFuncExpContext ctx) {
        String functionName = ctx.IDENT().getText();
        LLVMValueRef function = currentScope.resolve(functionName);
        int argCounts = 0;
        PointerPointer<Pointer> args = null;
        if (ctx.funcRParams()!=null){
            argCounts = ctx.funcRParams().param().size();
            LLVMValueRef[] rparms = new LLVMValueRef[argCounts];
            for (int i=0;i<argCounts;i++){
                rparms[i] = visit(ctx.funcRParams().param(i));
            }
            args = new PointerPointer<>(rparms);
        }

        return LLVMBuildCall(builder, function, args, argCounts, "");
    }

    @Override
    public LLVMValueRef visitAsignStmt(SysYParser.AsignStmtContext ctx) {
        LLVMValueRef ptr = visit(ctx.lVal());
        LLVMValueRef value = visit(ctx.exp());
        LLVMBuildStore(builder, value, ptr);
        return null;
    }

    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        if (ctx.exp() == null){
            //void返回在函数定义时已处理
            return null;
        }
        LLVMValueRef result = visit(ctx.exp());

        LLVMBuildRet(builder, /*result:LLVMValueRef*/result);

        return null;
    }

    @Override
    public LLVMValueRef visitNumber(SysYParser.NumberContext ctx) {
        if (ctx.DECIMAL_CONST()!=null)
            return LLVMConstInt(i32Type, Integer.parseInt(ctx.getText()), /* signExtend */ 0);
        else if(ctx.OCTAL_CONST()!=null){//8进制
            int result = Integer.valueOf(ctx.OCTAL_CONST().getText().substring(1), 8);
            return LLVMConstInt(i32Type, result, /* signExtend */ 0);
        }else {//十六进制
            int result = Integer.valueOf(ctx.HEXADECIMAL_CONST().getText().substring(2), 16);
            return LLVMConstInt(i32Type, result, /* signExtend */ 0);
        }
    }

    @Override
    public LLVMValueRef visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        LLVMValueRef expValue = visit(ctx.exp());
        if (ctx.unaryOp().getText().equals("+"))
            return expValue;
        else if (ctx.unaryOp().getText().equals("-")){
            return LLVMBuildNeg(builder, expValue, "neg" );
        }else if (ctx.unaryOp().getText().equals("!")){
            // 生成icmp
            expValue = LLVMBuildICmp(builder, LLVMIntNE, LLVMConstInt(i32Type, 0, 0), expValue, "tmp_");
// 生成xor
            expValue = LLVMBuildXor(builder, expValue, LLVMConstInt(LLVMInt1Type(), 1, 0), "tmp_");
// 生成zext
            expValue = LLVMBuildZExt(builder, expValue, i32Type, "tmp_");
                return expValue;
        }
        return null;
    }

    @Override
    public LLVMValueRef visitExpParenthesis(SysYParser.ExpParenthesisContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitPlusExp(SysYParser.PlusExpContext ctx) {
        LLVMValueRef expValue0 = visit(ctx.exp(0));
        LLVMValueRef expValue1 = visit(ctx.exp(1));
        if (ctx.PLUS()!=null){
            return LLVMBuildAdd(builder,expValue0,expValue1,"add");
        }else{
            return LLVMBuildSub(builder,expValue0,expValue1,"sub");
        }
    }

    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        LLVMValueRef expValue0 = visit(ctx.exp(0));
        LLVMValueRef expValue1 = visit(ctx.exp(1));
        if (ctx.MUL()!=null){
            return LLVMBuildMul(builder,expValue0,expValue1,"mul");
        }else if(ctx.DIV()!=null){
            return LLVMBuildSDiv(builder,expValue0,expValue1,"div");
        }else {
            return LLVMBuildSRem(builder,expValue0,expValue1,"rem");
//            LLVMValueRef a = LLVMBuildSDiv(builder,expValue0,expValue1,"return");
//            LLVMValueRef b = LLVMBuildMul(builder,a,expValue1,"return");
//            return LLVMBuildSub(builder,expValue0,b,"return");
        }
    }

    @Override
    public LLVMValueRef visitIfelseStmt(SysYParser.IfelseStmtContext ctx){
        LLVMValueRef condRe = visit(ctx.cond());
        if (LLVMTypeOf(condRe).equals(i32Type))
            condRe = LLVMBuildICmp(builder,LLVMIntNE,condRe,zero,"condRe");

        LLVMBasicBlockRef tr = LLVMAppendBasicBlock(func, /*blockName:String*/"true");
        LLVMBasicBlockRef fal = LLVMAppendBasicBlock(func, /*blockName:String*/"false");
        LLVMBasicBlockRef entry = LLVMAppendBasicBlock(func, /*blockName:String*/"entry");

        LLVMBuildCondBr(builder,condRe,tr,fal);

        LLVMPositionBuilderAtEnd(builder, tr);//后续生成的指令将追加在 的后面
        visit(ctx.stmt(0));
        LLVMBuildBr(builder,entry);

        LLVMPositionBuilderAtEnd(builder, fal);//后续生成的指令将追加在 的后面
        if (ctx.stmt(1)!=null)
            visit(ctx.stmt(1));
        LLVMBuildBr(builder,entry);

        LLVMPositionBuilderAtEnd(builder, entry);

        return null;
    }

    @Override
    public LLVMValueRef visitAndCond(SysYParser.AndCondContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));
        if (left.equals(zero)||right.equals(zero))
            return t1;
        else
            return f0;
    }

    @Override
    public LLVMValueRef visitOrCond(SysYParser.OrCondContext ctx) {
        LLVMValueRef left = visit(ctx.cond(0));
        LLVMValueRef right = visit(ctx.cond(1));
        if (left.equals(zero)&&right.equals(zero))
            return t1;
        else
            return f0;
    }

    @Override
    public LLVMValueRef visitEqCond(SysYParser.EqCondContext ctx) {
        LLVMValueRef le = visit(ctx.cond(0));
        if (LLVMTypeOf(le).equals(i1Type))
            le = LLVMBuildZExt(builder,le,i32Type,"le");
        LLVMValueRef ri = LLVMConstIntCast(visit(ctx.cond(1)),i32Type,1);
        if (LLVMTypeOf(ri).equals(i1Type))
            ri = LLVMBuildZExt(builder,ri,i32Type,"ri");
        if (ctx.EQ()!=null)
            return LLVMBuildICmp(builder,LLVMIntEQ,le,ri,"EQ");
        else
            return LLVMBuildICmp(builder,LLVMIntNE,le,ri,"NE");
    }

    @Override
    public LLVMValueRef visitLtCond(SysYParser.LtCondContext ctx) {
        LLVMValueRef le = visit(ctx.cond(0));
        if (LLVMTypeOf(le).equals(i1Type))
            le = LLVMBuildZExt(builder,le,i32Type,"le");
        LLVMValueRef ri = LLVMConstIntCast(visit(ctx.cond(1)),i32Type,1);
        if (LLVMTypeOf(ri).equals(i1Type))
            ri = LLVMBuildZExt(builder,ri,i32Type,"ri");

        if (ctx.LE()!=null){
//            if (LLVMBuildICmp(builder,LLVMIntSLE,le,ri,"LE").equals(t1))
//                return ri;
//            else return f0;
            return LLVMBuildICmp(builder,LLVMIntSLE,le,ri,"LE");
        }else if (ctx.LT()!=null){

            return LLVMBuildICmp(builder,LLVMIntSLT,le,ri,"LT");
        }else if (ctx.GT()!=null){
//            if (LLVMBuildICmp(builder,LLVMIntSGT,le,ri,"GE").equals(t1))
//                return ri;
//            else return f0;
            return LLVMBuildICmp(builder,LLVMIntSGT,le,ri,"GT");
        }else {
//            if (LLVMBuildICmp(builder,LLVMIntSGE,le,ri,"GE").equals(t1))
//                return ri;
//            else return f0;
            return LLVMBuildICmp(builder,LLVMIntSGE,le,ri,"GE");
        }
    }

    @Override
    public LLVMValueRef visitExpCond(SysYParser.ExpCondContext ctx) {
        return visit(ctx.exp());//return visit(exp)
    }
}
