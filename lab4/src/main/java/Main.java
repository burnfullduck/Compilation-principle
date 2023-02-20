import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String replaceName = "";
        List<Integer> replacePos;

        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);


        sysYParser.removeErrorListeners();
//        MySysYParserErrorListener listener = new MySysYParserErrorListener();
//        sysYParser.addErrorListener(listener);

        ParseTree tree = sysYParser.program();

        LLVMvisitor llvMvisitor = new LLVMvisitor();
        llvMvisitor.visit(tree);
        llvMvisitor.consoleIR();

//        Visitor visitor = new Visitor();
//        visitor.setRenamePos(args[3], pos);
//        if (stVisitor.getErrorCount()!=0)
//            return;
//        visitor.visit(tree);

    }
}
