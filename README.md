# **Compiler for Custom Programming Language**

## **Overview**
This project implements a compiler for a custom programming language. The compiler includes lexical analysis, syntax parsing, symbol table management, DFA-based token validation, and error handling. It reads a source file, tokenizes the content, builds an abstract syntax tree, and ensures proper syntax and semantic validation.

## **Features**
- **Lexical Analysis**: Tokenizes the source code into keywords, operators, identifiers, and constants.
- **Syntax Parsing**: Implements grammar rules to validate the structure of statements.
- **Symbol Table Management**: Stores tokens with attributes like type and scope.
- **DFA-Based Token Validation**: Uses deterministic finite automata (DFA) to verify token validity.
- **Error Handling**: Detects syntax errors such as missing semicolons, unmatched braces, and invalid identifiers.

## **File Structure**
- `Compiler.java` – Main class that drives lexical analysis, parsing, and token validation.
- `Lexer.java` – Handles tokenization and builds the symbol table.
- `Parser.java` – Implements syntax analysis based on predefined grammar rules.
- `SymbolTable.java` – Manages tokens and their attributes.
- `DFA.java` – Defines and processes DFA transitions for token validation.
- `ErrorChecker.java` – Detects syntax errors and reports issues.

## **Language Rules**

### **1. Keywords**
Reserved words recognized in the language:
new, if, else, return, for, while, switch, case, break, continue

### **2. Datatypes**
Supported primitive data types:
int, float, double, char, boolean, string

### **3. Operators**
Supported arithmetic, relational, and logical operators:
/ % ^ && || ! == != > < >= <=

### **4. Input Operations**
Methods for user input handling:
Scanner, System.in, nextInt(), nextLine(), BufferedReader, readLine()

### **5. Output Operations**
Methods for displaying output:
System.out.print, System.out.println, System.out.printf

### **6. Special Symbols**
Recognized punctuation and control symbols:
= ; ( ) { } [ ]

### **7. Constants**
Supported numeric and character constants:
0-9 (integer values), floating-point numbers, character literals ('a', 'b', etc.)

### **8. Identifiers**
Naming rules:
- Must start with a lowercase letter.
- May contain additional lowercase letters, digits, or underscores.


## Symbol Table

The symbol table stores each token along with:

Token Value: The actual token from the source code.

Type: Classification such as Keyword, Identifier, Operator, etc.

Scope: Global or Local (for identifiers).

## Token Validation

A master regular expression is used to validate tokens.

DFA transitions are applied to check if a token is valid.

## Error Checking

The ErrorChecker class scans the source code and detects syntax errors.

Reports missing semicolons, unmatched braces, and invalid identifiers.

## Contributors
Arshaq Kirmani


