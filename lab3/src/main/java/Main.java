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

        if (args.length < 4) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);

        for (Token t:new SysYLexer(CharStreams.fromFileName(source)).getAllTokens()){
            if(t.getLine()==Integer.parseInt(args[1])&&t.getCharPositionInLine()==Integer.parseInt(args[2])){
                replaceName = t.getText();
            }
        }

        sysYParser.removeErrorListeners();
//        MySysYParserErrorListener listener = new MySysYParserErrorListener();
//        sysYParser.addErrorListener(listener);

        ParseTree tree = sysYParser.program();
        //SysY.Visitor extends SysY.SysYParserBaseVisitor<Void>
        SymbolTableVisitor stVisitor = new SymbolTableVisitor();//遍历生成符号表
        stVisitor.visit(tree);

        ArrayList<Integer> pos = stVisitor.getRenamePos(Integer.parseInt(args[1]),Integer.parseInt(args[2]),replaceName);

        Visitor visitor = new Visitor();
        visitor.setRenamePos(args[3], pos);
        if (stVisitor.getErrorCount()!=0)
            return;
        visitor.visit(tree);

    }
}
