package br.com.murilo.liberthia.item.script;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-pass lexer + parser + bytecode emitter for LiberScript.
 *
 * Grammar (line-oriented; statements separated by newlines or {@code ;}):
 * <pre>
 *   program  := stmt*
 *   stmt     := 'let' IDENT '=' expr
 *             | 'if' expr block ('else' block)? 'end'
 *             | 'while' expr block 'end'
 *             | 'repeat' expr block 'end'
 *             | 'wait' expr
 *             | 'say' &lt;rest-of-line&gt;
 *             | 'run' &lt;rest-of-line&gt;
 *   block    := stmt*
 *   expr     := orExpr
 *   orExpr   := andExpr ('or' andExpr)*
 *   andExpr  := cmpExpr ('and' cmpExpr)*
 *   cmpExpr  := addExpr (('=='|'!='|'&lt;'|'&lt;='|'&gt;'|'&gt;=') addExpr)*
 *   addExpr  := mulExpr (('+'|'-') mulExpr)*
 *   mulExpr  := unary (('*'|'/'|'%') unary)*
 *   unary    := ('-'|'not') unary | primary
 *   primary  := NUMBER | STRING | 'true'|'false' | IDENT | '(' expr ')'
 * </pre>
 *
 * {@code say} and {@code run} consume the rest of their physical line as a
 * raw template (with {@code {var}} interpolation done at runtime).
 */
public final class ScriptCompiler {

    public static List<Insn> compile(String source) {
        ScriptCompiler c = new ScriptCompiler(source);
        c.lex();
        c.parseProgram();
        c.code.add(new Insn(Insn.Op.HALT));
        return c.code;
    }

    // ---------------------------------------------------------------- lexer

    private static final class Tok {
        final String kind;   // NUM, STR, IDENT, KW, OP, SEP, LINE_REST, EOF
        final String text;
        final int line;
        Tok(String k, String t, int l) { kind = k; text = t; line = l; }
        @Override public String toString() { return kind + "(" + text + ")"; }
    }

    private final String src;
    private final List<Tok> tokens = new ArrayList<>();
    private int p; // lex cursor
    private int line = 1;
    private int t; // token cursor
    private final List<Insn> code = new ArrayList<>();

    private ScriptCompiler(String source) {
        this.src = source == null ? "" : source;
    }

    private static final java.util.Set<String> KEYWORDS = java.util.Set.of(
            "let", "if", "else", "end", "while", "repeat",
            "wait", "say", "run",
            "true", "false", "and", "or", "not");

    private void lex() {
        while (p < src.length()) {
            char c = src.charAt(p);

            if (c == '\n') { tokens.add(new Tok("SEP", "\n", line)); line++; p++; continue; }
            if (Character.isWhitespace(c)) { p++; continue; }

            if (c == '#') { // comment to end of line
                while (p < src.length() && src.charAt(p) != '\n') p++;
                continue;
            }
            if (c == ';') { tokens.add(new Tok("SEP", ";", line)); p++; continue; }

            if (Character.isDigit(c)) { lexNumber(); continue; }
            if (c == '"') { lexString(); continue; }
            if (Character.isLetter(c) || c == '_') { lexIdentOrKeyword(); continue; }

            // Multi-char ops
            if (matches("==")) { tokens.add(new Tok("OP", "==", line)); p += 2; continue; }
            if (matches("!=")) { tokens.add(new Tok("OP", "!=", line)); p += 2; continue; }
            if (matches("<=")) { tokens.add(new Tok("OP", "<=", line)); p += 2; continue; }
            if (matches(">=")) { tokens.add(new Tok("OP", ">=", line)); p += 2; continue; }

            switch (c) {
                case '+': case '-': case '*': case '/': case '%':
                case '<': case '>': case '=': case '(': case ')': case ',':
                    tokens.add(new Tok("OP", String.valueOf(c), line)); p++; break;
                default:
                    throw err("Caractere inesperado: '" + c + "'");
            }
        }
        tokens.add(new Tok("EOF", "", line));
    }

    private boolean matches(String s) {
        return p + s.length() <= src.length() && src.substring(p, p + s.length()).equals(s);
    }

    private void lexNumber() {
        int start = p;
        while (p < src.length() && (Character.isDigit(src.charAt(p)) || src.charAt(p) == '.')) p++;
        tokens.add(new Tok("NUM", src.substring(start, p), line));
    }

    private void lexString() {
        p++; // skip opening "
        StringBuilder sb = new StringBuilder();
        while (p < src.length() && src.charAt(p) != '"') {
            char ch = src.charAt(p);
            if (ch == '\\' && p + 1 < src.length()) {
                char nx = src.charAt(p + 1);
                switch (nx) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(nx);
                }
                p += 2;
            } else {
                sb.append(ch);
                p++;
            }
        }
        if (p >= src.length()) throw err("String não fechada");
        p++; // skip closing "
        tokens.add(new Tok("STR", sb.toString(), line));
    }

    private void lexIdentOrKeyword() {
        int start = p;
        while (p < src.length()
                && (Character.isLetterOrDigit(src.charAt(p)) || src.charAt(p) == '_')) p++;
        String word = src.substring(start, p);
        if (KEYWORDS.contains(word)) {
            // For say/run we ALSO grab the rest of this line as a separate LINE_REST token.
            tokens.add(new Tok("KW", word, line));
            if (word.equals("say") || word.equals("run")) {
                // skip leading spaces (but not newlines)
                while (p < src.length() && (src.charAt(p) == ' ' || src.charAt(p) == '\t')) p++;
                int lstart = p;
                while (p < src.length() && src.charAt(p) != '\n') p++;
                tokens.add(new Tok("LINE_REST", src.substring(lstart, p), line));
            }
        } else {
            tokens.add(new Tok("IDENT", word, line));
        }
    }

    // ---------------------------------------------------------------- parser

    private Tok peek() { return tokens.get(t); }
    private Tok advance() { return tokens.get(t++); }
    private boolean check(String kind) { return peek().kind.equals(kind); }
    private boolean checkKw(String kw) { return check("KW") && peek().text.equals(kw); }
    private boolean checkOp(String op) { return check("OP") && peek().text.equals(op); }

    private boolean matchKw(String kw) {
        if (checkKw(kw)) { advance(); return true; }
        return false;
    }
    private boolean matchOp(String op) {
        if (checkOp(op)) { advance(); return true; }
        return false;
    }

    private void skipSeparators() {
        while (check("SEP")) advance();
    }

    private void expect(String kind, String text) {
        if (!check(kind) || (text != null && !peek().text.equals(text))) {
            throw err("Esperado '" + (text == null ? kind : text) + "' mas veio '" + peek().text + "'");
        }
        advance();
    }

    private void parseProgram() {
        skipSeparators();
        while (!check("EOF")) {
            parseStmt();
            skipSeparators();
        }
    }

    private void parseStmt() {
        if (matchKw("let")) {
            if (!check("IDENT")) throw err("Esperado nome de variável após 'let'");
            String name = advance().text;
            expect("OP", "=");
            parseExpr();
            emit(Insn.Op.STORE, name);
        } else if (matchKw("if")) {
            parseExpr();
            int jfalse = emitJump(Insn.Op.JFALSE);
            parseBlockUntil("else", "end");
            int jend = -1;
            if (matchKw("else")) {
                jend = emitJump(Insn.Op.JMP);
                patchJump(jfalse, code.size());
                parseBlockUntil("end");
            } else {
                patchJump(jfalse, code.size());
            }
            expectKw("end");
            if (jend != -1) patchJump(jend, code.size());
        } else if (matchKw("while")) {
            int loopStart = code.size();
            parseExpr();
            int jfalse = emitJump(Insn.Op.JFALSE);
            parseBlockUntil("end");
            expectKw("end");
            int jback = code.size();
            code.add(new Insn(Insn.Op.JMP, loopStart));
            patchJump(jfalse, code.size());
            // Note: the JMP we just emitted already targets loopStart, fine.
        } else if (matchKw("repeat")) {
            // counter held in a fresh hidden var
            String counter = "__r" + System.identityHashCode(peek()) + "_" + code.size();
            parseExpr();
            emit(Insn.Op.STORE, counter);
            int loopStart = code.size();
            emit(Insn.Op.LOAD, counter);
            emit(Insn.Op.PUSH, 0d);
            emit(Insn.Op.GT);
            int jfalse = emitJump(Insn.Op.JFALSE);
            parseBlockUntil("end");
            expectKw("end");
            emit(Insn.Op.LOAD, counter);
            emit(Insn.Op.PUSH, 1d);
            emit(Insn.Op.SUB);
            emit(Insn.Op.STORE, counter);
            code.add(new Insn(Insn.Op.JMP, loopStart));
            patchJump(jfalse, code.size());
        } else if (matchKw("wait")) {
            parseExpr();
            emit(Insn.Op.WAIT);
        } else if (matchKw("say")) {
            String tpl = check("LINE_REST") ? advance().text : "";
            emit(Insn.Op.SAY, tpl);
        } else if (matchKw("run")) {
            String tpl = check("LINE_REST") ? advance().text : "";
            emit(Insn.Op.RUN, tpl);
        } else {
            throw err("Statement desconhecido: '" + peek().text + "'");
        }
    }

    private void parseBlockUntil(String... terminators) {
        skipSeparators();
        java.util.Set<String> stops = java.util.Set.of(terminators);
        while (!check("EOF") && !(check("KW") && stops.contains(peek().text))) {
            parseStmt();
            skipSeparators();
        }
    }

    private void expectKw(String kw) {
        if (!matchKw(kw)) throw err("Esperado '" + kw + "'");
    }

    // ---------------------------------------------------------------- expressions

    private void parseExpr() { parseOr(); }

    private void parseOr() {
        parseAnd();
        while (matchKw("or")) { parseAnd(); emit(Insn.Op.OR); }
    }

    private void parseAnd() {
        parseCmp();
        while (matchKw("and")) { parseCmp(); emit(Insn.Op.AND); }
    }

    private void parseCmp() {
        parseAdd();
        while (true) {
            if (matchOp("==")) { parseAdd(); emit(Insn.Op.EQ); }
            else if (matchOp("!=")) { parseAdd(); emit(Insn.Op.NEQ); }
            else if (matchOp("<=")) { parseAdd(); emit(Insn.Op.LE); }
            else if (matchOp(">=")) { parseAdd(); emit(Insn.Op.GE); }
            else if (matchOp("<")) { parseAdd(); emit(Insn.Op.LT); }
            else if (matchOp(">")) { parseAdd(); emit(Insn.Op.GT); }
            else break;
        }
    }

    private void parseAdd() {
        parseMul();
        while (true) {
            if (matchOp("+")) { parseMul(); emit(Insn.Op.ADD); }
            else if (matchOp("-")) { parseMul(); emit(Insn.Op.SUB); }
            else break;
        }
    }

    private void parseMul() {
        parseUnary();
        while (true) {
            if (matchOp("*")) { parseUnary(); emit(Insn.Op.MUL); }
            else if (matchOp("/")) { parseUnary(); emit(Insn.Op.DIV); }
            else if (matchOp("%")) { parseUnary(); emit(Insn.Op.MOD); }
            else break;
        }
    }

    private void parseUnary() {
        if (matchOp("-")) { parseUnary(); emit(Insn.Op.NEG); return; }
        if (matchKw("not")) { parseUnary(); emit(Insn.Op.NOT); return; }
        parsePrimary();
    }

    private void parsePrimary() {
        Tok tk = peek();
        if (tk.kind.equals("NUM")) {
            advance();
            emit(Insn.Op.PUSH, Double.parseDouble(tk.text));
        } else if (tk.kind.equals("STR")) {
            advance();
            emit(Insn.Op.PUSH, tk.text);
        } else if (tk.kind.equals("KW") && tk.text.equals("true")) {
            advance(); emit(Insn.Op.PUSH, Boolean.TRUE);
        } else if (tk.kind.equals("KW") && tk.text.equals("false")) {
            advance(); emit(Insn.Op.PUSH, Boolean.FALSE);
        } else if (tk.kind.equals("IDENT")) {
            advance();
            emit(Insn.Op.LOAD, tk.text);
        } else if (tk.kind.equals("OP") && tk.text.equals("(")) {
            advance();
            parseExpr();
            expect("OP", ")");
        } else {
            throw err("Expressão inválida em '" + tk.text + "'");
        }
    }

    // ---------------------------------------------------------------- emit

    private void emit(Insn.Op op) { code.add(new Insn(op)); }
    private void emit(Insn.Op op, Object payload) { code.add(new Insn(op, payload)); }

    private int emitJump(Insn.Op op) {
        Insn ins = new Insn(op, -1);
        code.add(ins);
        return code.size() - 1;
    }

    private void patchJump(int idx, int target) {
        code.get(idx).jumpTarget = target;
    }

    private RuntimeException err(String msg) {
        int ln = (t < tokens.size() ? tokens.get(t).line : line);
        return new RuntimeException("[linha " + ln + "] " + msg);
    }
}
