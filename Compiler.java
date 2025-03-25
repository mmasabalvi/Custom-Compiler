
import java.io.*;
import java.util.*;

//------------------------- Token Class -------------------------
class Token {
    String type;    // e.g., KEYWORD, IDENTIFIER, NUMBER, OPERATOR, LBRACE, RBRACE, SEMICOLON, etc.
    String lexeme;  // The actual string value
    int line;       // Line number where the token was found

    public Token(String type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + ":" + lexeme + " (Line " + line + ")";
    }
}

//------------------------- DFA / Automaton -------------------------
class Automaton {
    private Set<String> states = new HashSet<>();
    private Map<String, Map<Character, String>> transitions = new HashMap<>();
    private String startState;
    private Set<String> acceptStates = new HashSet<>();

    public void addState(String state, boolean isAccept) {
        states.add(state);
        if (isAccept) {
            acceptStates.add(state);
        }
    }

    public void setStartState(String state) {
        startState = state;
    }

    public void addTransition(String from, char input, String to) {
        transitions.putIfAbsent(from, new HashMap<>());
        transitions.get(from).put(input, to);
    }

    // Returns the length of the longest prefix (from 'start') accepted by this DFA.
    public int getLongestAcceptedLength(String input, int start) {
        String state = startState;
        int longestAccepted = -1;
        int length = 0;
        if (acceptStates.contains(state)) {
            longestAccepted = 0;
        }
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            Map<Character, String> stateTrans = transitions.get(state);
            if (stateTrans == null || !stateTrans.containsKey(c)) {
                break;
            }
            state = stateTrans.get(c);
            length++;
            if (acceptStates.contains(state)) {
                longestAccepted = length;
            }
        }
        return longestAccepted >= 0 ? longestAccepted : 0;
    }

    public void displayTransitionTable() {
        System.out.println("Total States: " + states.size());
        System.out.println("States: " + states);
        System.out.println("Accepting States: " + acceptStates);
        System.out.println("Transitions:");
        for (String state : transitions.keySet()) {
            System.out.println("  " + state + " -> " + transitions.get(state));
        }
    }
}

//------------------------- Lexer -------------------------
class Lexer {
    private String input;
    private int pos = 0;
    private int line = 1;
    private final ErrorHandler errorHandler;
    // DFAs for identifiers, numbers, and operators.
    final Automaton identifierDFA = new Automaton();
    final Automaton numberDFA = new Automaton();
    final Automaton operatorDFA = new Automaton();

    // Reserved keywords.
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "if", "else", "while", "for",
        "int", "decimal", "bool", "char", "string",
        "true", "false", "input", "output"
    ));

    public Lexer(String input, ErrorHandler errorHandler) {
        this.input = input;
        this.errorHandler = errorHandler;
        initializeDFAs();
    }

    private void initializeDFAs() {
        // Identifier DFA: accepts one or more lowercase letters.
        identifierDFA.setStartState("q0");
        identifierDFA.addState("q0", false);
        identifierDFA.addState("q1", true);
        for (char c = 'a'; c <= 'z'; c++) {
            identifierDFA.addTransition("q0", c, "q1");
            identifierDFA.addTransition("q1", c, "q1");
        }

        // Number DFA: supports integer, decimal (up to 5 decimal places), and exponent notation.
        numberDFA.setStartState("q0");
        numberDFA.addState("q0", false);
        numberDFA.addState("q1", true);  // integer part
        numberDFA.addState("q2", false); // decimal point seen
        numberDFA.addState("q3", true);  // fractional part
        numberDFA.addState("q4", false); // exponent symbol seen
        numberDFA.addState("q5", false); // exponent sign seen
        numberDFA.addState("q6", true);  // exponent digits

        for (char c = '0'; c <= '9'; c++) {
            numberDFA.addTransition("q0", c, "q1");
            numberDFA.addTransition("q1", c, "q1");
            numberDFA.addTransition("q2", c, "q3");
            numberDFA.addTransition("q3", c, "q3");
            numberDFA.addTransition("q4", c, "q6");
            numberDFA.addTransition("q5", c, "q6");
            numberDFA.addTransition("q6", c, "q6");
        }
        numberDFA.addTransition("q1", '.', "q2");
        numberDFA.addTransition("q1", 'E', "q4");
        numberDFA.addTransition("q3", 'E', "q4");
        numberDFA.addTransition("q4", '+', "q5");
        numberDFA.addTransition("q4", '-', "q5");

        // Operator DFA: supports +, -, *, /, %, and two-character operators.
        operatorDFA.setStartState("q0");
        operatorDFA.addState("q0", false);
        operatorDFA.addState("q1", true);
        operatorDFA.addState("q2", true);
        for (char op : new char[]{'+', '-', '*', '/', '%', '<', '>', '=', '!'}) {
            operatorDFA.addTransition("q0", op, "q1");
        }
        operatorDFA.addTransition("q1", '=', "q2");
    }

    // Returns a list of Token objects.
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char currentChar = input.charAt(pos);

            // Skip whitespace.
            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') {
                    line++;
                }
                pos++;
                continue;
            }

            // Scope punctuation.
            if (currentChar == '{') {
                tokens.add(new Token("LBRACE", "{", line));
                pos++;
                continue;
            }
            if (currentChar == '}') {
                tokens.add(new Token("RBRACE", "}", line));
                pos++;
                continue;
            }
            if (currentChar == ';') {
                tokens.add(new Token("SEMICOLON", ";", line));
                pos++;
                continue;
            }

            // Comments.
            if (currentChar == '/') {
                if (pos + 1 < input.length()) {
                    char nextChar = input.charAt(pos + 1);
                    if (nextChar == '/' || nextChar == '*') {
                        handleComments();
                        continue;
                    }
                }
            }

            // String literal.
            if (currentChar == '"') {
                String strLiteral = readStringLiteral();
                tokens.add(new Token("STRING", strLiteral, line));
                continue;
            }

            // Character literal.
            if (currentChar == '\'') {
                String charLiteral = readCharLiteral();
                tokens.add(new Token("CHAR", charLiteral, line));
                continue;
            }

            // Identifiers and keywords.
            if (Character.isLetter(currentChar)) {
                int len = identifierDFA.getLongestAcceptedLength(input, pos);
                if (len > 0) {
                    String lexeme = input.substring(pos, pos + len);
                    int tokenLine = line;
                    pos += len;
                    if (KEYWORDS.contains(lexeme)) {
                        tokens.add(new Token("KEYWORD", lexeme, tokenLine));
                    } else {
                        tokens.add(new Token("IDENTIFIER", lexeme, tokenLine));
                    }
                    continue;
                }
            }

            // Numbers.
            if (Character.isDigit(currentChar)) {
                int len = numberDFA.getLongestAcceptedLength(input, pos);
                if (len > 0) {
                    String lexeme = input.substring(pos, pos + len);
                    int tokenLine = line;
                    pos += len;
                    validateNumber(lexeme);
                    tokens.add(new Token("NUMBER", lexeme, tokenLine));
                    continue;
                }
            }

            // Operators.
            if ("=<>!+-*/%^".indexOf(currentChar) != -1) {
                int len = operatorDFA.getLongestAcceptedLength(input, pos);
                if (len > 0) {
                    String lexeme = input.substring(pos, pos + len);
                    int tokenLine = line;
                    pos += len;
                    tokens.add(new Token("OPERATOR", lexeme, tokenLine));
                    continue;
                }
            }

            errorHandler.reportError(line, "Invalid character: '" + currentChar + "'");
            pos++;
        }
        tokens.add(new Token("EOF", "", line));
        System.out.println("Total tokens: " + tokens.size());
        return tokens;
    }

    private void validateNumber(String value) {
        if (value.contains(".")) {
            String[] parts = value.split("\\.");
            if (parts.length > 1 && parts[1].length() > 5) {
                errorHandler.reportError(line, "Decimal exceeds 5 places: " + value);
            }
        }
        if (value.toUpperCase().contains("E") && !value.matches(".*E[+-]?\\d+")) {
            errorHandler.reportError(line, "Invalid exponent: " + value);
        }
    }

    private void handleComments() {
        if (pos + 1 >= input.length()) {
            pos++;
            return;
        }
        char nextChar = input.charAt(pos + 1);
        // Single-line comment.
        if (nextChar == '/') {
            pos += 2;
            while (pos < input.length() && input.charAt(pos) != '\n') {
                pos++;
            }
        }
        // Multi-line comment.
        else if (nextChar == '*') {
            pos += 2;
            int commentStartLine = line;
            boolean closed = false;
            while (pos < input.length() - 1) {
                if (input.charAt(pos) == '\n') {
                    line++;
                }
                if (input.charAt(pos) == '*' && input.charAt(pos + 1) == '/') {
                    pos += 2;
                    closed = true;
                    break;
                }
                pos++;
            }
            if (!closed) {
                errorHandler.reportError(commentStartLine, "Unclosed multi-line comment");
            }
        }
    }

    private String readStringLiteral() {
        StringBuilder sb = new StringBuilder();
        pos++; // Skip opening quote.
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '"') {
                pos++; // Skip closing quote.
                return sb.toString();
            }
            if (c == '\\') {
                pos++;
                if (pos < input.length()) {
                    char next = input.charAt(pos);
                    switch (next) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        default: sb.append(next); break;
                    }
                }
            } else {
                sb.append(c);
            }
            pos++;
        }
        errorHandler.reportError(line, "Unclosed string literal");
        return sb.toString();
    }

    private String readCharLiteral() {
        StringBuilder sb = new StringBuilder();
        pos++; // Skip opening apostrophe.
        if (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos < input.length()) {
                    char next = input.charAt(pos);
                    switch (next) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        default: sb.append(next); break;
                    }
                    pos++;
                }
            } else {
                sb.append(c);
                pos++;
            }
        }
        if (pos < input.length() && input.charAt(pos) == '\'') {
            pos++; // Skip closing apostrophe.
        } else {
            errorHandler.reportError(line, "Unclosed character literal");
        }
        return sb.toString();
    }
}

//------------------------- Symbol Table -------------------------
class SymbolTable {
    // The stack holds active scopes.
    private Stack<Map<String, String>> scopes = new Stack<>();
    // Completed scopes are stored here for display purposes.
    private List<Map<String, String>> completedScopes = new ArrayList<>();

    public SymbolTable() {
        // Global scope.
        scopes.push(new HashMap<>());
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            // Save the local scope for later display.
            Map<String, String> completed = scopes.peek();
            completedScopes.add(new HashMap<>(completed));
            scopes.pop();
        }
    }

    // Adds a symbol with its type (e.g., "int-global" or "int-local").
    public void addSymbol(String name, String type) {
        scopes.peek().put(name, type);
    }

    // Checks if a symbol exists in any active scope.
    public boolean lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    // Checks if a symbol exists in the current (top) scope.
    public boolean existsInCurrentScope(String name) {
        return scopes.peek().containsKey(name);
    }

    // Returns the current scope depth (1 = global, >1 = local).
    public int currentScopeDepth() {
        return scopes.size();
    }

    public void displaySymbols() {
        System.out.println("\n=== Symbol Table ===");
        // Display global scope.
        System.out.println("global scope:");
        Map<String, String> global = scopes.firstElement();
        for (Map.Entry<String, String> entry : global.entrySet()) {
            System.out.println("  " + entry.getKey() + " : " + entry.getValue());
        }
        // Display completed (exited) local scopes.
        for (int i = 0; i < completedScopes.size(); i++) {
            System.out.println("local scope (exited):");
            Map<String, String> local = completedScopes.get(i);
            for (Map.Entry<String, String> entry : local.entrySet()) {
                System.out.println("  " + entry.getKey() + " : " + entry.getValue());
            }
        }
        // Display any remaining active local scopes (if any beyond global).
        if (scopes.size() > 1) {
            for (int i = 1; i < scopes.size(); i++) {
                System.out.println("local scope (active):");
                Map<String, String> local = scopes.get(i);
                for (Map.Entry<String, String> entry : local.entrySet()) {
                    System.out.println("  " + entry.getKey() + " : " + entry.getValue());
                }
            }
        }
    }
}

//------------------------- Error Handler -------------------------
class ErrorHandler {
    private List<String> errors = new ArrayList<>();

    public void reportError(int line, String message) {
        String prefix = (line >= 0) ? "[Line " + line + "] " : "";
        errors.add(prefix + "ERROR: " + message);
    }

    public void displayErrors() {
        if (!errors.isEmpty()) {
            System.out.println("\n=== Errors ===");
            errors.forEach(System.out::println);
        }
    }
}

//------------------------- Parser -------------------------
// A simple parser that distinguishes declarations from assignments,
// manages scope using LBRACE and RBRACE tokens, and checks for use of undeclared variables.
class Parser {
    private List<Token> tokens;
    private int index = 0;
    private final SymbolTable symbolTable;
    private final ErrorHandler errorHandler;

    public Parser(List<Token> tokens, SymbolTable symbolTable, ErrorHandler errorHandler) {
        this.tokens = tokens;
        this.symbolTable = symbolTable;
        this.errorHandler = errorHandler;
    }

    private Token currentToken() {
        return tokens.get(index);
    }

    private void nextToken() {
        index++;
    }

    public void parse() {
        while (index < tokens.size() && !currentToken().type.equals("EOF")) {
            parseStatement();
        }
    }

    private void parseStatement() {
        Token token = currentToken();
        if (token.type.equals("LBRACE")) {
            symbolTable.enterScope();
            nextToken(); // consume LBRACE
            while (index < tokens.size() &&
                   !currentToken().type.equals("RBRACE") &&
                   !currentToken().type.equals("EOF")) {
                parseStatement();
            }
            if (index < tokens.size() && currentToken().type.equals("RBRACE")) {
                nextToken(); // consume RBRACE
                symbolTable.exitScope();
            } else {
                errorHandler.reportError(token.line, "Missing closing brace");
            }
        }
        // Declaration: a type keyword (int, decimal, bool, char, string)
        else if (token.type.equals("KEYWORD") && isTypeKeyword(token.lexeme)) {
            String typeKeyword = token.lexeme;
            int declLine = token.line;
            nextToken(); // consume type keyword
            if (index < tokens.size() && currentToken().type.equals("IDENTIFIER")) {
                Token identToken = currentToken();
                String ident = identToken.lexeme;
                if (ident.length() != 1) {
                    errorHandler.reportError(identToken.line,
                        "Invalid identifier '" + ident + "'. Must be a single lowercase letter.");
                }
                if (symbolTable.existsInCurrentScope(ident)) {
                    errorHandler.reportError(identToken.line,
                        "Variable '" + ident + "' already declared in this scope.");
                } else {
                    String scopeLabel = (symbolTable.currentScopeDepth() == 1) ? "global" : "local";
                    symbolTable.addSymbol(ident, typeKeyword + "-" + scopeLabel);
                }
                nextToken(); // consume identifier
                if (index < tokens.size() && currentToken().type.equals("OPERATOR")
                    && currentToken().lexeme.equals("=")) {
                    nextToken(); // consume '='
                } else {
                    errorHandler.reportError(declLine,
                        "Expected '=' in declaration for variable " + ident);
                }
                // Process the initialization expression.
                while (index < tokens.size() &&
                       !currentToken().type.equals("SEMICOLON") &&
                       !currentToken().type.equals("EOF")) {
                    Token exprToken = currentToken();
                    if (exprToken.type.equals("IDENTIFIER")) {
                        if (!symbolTable.lookup(exprToken.lexeme)) {
                            errorHandler.reportError(exprToken.line,
                                "Variable '" + exprToken.lexeme + "' used in initialization is not declared.");
                        }
                    }
                    nextToken();
                }
                if (index < tokens.size() && currentToken().type.equals("SEMICOLON")) {
                    nextToken(); // consume semicolon
                } else {
                    errorHandler.reportError(declLine,
                        "Missing semicolon after declaration of " + ident);
                }
            } else {
                errorHandler.reportError(declLine, "Expected identifier after type keyword " + typeKeyword);
            }
        }
        // Assignment statement.
        else if (token.type.equals("IDENTIFIER")) {
            int assignLine = token.line;
            String ident = token.lexeme;
            if (!symbolTable.lookup(ident)) {
                errorHandler.reportError(token.line, "Variable '" + ident + "' is not declared.");
            }
            nextToken(); // consume identifier
            if (index < tokens.size() && currentToken().type.equals("OPERATOR")
                && currentToken().lexeme.equals("=")) {
                nextToken(); // consume '='
            } else {
                errorHandler.reportError(assignLine, "Expected '=' in assignment for variable " + ident);
            }
            // Process right-hand side expression.
            while (index < tokens.size() &&
                   !currentToken().type.equals("SEMICOLON") &&
                   !currentToken().type.equals("EOF")) {
                Token exprToken = currentToken();
                if (exprToken.type.equals("IDENTIFIER")) {
                    if (!symbolTable.lookup(exprToken.lexeme)) {
                        errorHandler.reportError(exprToken.line,
                            "Variable '" + exprToken.lexeme + "' is not declared.");
                    }
                }
                nextToken();
            }
            if (index < tokens.size() && currentToken().type.equals("SEMICOLON")) {
                nextToken(); // consume semicolon
            } else {
                errorHandler.reportError(assignLine, "Missing semicolon in assignment for variable " + ident);
            }
        }
        // For any other tokens (like SEMICOLON), just consume.
        else {
            nextToken();
        }
    }

    private boolean isTypeKeyword(String keyword) {
        return keyword.equals("int") || keyword.equals("decimal") || keyword.equals("bool") ||
               keyword.equals("char") || keyword.equals("string");
    }
}

//------------------------- Compiler (Main) -------------------------
public class Compiler {
    public static void main(String[] args) {
        try {
            String input = readFile("Sample Code.txt");
            ErrorHandler errorHandler = new ErrorHandler();
            Lexer lexer = new Lexer(input, errorHandler);
            List<Token> tokens = lexer.tokenize();

            System.out.println("=== Tokens ===");
            tokens.forEach(System.out::println);

            SymbolTable symbolTable = new SymbolTable();
            Parser parser = new Parser(tokens, symbolTable, errorHandler);
            parser.parse();

            symbolTable.displaySymbols();
            errorHandler.displayErrors();

            // Display DFA transition tables.
            System.out.println("\n=== Identifier DFA Transition Table ===");
            lexer.identifierDFA.displayTransitionTable();
            System.out.println("\n=== Number DFA Transition Table ===");
            lexer.numberDFA.displayTransitionTable();
            System.out.println("\n=== Operator DFA Transition Table ===");
            lexer.operatorDFA.displayTransitionTable();

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static String readFile(String path) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
