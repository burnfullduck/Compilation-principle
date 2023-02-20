
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMConstInt;

public class LLVMvisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    public static final BytePointer error = new BytePointer();
    LLVMModuleRef module;
    LLVMBuilderRef builder;
    LLVMTypeRef i32Type;


    public void consoleIR(){
        LLVMDumpModule(module);
    }

    public void getIR(String outputPath){
        if (LLVMPrintModuleToFile(module, outputPath, error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }
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
        i32Type = LLVMInt32Type();

    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //生成返回值类型
        LLVMTypeRef returnType = null;  //返回值为void的话为null
        if (ctx.funcType().getText().equals("int")){
            returnType = i32Type;
        }


        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(0); //main函数没有参数

        //生成函数类型
        LLVMTypeRef ft;
        //若仅需一个参数也可以使用如下方式直接生成函数类型
        ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 0, /* isVariadic */ 0);

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/ctx.IDENT().getText(), ft);

        LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/"mainEntry");

        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block1);//后续生成的指令将追加在block1的后面


        return super.visitFuncDef(ctx);
    }

    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        LLVMValueRef result = visit(ctx.exp());

        LLVMBuildRet(builder, /*result:LLVMValueRef*/result);

        return super.visitReturnStmt(ctx);
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
            return LLVMBuildNeg(builder, expValue, "return" );
        }else if (ctx.unaryOp().getText().equals("!")){
//            LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
//            LLVMValueRef condition = LLVMBuildICmp(builder, /*这是个int型常量，表示比较的方式*/LLVMIntEQ, expValue, zero, "condition = n == 0");
//            return condition;
            if (expValue.equals(LLVMConstInt(i32Type, 0, /* signExtend */ 0)))
                return LLVMConstInt(i32Type, 1, /* signExtend */ 0);
            else
                return LLVMConstInt(i32Type, 0, /* signExtend */ 0);
        }
        return super.visitUnaryOpExp(ctx);
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
            return LLVMBuildAdd(builder,expValue0,expValue1,"return");
        }else{
            return LLVMBuildSub(builder,expValue0,expValue1,"return");
        }
    }

    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        LLVMValueRef expValue0 = visit(ctx.exp(0));
        LLVMValueRef expValue1 = visit(ctx.exp(1));
        if (ctx.MUL()!=null){
            return LLVMBuildMul(builder,expValue0,expValue1,"return");
        }else if(ctx.DIV()!=null){
            return LLVMBuildSDiv(builder,expValue0,expValue1,"return");
        }else {
            return LLVMBuildSRem(builder,expValue0,expValue1,"return");
//            LLVMValueRef a = LLVMBuildSDiv(builder,expValue0,expValue1,"return");
//            LLVMValueRef b = LLVMBuildMul(builder,a,expValue1,"return");
//            return LLVMBuildSub(builder,expValue0,b,"return");
        }
    }
}
