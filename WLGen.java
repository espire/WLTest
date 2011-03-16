import java.util.*;

/**
 * Starter code for CS241 assignments 9 and 10 for Spring 2010.
 * 
 * Based on Scheme code by Gord Cormack. Java translation by Ondrej Lhotak.
 * 
 * Version 20081105.1
 *
 */
public class WLGen {
    Scanner in = new Scanner(System.in);

    // The set of terminal symbols in the WL grammar.
    Set<String> terminals = new HashSet<String>(Arrays.asList("BOF", "BECOMES", 
         "COMMA", "ELSE", "EOF", "EQ", "GE", "GT", "ID", "IF", "INT", "LBRACE", 
         "LE", "LPAREN", "LT", "MINUS", "NE", "NUM", "PCT", "PLUS", "PRINTLN",
         "RBRACE", "RETURN", "RPAREN", "SEMI", "SLASH", "STAR", "WAIN", "WHILE"));

    List<String> symbols;

    // Data structure for storing the parse tree.
    public class Tree {
        List<String> rule;

        ArrayList<Tree> children = new ArrayList<Tree>();

        // Does this node's rule match otherRule?
        boolean matches(String otherRule) {
            return tokenize(otherRule).equals(rule);
        }
    }

    // Divide a string into a list of tokens.
    List<String> tokenize(String line) {
        List<String> ret = new ArrayList<String>();
        Scanner sc = new Scanner(line);
        while (sc.hasNext()) {
            ret.add(sc.next());
        }
        return ret;
    }

    // Read and return wli parse tree
    Tree readParse(String lhs) {
        String line = in.nextLine();
        List<String> tokens = tokenize(line);
        Tree ret = new Tree();
        ret.rule = tokens;
        if (!terminals.contains(lhs)) {
            Scanner sc = new Scanner(line);
            sc.next(); // discard lhs
            while (sc.hasNext()) {
                String s = sc.next();
                ret.children.add(readParse(s));
            }
        }
        return ret;
    }

    // Compute symbols defined in t
    List<String> genSymbols(Tree t) {
        if (t.matches("S BOF procedure EOF")) {
            // recurse on procedure
            return genSymbols(t.children.get(1));
        } else if (t.matches("procedure INT WAIN LPAREN dcl COMMA dcl RPAREN LBRACE dcls statements RETURN expr SEMI RBRACE")) {
            List<String> ret = new ArrayList<String>();
            // recurse on dcl and dcl
            ret.addAll(genSymbols(t.children.get(3)));
            ret.addAll(genSymbols(t.children.get(5)));
            return ret;
        } else if (t.matches("dcl INT ID")) {
            // recurse on ID
            return genSymbols(t.children.get(1));
        } else if (t.rule.get(0).equals("ID")) {
            List<String> ret = new ArrayList<String>();
            ret.add(t.rule.get(1));
            return ret;
        } else {
            bail("unrecognized rule " + t.rule);
            return null;
        }
    }

    // Print an error message and exit the program.
    void bail(String msg) {
        System.err.println("ERROR: " + msg);
        System.exit(0);
    }

    // Generate the code for the parse tree t.
    String genCode(Tree t) {
        if (t.matches("S BOF procedure EOF")) {
            return genCode(t.children.get(1)) + "jr $31\n";
        } else if (t
                .matches("procedure INT WAIN LPAREN dcl COMMA dcl RPAREN LBRACE dcls statements RETURN expr SEMI RBRACE")) {
            return genCode(t.children.get(11));
        } else if (t.matches("expr term")) {
            return genCode(t.children.get(0));
        } else if (t.matches("term factor")) {
            return genCode(t.children.get(0));
        } else if (t.matches("factor ID")) {
            return genCode(t.children.get(0));
        } else if (t.rule.get(0).equals("ID")) {
            String name = t.rule.get(1); // variable name
			for(String s : symbols) { // iterate through the symbol table
				if(name.equals(s)) {
					return "add $3,$0,$1\n";
					// this code DOES NOT fetch the variable from the correct location
					// it is placeholder code until fetching is implemented
				} // if
			} // for
			bail("symbol already defined: " + name);
			return null; // this line will never be executed
        } else {
            bail("unrecognized rule " + t.rule);
            return null;
        }
    }

    // Main program
    public static final void main(String args[]) {
        new WLGen().go();
    }

    public void go() {
        Tree parseTree = readParse("S");
        symbols = genSymbols(parseTree);
		for(String s : symbols) {
			System.err.println(s);
		}
        System.out.print(genCode(parseTree));
    }
}