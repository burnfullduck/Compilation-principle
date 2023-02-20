
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import symtable.*;

import java.util.ArrayList;

public class Visitor extends SysYParserBaseVisitor<Void> {
    private ArrayList<Integer> pos;
    private final String[] colors = new String[] {
        "[orange]", "[orange]", "[orange]", "[orange]", "[orange]", "[orange]", "[orange]", "[orange]", "[orange]",
                "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "[blue]",
                "[blue]", "[blue]", "[blue]", "[blue]", "[blue]", "L_PAREN", "R_PAREN", "L_BRACE", "R_BRACE",
                "L_BRACKT", "R_BRACKT", "COMMA", "SEMICOLON", "[red]", "[green]",
                "DECIMAL_CONST", "OCTAL_CONST", "HEXADECIMAL_CONST", "WS", "LINE_COMMENT",
                "MULTILINE_COMMENT"
    };

    private String blanks = "";
    private String rename;
    private String needRe;


    public void setRenamePos(String rename, ArrayList<Integer> pos){
        this.pos = pos;
        this.rename = rename;
    }

    public boolean needReplace(int line, int row){
        for(int i=0;i<pos.size();i+=2){
            if(pos.get(i)==line && pos.get(i+1)==row)
                return true;
        }
        return false;
    }


    @Override
    public Void visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType()<0)
            return super.visitTerminal(node);
        if(!needSkip(node)) {
            String result;
            int num;
            if(node.getText().length() >= 2 && node.getSymbol().getType()==34&&node.getText().charAt(0) == '0' ) {
                if (node.getText().charAt(1) == 'x' || node.getText().charAt(1) == 'X') {
                    num = Integer.valueOf(node.getText().substring(2), 16);
                } else {
                    num = Integer.valueOf(node.getText().substring(1), 8);
                }
                result = String.valueOf(num);
            }else {//重命名
                if(node.getSymbol().getType()==33 && needReplace(node.getSymbol().getLine(),node.getSymbol().getCharPositionInLine()))// && currentScope.resolve())
                    result = rename;
                else
                    result = node.getText();
            }
            result = result + " " + SysYLexer.ruleNames[node.getSymbol().getType()-1];
            System.err.println(blanks + result + colors[node.getSymbol().getType()-1]);
        }
        return super.visitTerminal(node);
    }

    @Override
    public Void visitChildren(RuleNode node) {
        int num = node.getRuleContext().getRuleIndex();
        String ruleName = UpperFirstLetter(SysYParser.ruleNames[num]);
        System.err.println(blanks + ruleName );
        blanks=blanks+"  ";
        //
        Void result = this.defaultResult();
        int n = node.getChildCount();

        for(int i = 0; i < n && this.shouldVisitNextChild(node, result); ++i) {
            ParseTree c = node.getChild(i);
            Void childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }
        //
        if(blanks.length()>=2)
            blanks = blanks.substring(2);
        return result;
    }

    private String UpperFirstLetter(String str){
        if (str.length()==0)
            return str;
        char firstLetter = (char)(str.charAt(0)-32);
        str = firstLetter + str.substring(1);
        return str;
    }

    private boolean needSkip(TerminalNode node){
        return SysYLexer.ruleNames[node.getSymbol().getType()-1].equals(colors[node.getSymbol().getType()-1]) ;
    }

}