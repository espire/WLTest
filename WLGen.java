import java.util.*;

/**
 * Starter code for CS241 assignments 9 and 10 for Spring 2010.
 * 
 * Based on Scheme code by Gord Cormack. Java translation by Ondrej Lhotak.
 * 
 * Version 20081105.1
 *
 * Augmented by Eli Spiro for use on said assignments.
 * Code not originally part of the starter code is copyright (c) Eli Spiro 2011
 *
 * IMPORTANT INFO FOR UNIVERSITY OF WATERLOO STUDENTS:
 * If you are a CS 241 student, you are DISALLOWED to gain any benefit from this.
 * That includes copying, modifying, or even reading the code. FLY AWAY NOW!
 *
 * If you are not a CS 241 student, you are free to read the code, but you are
 * not free to modify, save, or redistribute it.
 */
public class WLGen {
    Scanner in = new Scanner(System.in);

    // The set of terminal symbols in the WL grammar.
    Set<String> terminals = new HashSet<String>(Arrays.asList("BOF", "BECOMES", 
         "COMMA", "ELSE", "EOF", "EQ", "GE", "GT", "ID", "IF", "INT", "LBRACE", 
         "LE", "LPAREN", "LT", "MINUS", "NE", "NUM", "PCT", "PLUS", "PRINTLN",
         "RBRACE", "RETURN", "RPAREN", "SEMI", "SLASH", "STAR", "WAIN", "WHILE"));

    List<String> symbols;

	String prologue = "; PROLOGUE\n; \"constant\" registers:\nlis $4\n.word 4\nlis $11\n.word 11\n\n";
	String epilogue = "\n; EPILOGUE\n; exit to DOS\njr $31\n";

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
            ret.addAll(genSymbols(t.children.get(3))); // first dcl
            ret.addAll(genSymbols(t.children.get(5))); // second dcl
			//ret.addAll(genSymbols(t.children.get(8))); // dcls, not until later parts of assignment
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
        System.exit(1); // 1 tells shell there was an error
    }

    // Generate the code for the parse tree t.
    String genCode(Tree t) {
        if (t.matches("S BOF procedure EOF")) {
            return prologue + genCode(t.children.get(1)) + epilogue;
        } else if (t
			.matches("procedure INT WAIN LPAREN dcl COMMA dcl RPAREN LBRACE dcls statements RETURN expr SEMI RBRACE")) {
            return genCode(t.children.get(11));
		} else if (t.matches("expr term")) {
            return genCode(t.children.get(0));
        } else if (t.matches("term factor")) {
            return genCode(t.children.get(0));
        } else if (t.matches("factor ID")) {
            return genCode(t.children.get(0));
		} else if (t.matches("factor NUM")) {
			return genCode(t.children.get(0));
		} else if (t.rule.get(0).equals("NUM")) {
			String ret = "lis $3\n";
			ret += (".word " + t.rule.get(1) + " ; load program constant\n");
			return ret;
		} else if (t.matches("factor LPAREN expr RPAREN")) {
			return genCode(t.children.get(1));
        } else if (t.matches("expr expr PLUS term")) {
			String ret = "; expr PLUS term\n";
			ret += genCode(t.children.get(2));
			ret += "sw $3,-4($30)\n";
			ret += "sub $30,$30,$4 ; push the stack\n";
			ret += genCode(t.children.get(0));
			ret += "lw $5,0($30)\n";
			ret += "add $30,$30,$4 ; pop the stack\n";
			ret += "add $3,$3,$5 ; result of PLUS\n";
			return ret;
		} else if (t.matches("expr expr MINUS term")) {
			String ret = "; expr MINUS term\n";
			ret += genCode(t.children.get(2));
			ret += "sw $3,-4($30)\n";
			ret += "sub $30,$30,$4 ; push the stack\n";
			ret += genCode(t.children.get(0));
			ret += "lw $5,0($30)\n";
			ret += "add $30,$30,$4 ; pop the stack\n";
			ret += "sub $3,$3,$5 ; result of MINUS\n";
			return ret;
		} else if (t.matches("term term STAR factor")) {
			String ret = "; term STAR factor\n";
			ret += genCode(t.children.get(2));
			ret += "sw $3,-4($30)\n";
			ret += "sub $30,$30,$4\n";
			ret += genCode(t.children.get(0));
			ret += "lw $5,0($30)\n";
			ret += "add $30,$30,$4\n";
			ret += "mult $3,$5\n";
			ret += "mflo $3 ; result of STAR\n";
			return ret;
		} else if (t.matches("term term SLASH factor")) {
			String ret = "; term SLASH factor\n";
			ret += genCode(t.children.get(2));
			ret += "sw $3,-4($30)\n";
			ret += "sub $30,$30,$4\n";
			ret += genCode(t.children.get(0));
			ret += "lw $5,0($30)\n";
			ret += "add $30,$30,$4\n";
			ret += "div $3,$5\n";
			ret += "mflo $3 ; result of SLASH\n";
			return ret;
		} else if (t.matches("term term PCT factor")) {
			String ret = "; term PCT factor\n";
			ret += genCode(t.children.get(2));
			ret += "sw $3,-4($30)\n";
			ret += "sub $30,$30,$4\n";
			ret += genCode(t.children.get(0));
			ret += "lw $5,0($30)\n";
			ret += "add $30,$30,$4\n";
			ret += "div $3,$5\n";
			ret += "mfhi $3 ; result of PERCENT\n";
			return ret;
		} else if (t.rule.get(0).equals("ID")) {
            String name = t.rule.get(1); // variable name
			for(String s : symbols) { // iterate through the symbol table
				if(name.equals(s)) { // found our symbol
					if (name.equals(symbols.get(0)))
						return "add $3,$0,$1\n";
					if (name.equals(symbols.get(1)))
						return "add $3,$0,$2\n";
					return null;
					// this code DOES NOT fetch the variable from the correct location
					// it is placeholder code until fetching is implemented
				} // if
			} // for
			bail("symbol not found: " + name); // we couldn't find the symbol; abandon ship!
			return null; // this line will never be executed
        } else {
            bail("unrecognized rule " + t.rule);
            return null;
        }
    }

	void symbolDuplicates(List<String> symbols) {
		for(String s : symbols) {
			boolean defined = false; // has the symbol already been defined once in our iteration?
			for(String t : symbols) { // cartesian comparison of symbols against symbols
				if(s.equals(t)) { // if we found a match
					if(defined == true) {
						bail("symbol already defined: " + s);
					}
					else defined = true;
				} // if
			} // for : t
			//System.err.println(s); spits out complete symbol table
		} // for : s
	} // symbolDuplicates

    // Main program
    public static final void main(String args[]) {
        new WLGen().go();
    }

    public void go() {
        Tree parseTree = readParse("S");
        symbols = genSymbols(parseTree);
		symbolDuplicates(symbols);
        System.out.print(genCode(parseTree));
    }
}