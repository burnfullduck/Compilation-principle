import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        sysYParser.removeErrorListeners();
        MySysYParserErrorListener listener = new MySysYParserErrorListener();
        sysYParser.addErrorListener(listener);

        ParseTree tree = sysYParser.program();
        //Visitor extends SysYParserBaseVisitor<Void>
        Visitor visitor = new Visitor();
        if (listener.haserror == 1)
            return;
        visitor.visit(tree);

    }
}
