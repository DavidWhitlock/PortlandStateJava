package edu.pdx.cs399J.family;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * This class parses the text output generated by a
 * <code>TextDumper</code> and creates a family tree.
 *
 * @see TextDumper
 */
public class TextParser implements Parser {

  private LineNumberReader in;     // Read input from here
  private FamilyTree tree;         // The family tree we're building

  ///////////////////////  Static Methods  ///////////////////////

  private static final PrintStream out = System.out;
  private static final PrintStream err = System.err;

  private static void db(String s) {
    if (Boolean.getBoolean("TextParser.DEBUG")) {
      out.println(s);
    }
  }

  ////////////////////////  Constructors  ////////////////////////

  /**
   * Creates a new text parser that reads its input from a file of a
   * given name.
   */
  public TextParser(String fileName) throws FileNotFoundException{
    this(new File(fileName));
  }

  /**
   * Creates a new text parser that reads its input from the given
   * file. 
   */
  public TextParser(File file) throws FileNotFoundException {
    this(new FileReader(file));
  }

  /**
   * Creates a new text parser that reads its input from the given
   * writer.  This lets us read from a sources other than files.
   */
  public TextParser(Reader reader) {
    this.in = new LineNumberReader(reader);
  }

  //////////////////////  Instance Methods  //////////////////////

  /**
   * Helper method that creates an error string and throws a
   * <code>FamilyTreeException</code>.
   */
  private void error(String message) throws FamilyTreeException {
    int lineNumber = this.in.getLineNumber();
    String m = "Error at line " + lineNumber + ": " + message;
    throw new FamilyTreeException(m);
  }

  /**
   * Parses the specified input source and from it creates a family
   * tree.
   *
   * @throws FamilyTreeException
   *         The data source is malformatted
   */
  public FamilyTree parse() throws FamilyTreeException {
    this.tree = new FamilyTree();

    // Examine each line of the file.  The first line should contain a
    // header of the form "x n" where "x" is either "P" or "M" and "n"
    // is the number of lines the entry takes up.  Parse this header
    // and delegate the responsibility for constructing objects to
    // other methods.

    try {
      while (this.in.ready()) {
	String line = this.in.readLine();

	// Ignore empty lines
        if (line == null) {
          break;

        } else if(line.equals("")) {
	  continue;
	}

        db("Read line: \"" + line + "\"");

	// Parse the header line
	String type = null;
	String nLines = null;
	StringTokenizer st = new StringTokenizer(line, " ");
	if(st.hasMoreTokens()) {
	  type = st.nextToken();

	} else {
	  error("Missing type token in header");
	}

	if(st.hasMoreTokens()) {
	  nLines = st.nextToken();

	} else {
	  error("Missing line count in header");
	}

	try {
	  int n = Integer.parseInt(nLines);
	  if (type.equals("P")) {
	    this.parsePerson(n);

	  } else if (type.equals("M")) {
	    this.parseMarriage(n);

	  } else {
	    error("Invalid type string: " + type);
	  }

	} catch (NumberFormatException ex) {
	  error("Malformatted line count: " + nLines);
	}

      }
    
    } catch (IOException ex) {
      int lineNumber = this.in.getLineNumber();
      String m = "Parsing error at line " + lineNumber;
      throw new FamilyTreeException(m);
    }

    // Okay, we're all done parsing the tree now we need to "patch up"
    // the Person objects to make sure that their mothers and fathers
    // all exist.
    Iterator people = this.tree.getPeople().iterator();
    while (people.hasNext()) {
      Person person = (Person) people.next();
      person.patchUp(this.tree);
    }

    return this.tree;
  }

  /**
   * Helper method that parses the source, creates a
   * <code>Person</code>, and adds it to the family tree.
   */
  private void parsePerson(int nLines) throws FamilyTreeException {
    Person person = null;

    DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);

    for (int i = 0; i < nLines; i++) {
      String line = null;
      try {
	if(!this.in.ready()) {
	  // The reader should be ready, Houston we have a problem!
	  error("Unexpected end of file");
	}

	line = this.in.readLine();

      } catch (IOException ex) {
	error("IOException: " + ex.getMessage());
      }

      if (line == null) {
        break;
      }

      // Ignore empty lines
      if (line.equals("")) {
	continue;
      }

      db("Read line: \"" + line + "\"");

      // Parse the line
      String key = null;
      String value = null;

      StringTokenizer st = new StringTokenizer(line, ":");
	
      if (st.hasMoreTokens()) {
	key = st.nextToken();

      } else {
	error("No key specified");
      }

      if (st.hasMoreTokens()) {
	StringBuffer sb = new StringBuffer();
	while(st.hasMoreTokens()) {
	  sb.append(st.nextToken() + " ");
	}
	value = sb.toString().trim();

      } else {
	error("No value specified");
      }

      // Now do a "switch" on the key and parse the value
      // appropriately
      if (key.equals("id")) {
	// id
	try {
	  int id = Integer.parseInt(value);
          if (this.tree.getPerson(id) != null) {
            error("FamilyTree already has person " + id);

          } else {
            // Call this Person constructor because we will fill in
            // the gender later
            person = new Person(id);
            this.tree.addPerson(person);
          }

	} catch (NumberFormatException ex) {
	  error("Malformatted id: " + value);
	}

      } else if (key.equals("g")) {
        // gender
        if (person != null) {
          try {
            int gender = Integer.parseInt(value);
            person.setGender(gender);
            db("Set gender of " + person + " to " + gender);

          } catch (NumberFormatException ex) {
            error("Malformed gender: " + value);
          }

        } else {
          error("Id must be specified before gender");
        }

      } else if (key.equals("fn")) {
	// firstName
	if(person != null) {
	  person.setFirstName(value);

	} else {
	  error("Id must be specified before first name");
	}

      } else if (key.equals("mn")) {
	// middleName
	if(person != null) {
	  person.setMiddleName(value);

	} else {
	  error("Id must be specified before middle name");
	}

      } else if (key.equals("ln")) {
	// lastName
	if(person != null) {
	  person.setLastName(value);

	} else {
	  error("Id must be specified before last name");
	}

      } else if (key.equals("f")) {
	// father
	if(person != null) {
	  try {
	    int fatherId  = Integer.parseInt(value);
	    person.setFatherId(fatherId);

	  } catch (NumberFormatException ex) {
	    error("Malformatted father id: " + value);
	  }

	} else {
	  error("Id must be specified before father");
	}

      } else if (key.equals("m")) {
	// mother
	if(person != null) {
	  try {
	    int motherId = Integer.parseInt(value);
	    person.setMotherId(motherId);

	  } catch (NumberFormatException ex) {
	    error("Malformatted mother id: " + value);
	  }

	} else {
	  error("Id must be specified before mother");
	}

      } else if (key.equals("dob")) {
	// date of birth
	if(person != null) {
	  try {
	    person.setDateOfBirth(df.parse(value));

	  } catch (ParseException ex) {
	    error("Malformatted date of birth: " + value);
	  }

	} else {
	  error("Id must be specified before date of birth");
	}

      } else if (key.equals("dod")) {
	// date of death
	if(person != null) {
	  try {
	    person.setDateOfDeath(df.parse(value));

	  } catch (ParseException ex) {
	    error("Malformatted date of death: " + value);
	  }

	} else {
	  error("Id must be specified before date of death");
	}

      } else {
	error("Unknown person key: " + key);
      }
    }
  }

  /**
   * Helper method that parses the source and create marriages.
   */
  private void parseMarriage(int nLines) throws FamilyTreeException {
    Marriage marriage = null;

    DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);

    if (nLines < 1) {
      error("Missing ids in marriage");
    }

    String line = null;
    try {
      if (!this.in.ready()) {
	// The reader should be ready, Houston we have a problem!
	error("Unexpected end of file");
      }

      line = this.in.readLine();

    } catch (IOException ex) {
      error("IOException: " + ex.getMessage());
    }

    // The first line should be the id of the husband followed by the
    // id of the wife
    Person husband = null;
    Person wife = null;
    StringTokenizer st = new StringTokenizer(line, " ");
    if (st.hasMoreTokens()) {
      String s = st.nextToken();
      try {
	int husbandId = Integer.parseInt(s);
	husband = this.tree.getPerson(husbandId);

      } catch (NumberFormatException ex) {
	error("Malformatted husband id: " + s);
      }

    } else {
      error("Missing husband id");
    }

    if (st.hasMoreTokens()) {
      String s = st.nextToken();
      try {
	int wifeId = Integer.parseInt(s);
	wife = this.tree.getPerson(wifeId);

      } catch (NumberFormatException ex) {
	error("Malformatted wife id: " + s);
      }

    } else {
      error("Missing wife id");
    }

    marriage = new Marriage(husband, wife);
    wife.addMarriage(marriage);
    husband.addMarriage(marriage);
    
    // Parse the rest
    for (int i = 1; i < nLines; i++) {
      line = null;
      try {
	if(!this.in.ready()) {
	  // The reader should be ready, Houston we have a problem!
	  error("Unexpected end of file");
	}

        line = this.in.readLine();

      } catch (IOException ex) {
        error("IOException: " + ex.getMessage());
      }

      // Ignore empty lines
      if (line.equals("")) {
        continue;
      }


      // Parse the line
      String key = null;
      String value = null;

      st = new StringTokenizer(line, ":");

      if (st.hasMoreTokens()) {
        key = st.nextToken();

      } else {
        error("No key specified");
      }

      if (st.hasMoreTokens()) {
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
          sb.append(st.nextToken() + " ");
        }
        value = sb.toString().trim();

      } else {
        error("No value specified for key " + key);
      }

      // Now do a "switch" on the key and parse the value
      // appropriately
      if (key.equals("d")) {
	// date
	try {
	  marriage.setDate(df.parse(value));

	} catch (ParseException ex) {
	  error("Malformatted marriage date: " + value);
	}

      } else if (key.equals("l")) {
	marriage.setLocation(value);

      } else {
	error("Unknown marriage key: " + key);
      }
    }
  }

  /**
   * Test program.  Parse the file that is given on the command line.
   * Pretty print the resulting family tree.
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("** Missing file name");
      System.exit(1);
    }

    // Parse the input file
    String fileName = args[0];
    try {
      TextParser parser = new TextParser(fileName);
      FamilyTree tree = parser.parse();

      PrintWriter out = new PrintWriter(System.out, true);
      PrettyPrinter pretty = new PrettyPrinter(out);
      pretty.dump(tree);

    } catch (FileNotFoundException ex) {
      System.err.println("** Could not find file " + fileName);

    } catch (FamilyTreeException ex) {
      System.err.println("** " + ex.getMessage());
    }
  }

}
