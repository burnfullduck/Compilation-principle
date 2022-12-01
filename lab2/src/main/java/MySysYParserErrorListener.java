import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class MySysYParserErrorListener extends BaseErrorListener {
    public int haserror=0;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        haserror=1;
        System.err.println("Error type B at Line "+ line + ": " + msg);
    }
}
