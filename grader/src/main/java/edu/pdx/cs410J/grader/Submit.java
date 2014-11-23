package edu.pdx.cs410J.grader;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

/**
 * This class is used to submit assignments in CS410J.  The user
 * specified his or her email address as well as the base directory
 * for his/her source files on the command line.  The directory is
 * searched recursively for files ending in .java.  Those files are
 * placed in a jar file and emailed to the grader.  A confirmation
 * email is sent to the submitter.
 * <p/>
 * <p/>
 * <p/>
 * More information about the JavaMail API can be found at:
 * <p/>
 * <center><a href="http://java.sun.com/products/javamail">
 * http://java.sun.com/products/javamail</a></center>
 *
 * @author David Whitlock
 * @since Fall 2000 (Refactored to use fewer static methods in Spring 2006)
 */
public class Submit extends EmailSender {

  private static final PrintWriter out = new PrintWriter(System.out, true);
  private static final PrintWriter err = new PrintWriter(System.err, true);

  /**
   * The grader's email address
   */
  private static final String TA_EMAIL = "sjavata@gmail.com";

  /**
   * A URL containing a list of files that should not be submitted
   */
  private static final String NO_SUBMIT_LIST_URL =
    "http://www.cs.pdx.edu/~whitlock/no-submit";

  private static final String PROJECT_NAMES_LIST_URL =
    "http://www.cs.pdx.edu/~whitlock/project-names";

  /////////////////////  Instance Fields  //////////////////////////

  /**
   * The name of the project being submitted
   */
  private String projName = null;

  /**
   * The name of the user (student) submits the project
   */
  private String userName = null;

  /**
   * The submitter's email address
   */
  private String userEmail = null;

  /**
   * The submitter's user id
   */
  private String userId = null;

  /**
   * A comment describing the project
   */
  private String comment = null;

  /**
   * Should the execution of this program be logged?
   */
  private boolean debug = false;

  /**
   * Should the generated jar file be saved?
   */
  private boolean saveJar = false;

  /**
   * The time at which the project was submitted
   */
  private Date submitTime = null;

  /**
   * The names of the files to be submitted
   */
  private Set<String> fileNames = new HashSet<>();

  /**
  * Koans submission mode.
  */
  private boolean koansMode = false;

  ///////////////////////  Constructors  /////////////////////////

  /**
   * Creates a new <code>Submit</code> program
   */
  public Submit() {

  }

  /////////////////////  Instance Methods  ///////////////////////

  /**
   * Sets the name of the SMTP server that is used to send emails
   */
  public void setServerName(String serverName) {
    EmailSender.serverName = serverName;
  }

  /**
   * Sets whether or not the progress of the submission should be logged.
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * Sets whether or not the jar file generated by the submission
   * should be saved.
   */
  public void setSaveJar(boolean saveJar) {
    this.saveJar = saveJar;
  }

  /**
   * Sets the comment for this submission
   */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /**
   * Sets the name of project being submitted
   */
  public void setProjectName(String projName) {
    this.projName = projName;
  }

  /**
   * Sets the name of the user who is submitting the project
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Sets the id of the user who is submitting the project
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Sets the email address of the user who is submitting the project
   */
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
   * Adds the file with the given name to the list of files to be
   * submitted.
   */
  public void addFile(String fileName) {
    this.fileNames.add(fileName);
  }

  /**
  * Sets the flag to enable koan submission mode
  */
  public void setKoansMode(boolean koansMode) {
    this.koansMode = koansMode;
  }
  /**
   * Validates the state of this submission
   *
   * @throws IllegalStateException If any state is incorrect or missing
   */
  public void validate() {
    validateProjectName();

    if (projName == null) {
      throw new IllegalStateException("Missing project name");
    }

    if (userName == null) {
      throw new IllegalStateException("Missing student name");
    }

    if (userId == null) {
      throw new IllegalStateException("Missing login id");
    }

    if (userEmail == null) {
      throw new IllegalStateException("Missing email address");

    } else {
      // Make sure user's email is okay
      try {
        new InternetAddress(userEmail);

      } catch (AddressException ex) {
        String s = "Invalid email address: " + userEmail;
        IllegalStateException ex2 = new IllegalStateException(s);
        ex2.initCause(ex);
        throw ex2;
      }
    }
  }

  private void validateProjectName() {
    List<String> validProjectNames = fetchListOfValidProjectNames();
    if (!validProjectNames.contains(projName)) {
      String message = "\"" + projName + "\" is not in the list of valid project names: " + validProjectNames;
      throw new IllegalStateException(message);
    }
  }

  private List<String> fetchListOfValidProjectNames() {
    return fetchListOfStringsFromUrl(PROJECT_NAMES_LIST_URL);
  }

  /**
   * Submits the project to the grader
   *
   * @param verify Should the user be prompted to verify his submission?
   * @return Whether or not the submission actually occurred
   * @throws IllegalStateException If no source files were found
   */
  public boolean submit(boolean verify) throws IOException, MessagingException {

    Set<File> sourceFiles;
    if(!koansMode){
      // Recursively search the source directory for .java files
      sourceFiles = searchForSourceFiles(fileNames);
    } else {
      sourceFiles = findAllFilesUnderWorkingDirectory();
    }

    db(sourceFiles.size() + " source files found");

    if (sourceFiles.size() == 0) {
      String s = "No source files were found.";
      throw new IllegalStateException(s);
    } else if (sourceFiles.size() < 3) {
      String s = "Too few source files were submitted. Each project requires at least 3 files be" + 
                 " submitted. You only submitted " + sourceFiles.size() + " files";
      throw new IllegalStateException(s);
    }

    // Verify submission with user
    if (verify && !verifySubmission(sourceFiles)) {
      // User does not want to submit
      return false;
    }

    // Timestamp
    this.submitTime = new Date();

    // Create a temporary jar file to hold the source files
    File jarFile = makeJarFileWith(sourceFiles);

    // Send the jar file as an email attachment to the TA
    mailTA(jarFile, sourceFiles);

    // Send a receipt to the user
    mailReceipt(sourceFiles);

    return true;
  }

  /**
   * Prints debugging output.
   */
  private void db(String s) {
    if (this.debug) {
      err.println("++ " + s);
    }
  }

  /**
   * Searches for the files given on the command line.  Ignores files
   * that do not end in .java, or that appear on the "no submit" list.
   * Files must reside in a directory named
   * edu/pdx/cs410J/<studentId>.
   */
  private Set<File> searchForSourceFiles(Set<String> fileNames) {
    List<String> noSubmit = fetchListOfFilesThatCanNotBeSubmitted();

    // Files should be sorted by name
    SortedSet<File> files =
      new TreeSet<>((o1, o2) -> o1.toString().compareTo(o2.toString()));

    for (String fileName : fileNames) {
      File file = new File(fileName);
      file = file.getAbsoluteFile();  // Full path

      // Does the file exist?
      if (!file.exists()) {
        err.println("** Not submitting file " + fileName +
          " because it does not exist");
        continue;
      }

      // Is the file on the "no submit" list?
      String name = file.getName();
      if (noSubmit.contains(name)) {
        err.println("** Not submitting file " + fileName +
          " because it is on the \"no submit\" list");
        continue;
      }

      // Does the file name end in .java?
      if (!name.endsWith(".java")) {
        err.println("** No submitting file " + fileName +
          " because does end in \".java\"");
        continue;
      }

      // Verify that file is in the correct directory.
      File parent = file.getParentFile();
      if (parent == null || !parent.getName().equals(userId)) {
        err.println("** Not submitting file " + fileName +
          ": it does not reside in a directory named " +
          userId);
        continue;
      }

      parent = parent.getParentFile();
      if (parent == null || !parent.getName().equals("cs410J")) {
        err.println("** Not submitting file " + fileName +
          ": it does not reside in a directory named " +
          "cs410J" + File.separator + userId);
        continue;
      }

      parent = parent.getParentFile();
      if (parent == null || !parent.getName().equals("pdx")) {
        err.println("** Not submitting file " + fileName +
          ": it does not reside in a directory named " +
          "pdx" + File.separator + "cs410J" + File.separator
          + userId);
        continue;
      }

      parent = parent.getParentFile();
      if (parent == null || !parent.getName().equals("edu")) {
        err.println("** Not submitting file " + fileName +
          ": it does not reside in a directory named " +
          "edu" + File.separator + "pdx" + File.separator +
          "cs410J" + File.separator + userId);
        continue;
      }

      // We like this file
      files.add(file);
    }

    return files;
  }

  /**
   * Finds all files under current working directory by calling
   * a recursive function that searches all directories under it
   * and adds files with .java extension.
   */
  private Set<File> findAllFilesUnderWorkingDirectory(){
    Set<File> files = new TreeSet<>();
    String path = System.getProperty("user.dir");
    recursiveJavaSourceFileAdder(path, files);
    return files;
  }

  /**
   * Recursively finds all files under a given path and adds them to the
   * files set.
   * Modified version of: http://stackoverflow.com/questions/2056221/recursively-list-files-in-java
   */
  public void recursiveJavaSourceFileAdder(String path, Set<File> files) {
    File root = new File(path);
    File[] list = root.listFiles();

    if (list == null) return;

    for ( File f : list ) {
      if ( f.isDirectory() ) {
        recursiveJavaSourceFileAdder(f.getAbsolutePath(), files);
      } else {
        String filename = f.getAbsoluteFile().toString();
        if (filename.split("\\.")[1].equals("java")){
          files.add(f);
        }
      }
    }
  }

  private List<String> fetchListOfFilesThatCanNotBeSubmitted() {
    return fetchListOfStringsFromUrl(NO_SUBMIT_LIST_URL);
  }

  private List<String> fetchListOfStringsFromUrl(String listUrl) {
    List<String> noSubmit = new ArrayList<>();

    try {
      URL url = new URL(listUrl);
      InputStreamReader isr = new InputStreamReader(url.openStream());
      BufferedReader br = new BufferedReader(isr);
      while (br.ready()) {
        noSubmit.add(br.readLine().trim());
      }

    } catch (MalformedURLException ex) {
      err.println("** WARNING: Cannot access " + listUrl + ": " +
        ex.getMessage());

    } catch (IOException ex) {
      err.println("** WARNING: Problems while reading " + listUrl + ": " + ex.getMessage());
    }
    return noSubmit;
  }

  /**
   * Prints a summary of what is about to be submitted and prompts the
   * user to verify that it is correct.
   *
   * @return <code>true</code> if the user wants to submit
   */
  private boolean verifySubmission(Set<File> sourceFiles) {
    // Print out what is going to be submitted
    out.print("\n" + userName);
    out.print("'s submission for ");
    out.println(projName);

    for (File file : sourceFiles) {
      out.println("  " + file);
    }

    if (comment != null) {
      out.println("\nComment: " + comment + "\n\n");
    }

    out.println("A receipt will be sent to: " + userEmail + "\n");

    warnIfMainProjectClassIsNotSubmitted(sourceFiles);

    return doesUserWantToSubmit();
  }

  private void warnIfMainProjectClassIsNotSubmitted(Set<File> sourceFiles) {
    boolean wasMainProjectClassSubmitted = sourceFiles.stream().anyMatch((f) -> f.getName().contains(this.projName));
    if (!wasMainProjectClassSubmitted) {
      String mainProjectClassName = this.projName + ".java";
      out.println("*** WARNING: You are submitting " + this.projName +
        ", but did not include " + mainProjectClassName + ".\n" +
        "    You might want to check the name of the project or the files you are submitting.\n");
    }
  }

  private boolean doesUserWantToSubmit() {
    InputStreamReader isr = new InputStreamReader(System.in);
    BufferedReader in = new BufferedReader(isr);

    while (true) {
      out.print("Do you wish to continue with the submission? (yes/no) ");
      out.flush();

      try {
        String line = in.readLine().trim();
        switch (line) {
          case "yes":
            return true;

          case "no":
            return false;

          default:
            err.println("** Please enter yes or no");
            break;
        }

      } catch (IOException ex) {
        err.println("** Exception while reading from System.in: " + ex);
      }
    }
  }

  /**
   * Returns the name of a <code>File</code> relative to the source
   * directory.
   */
  private String getRelativeName(File file) {
    // We already know that the file is in the correct directory
    return "edu/pdx/cs410J/" + userId + "/" + file.getName();
  }

  /**
   * Creates a Jar file that contains the source files.  The Jar File
   * is temporary and is deleted when the program exits.
   */
  private File makeJarFileWith(Set<File> sourceFiles) throws IOException {
    String jarFileName = userName.replace(' ', '_') + "-TEMP";
    File jarFile = File.createTempFile(jarFileName, ".jar");
    if (!saveJar) {
      jarFile.deleteOnExit();

    } else {
      out.println("Saving temporary Jar file: " + jarFile);
    }

    db("Created Jar file: " + jarFile);

    Map<File, String> sourceFilesWithNames =
      sourceFiles.stream().collect(Collectors.toMap(file -> file, this::getRelativeName));

    return new JarMaker(sourceFilesWithNames, jarFile, getManifestEntries()).makeJar();
  }

  private Map<Attributes.Name, String> getManifestEntries() {
    Map<Attributes.Name, String> manifestEntries = new HashMap<>();
    manifestEntries.put(ManifestAttributes.USER_NAME, userName);
    manifestEntries.put(ManifestAttributes.USER_ID, userId);
    manifestEntries.put(ManifestAttributes.USER_EMAIL, userEmail);
    manifestEntries.put(ManifestAttributes.PROJECT_NAME, projName);
    manifestEntries.put(ManifestAttributes.SUBMISSION_COMMENT, comment);
    manifestEntries.put(ManifestAttributes.SUBMISSION_TIME, ManifestAttributes.formatSubmissionTime(submitTime));
    return manifestEntries;
  }

  /**
   * Sends the Jar file to the TA as a MIME attachment.  Also includes
   * a textual summary of the contents of the Jar file.
   */
  private void mailTA(File jarFile, Set<File> sourceFiles) throws MessagingException {
    MimeMessage message = newEmailTo(newEmailSession(debug), TA_EMAIL, "CS410J-SUBMIT " + userName + "'s " + projName);

    MimeBodyPart textPart = createTextPartOfTAEmail(sourceFiles);
    MimeBodyPart filePart = createJarAttachment(jarFile);

    Multipart mp = new MimeMultipart();
    mp.addBodyPart(textPart);
    mp.addBodyPart(filePart);

    message.setContent(mp);

    out.println("Submitting project to Grader");

    Transport.send(message);
  }

  private MimeBodyPart createJarAttachment(File jarFile) throws MessagingException {
    // Now attach the Jar file
    DataSource ds = new FileDataSource(jarFile);
    DataHandler dh = new DataHandler(ds);
    MimeBodyPart filePart = new MimeBodyPart();

    String jarFileTitle = userName.replace(' ', '_') + ".jar";

    filePart.setDataHandler(dh);
    filePart.setFileName(jarFileTitle);
    filePart.setDescription(userName + "'s " + projName);

    return filePart;
  }

  private MimeBodyPart createTextPartOfTAEmail(Set<File> sourceFiles) throws MessagingException {
    // Create the text portion of the message
    StringBuilder text = new StringBuilder();
    text.append("Student name: ").append(userName).append(" (").append(userEmail).append(")\n");
    text.append("Project name: ").append(projName).append("\n");
    DateFormat df =
      DateFormat.getDateTimeInstance(DateFormat.FULL,
        DateFormat.FULL);
    text.append("Submitted on: ").append(df.format(submitTime)).append("\n");
    if (comment != null) {
      text.append("\nComment: ").append(comment).append("\n\n");
    }
    text.append("Contents:\n");

    for (File file : sourceFiles) {
      text.append("  ").append(getRelativeName(file)).append("\n");
    }
    text.append("\n\n");

    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setContent(text.toString(), "text/plain");

    // Try not to display text as separate attachment
    textPart.setDisposition("inline");
    return textPart;
  }

  /**
   * Sends a email to the user as a receipt of the submission.
   */
  private void mailReceipt(Set<File> sourceFiles) throws MessagingException {
    MimeMessage message = newEmailTo(newEmailSession(debug), userEmail, "CS410J " + projName + " submission");

    // Create the contents of the message
    DateFormat df =
      DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
    StringBuilder text = new StringBuilder();
    text.append("On ").append(df.format(submitTime)).append("\n");
    text.append(userName).append(" (").append(userEmail).append(")\n");
    text.append("submitted the following files for ").append(projName).append(":\n");

    for (File file : sourceFiles) {
      text.append("  ").append(file.getAbsolutePath()).append("\n");
    }

    if (comment != null) {
      text.append("\nComment: ").append(comment).append("\n\n");
    }

    text.append("\n\n");
    text.append("Have a nice day.");

    // Add the text to the message and send it
    message.setText(text.toString());
    message.setDisposition("inline");

    out.println("Sending receipt to you");

    Transport.send(message);
  }

  /////////////////////////  Main Program  ///////////////////////////

  /**
   * Prints usage information about this program.
   */
  private static void usage(String s) {
    err.println("\n** " + s + "\n");
    err.println("usage: java Submit [options] args file+");
    err.println("  args are (in this order):");
    err.println("    project      What project is being submitted (Project1, Project2, etc.)");
    err.println("    student      Who is submitting the project?");
    err.println("    loginId      UNIX login id");
    err.println("    email        Student's email address");
    err.println("    file         Java source file to submit");
    err.println("  options are (options may appear in any order):");
    err.println("    -savejar           Saves temporary Jar file");
    err.println("    -smtp serverName   Name of SMTP server");
    err.println("    -verbose           Log debugging output");
    err.println("    -comment comment   Info for the Grader");
    err.println("    -submitKoans       Automatically submit koans");
    err.println("");
    err.println("Submits Java source code to the CS410J grader.");
    System.exit(1);
  }

  /**
   * Parses the command line, finds the source files, prompts the user
   * to verify whether or not the settings are correct, and then sends
   * an email to the Grader.
   */
  public static void main(String[] args) throws IOException, MessagingException {
    Submit submit = new Submit();

    // Parse the command line
    for (int i = 0; i < args.length; i++) {
      // Check for options first
      if (args[i].equals("-smtp")) {
        if (++i >= args.length) {
          usage("No SMTP server specified");
        }

        submit.setServerName(args[i]);

      } else if (args[i].equals("-verbose")) {
        submit.setDebug(true);

      } else if (args[i].equals("-savejar")) {
        submit.setSaveJar(true);

      } else if (args[i].equals("-comment")) {
        if (++i >= args.length) {
          usage("No comment specified");
        }

        submit.setComment(args[i]);
      } else if (args[i].equals("-submitKoans")) {
          submit.setKoansMode(true);

      } else if (submit.projName == null) {
        submit.setProjectName(args[i]);

      } else if (submit.userName == null) {
        submit.setUserName(args[i]);

      } else if (submit.userId == null) {
        submit.setUserId(args[i]);

      } else if (submit.userEmail == null) {
        submit.setUserEmail(args[i]);

      } else {
        // The name of a source file
        submit.addFile(args[i]);
      }
    }

    boolean submitted;

    try {
      // Make sure that user entered enough information
      submit.validate();

      submit.db("Command line successfully parsed.");

      submitted = submit.submit(true);

    } catch (IllegalStateException ex) {
      usage(ex.getMessage());
      return;
    }

    // All done.
    if (submitted) {
      out.println(submit.projName + " submitted successfully.  Thank you.");

    } else {
      out.println(submit.projName + " not submitted.");
    }
  }


  static class ManifestAttributes {

    public static final Attributes.Name USER_NAME = new Attributes.Name("Submitter-User-Name");
    public static final Attributes.Name USER_ID = new Attributes.Name("Submitter-User-Id");
    public static final Attributes.Name USER_EMAIL = new Attributes.Name("Submitter-Email");
    public static final Attributes.Name PROJECT_NAME = new Attributes.Name("Project-Name");
    public static final Attributes.Name SUBMISSION_TIME = new Attributes.Name("Submission-Time");
    public static final Attributes.Name SUBMISSION_COMMENT = new Attributes.Name("Submission-Comment");

    public static String formatSubmissionTime(Date submitTime) {
      DateFormat format = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
      return format.format(submitTime);
    }
  }

}
