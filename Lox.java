package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

//import javax.management.loading.PrivateClassLoader;

//import com.sun.org.apache.xpath.internal.operations.String;

public class Lox {
    static boolean hadError = false;  // We use this to ensure we don't try to execute code
                                      // that has a known error

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }


    /*
     * Lox is a scripting language, meaning it can run directly from source.
     * Starting jlox from the command line and giving it the path to a file
     * allows it to read the file and execute it.
    */

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code
        if (hadError) System.exit(65);
    }

    /* Lox can also be run interactively by simply calling it without
     * any arguments.
    */

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break; // so we exit by entering EOF
            run(line);
            hadError = false;
        }
    }

    /* Both prompt and file runner are thin wrappers around the foll.
     * function:
     */

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        // stop if there was a syntax error
        if (hadError) return;

        System.out.println(new AstPrinter().print(expression));

    }

    /* ERROR HANDLING: A very important aspect of creating a language.
     * It is more of a practical matter than a formal concept, but it
     * is what separates a usable implementation from a useless one.
    */

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where,
            String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    /* It is a good engineering practice to separate the code that
     * /generates/ the errors from the code that /reports/ them.
    */

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

}

/* STEP:1: Implementing the Scanner.
 *
  +-------------------+     +---------+     +--------+
  |  Raw source code  |---->| Scanner |---->| Tokens |
  +-------------------+     +---------+     +--------+
*
* A `switch` statement with delusions of grandeur, basically.
*/
