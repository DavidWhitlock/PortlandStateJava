package edu.pdx.cs410J.grader;

import com.google.common.annotations.VisibleForTesting;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is used to submit assignments in CS410J.  The user
 * specified his or her email address as well as the base directory
 * for his/her source files on the command line.  The directory is
 * searched recursively for files ending in .java.  Those files are
 * placed in a zip file and emailed to the grader.  A confirmation
 * email is sent to the submitter.
 *
 * More information about the JavaMail API can be found at:
 *
 * <a href="http://java.sun.com/products/javamail">
 * http://java.sun.com/products/javamail</a>
 *
 * @author David Whitlock
 * @since Fall 2000 (Refactored to use fewer static methods in Spring 2006)
 */
public class Submit extends EmailSender {

  private static final PrintWriter out = new PrintWriter(System.out, true);
  private static final PrintWriter err = new PrintWriter(System.err, true);

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
   * Should the generated zip file be saved?
   */
  private boolean saveZip = false;

  /**
   * The time at which the project was submitted
   */
  private LocalDateTime submitTime = null;

  /**
   * The names of the files to be submitted
   */
  private Set<String> fileNames = new HashSet<>();

  private boolean isSubmittingKoans = false;
  private boolean sendReceipt = true;

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
  public void setEmailServerHostName(String serverName) {
    EmailSender.serverName = serverName;
  }

  /**
   * Sets whether or not the progress of the submission should be logged.
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  /**
   * Sets whether or not the zip file generated by the submission
   * should be saved.
   */
  public void setSaveZip(boolean saveZip) {
    this.saveZip = saveZip;
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

    if (isNineDigitStudentId(userId)) {
      throw new IllegalStateException(loginIdShouldNotBeStudentId(userId));
    }

    if (looksLikeAnEmailAddress(userId)) {
      throw new IllegalStateException(loginIdShouldNotBeEmailAddress(userId));
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

  private String loginIdShouldNotBeEmailAddress(String userId) {
    return "You login id (" + userId + ") should not be an email address";
  }

  @VisibleForTesting
  boolean looksLikeAnEmailAddress(String userId) {
    return userId.contains("@");
  }

  @VisibleForTesting
  boolean isNineDigitStudentId(String userId) {
    return userId.matches("^[0-9]{9}$");
  }

  private String loginIdShouldNotBeStudentId(String userId) {
    return "Your login id (" + userId + ") should not be your 9-digit student id";
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
    // Recursively search the source directory for .java files
    Set<File> sourceFiles = searchForSourceFiles(fileNames);

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
    this.submitTime = LocalDateTime.now();

    // Create a temporary zip file to hold the source files
    File zipFile = makeZipFileWith(sourceFiles);

    // Send the zip file as an email attachment to the TA
    mailTA(zipFile, sourceFiles);

    if (sendReceipt) {
      mailReceipt(sourceFiles);
    }

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
    // Files should be sorted by name
    SortedSet<File> files =
      new TreeSet<>((o1, o2) -> o1.toString().compareTo(o2.toString()));
    populateWithFilesFromSubdirectories(files, fileNames);

    files.removeIf((file) -> !canBeSubmitted(file));

    return files;
  }

  private void populateWithFilesFromSubdirectories(SortedSet<File> allFiles, Set<String> fileNames) {
    populateWithFilesFromSubdirectories(allFiles, fileNames.stream().map(name -> new File(name).getAbsoluteFile()));
  }

  private void populateWithFilesFromSubdirectories(SortedSet<File> allFiles, Stream<File> files) {
    files.forEach((file) -> {
      if (file.isDirectory()) {
        populateWithFilesFromSubdirectories(allFiles, Arrays.stream(file.listFiles()));

      } else {
        allFiles.add(file);
      }
    });

  }

  private boolean canBeSubmitted(File file) {
    if (!file.exists()) {
      err.println("** Not submitting file " + file +
        " because it does not exist");
      return false;
    }

    // Is the file on the "no submit" list?
    List<String> noSubmit = fetchListOfFilesThatCanNotBeSubmitted();
    String name = file.getName();
    if (noSubmit.contains(name)) {
      err.println("** Not submitting file " + file +
        " because it is on the \"no submit\" list");
      return false;
    }

    // Does the file name end in .java?
    if (!name.endsWith(".java")) {
      err.println("** Not submitting file " + file +
        " because does end in \".java\"");
      return false;
    }

    // Verify that file is in the correct directory.
    if (!isInAKoansDirectory(file) && !isInEduPdxCs410JDirectory(file)) {
      err.println("** Not submitting file " + file +
        ": it does not reside in a directory named " +
        "edu" + File.separator + "pdx" + File.separator +
        "cs410J" + File.separator + userId + " (or in one of the koans directories)");
      return false;
    }

    return true;
  }

  private boolean isInAKoansDirectory(File file) {
    boolean isInAKoansDirectory = hasParentDirectories(file, "beginner") ||
      hasParentDirectories(file, "intermediate") ||
      hasParentDirectories(file, "advanced") ||
      hasParentDirectories(file, "java7") ||
      hasParentDirectories(file, "java8");
    if (isInAKoansDirectory) {
      this.isSubmittingKoans = true;
      db(file + " is in a koans directory");
    }
    return isInAKoansDirectory;
  }

  @VisibleForTesting
  boolean isInEduPdxCs410JDirectory(File file) {
    boolean isInEduPdxCs410JDirectory = hasParentDirectories(file, userId, "cs410J", "pdx", "edu");
    if (isInEduPdxCs410JDirectory) {
      db(file + " is in the edu/pdx/cs410J directory");
    }
    return isInEduPdxCs410JDirectory;
  }

  private boolean hasParentDirectories(File file, String... parentDirectoryNames) {
    File parent = file.getParentFile();

    // Skip over subpackages
    while (parent != null && !parent.getName().equals(parentDirectoryNames[0])) {
      parent = parent.getParentFile();
    }

    for (String parentDirectoryName : parentDirectoryNames) {
      if (parent == null || !parent.getName().equals(parentDirectoryName)) {
        return false;

      } else {
        parent = parent.getParentFile();
      }
    }

    return true;
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
    if (!wasMainProjectClassSubmitted && !this.isSubmittingKoans) {
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
    if (isSubmittingKoans) {
      return file.getParentFile().getName() + "/" + file.getName();

    } else {
      // We already know that the file is in the correct directory
      return "edu/pdx/cs410J/" + userId + "/" + file.getName();
    }
  }

  /**
   * Creates a Zip file that contains the source files.  The Zip File
   * is temporary and is deleted when the program exits.
   */
  private File makeZipFileWith(Set<File> sourceFiles) throws IOException {
    String zipFileName = userName.replace(' ', '_') + "-TEMP";
    File zipFile = File.createTempFile(zipFileName, ".zip");
    if (!saveZip) {
      zipFile.deleteOnExit();

    } else {
      out.println("Saving temporary Zip file: " + zipFile);
    }

    db("Created Zip file: " + zipFile);

    Map<File, String> sourceFilesWithNames =
      sourceFiles.stream().collect(Collectors.toMap(file -> file, this::getRelativeName));

    return new ZipFileOfFilesMaker(sourceFilesWithNames, zipFile, getManifestEntries()).makeZipFile();
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
   * Sends the Zip file to the TA as a MIME attachment.  Also includes
   * a textual summary of the contents of the Zip file.
   */
  private void mailTA(File zipFile, Set<File> sourceFiles) throws MessagingException {
    MimeMessage message = newEmailTo(newEmailSession(debug), this.userEmail, TA_EMAIL, "CS410J-SUBMIT " + userName + "'s " + projName);

    MimeBodyPart textPart = createTextPartOfTAEmail(sourceFiles);
    MimeBodyPart filePart = createZipAttachment(zipFile);

    Multipart mp = new MimeMultipart();
    mp.addBodyPart(textPart);
    mp.addBodyPart(filePart);

    message.setContent(mp);

    out.println("Submitting project to Grader");

    Transport.send(message);
  }

  private MimeBodyPart createZipAttachment(File zipFile) throws MessagingException {
    // Now attach the Zip file
    DataSource ds = new FileDataSource(zipFile) {
      @Override
      public String getContentType() {
        return "application/zip";
      }
    };
    DataHandler dh = new DataHandler(ds);
    MimeBodyPart filePart = new MimeBodyPart();

    String zipFileTitle = userName.replace(' ', '_') + ".zip";

    filePart.setDataHandler(dh);
    filePart.setFileName(zipFileTitle);
    filePart.setDescription(userName + "'s " + projName);

    return filePart;
  }

  private MimeBodyPart createTextPartOfTAEmail(Set<File> sourceFiles) throws MessagingException {
    // Create the text portion of the message
    StringBuilder text = new StringBuilder();
    text.append("Student name: ").append(userName).append(" (").append(userEmail).append(")\n");
    text.append("Project name: ").append(projName).append("\n");
    text.append("Submitted on: ").append(humanReadableSubmitDate()).append("\n");
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

  private String humanReadableSubmitDate() {
    return submitTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM));
  }

  /**
   * Sends a email to the user as a receipt of the submission.
   */
  private void mailReceipt(Set<File> sourceFiles) throws MessagingException {
    MimeMessage message = newEmailTo(newEmailSession(debug), this.userEmail, "CS410J " + projName + " submission");

    // Create the contents of the message
    StringBuilder text = new StringBuilder();
    text.append("On ").append(humanReadableSubmitDate()).append("\n");
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

    out.println("Sending receipt to you at " + userEmail);

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
    err.println("    file         Java source file (or all files in a directory) to submit");
    err.println("  options are (options may appear in any order):");
    err.println("    -savezip           Saves temporary Zip file");
    err.println("    -smtp serverName   Name of SMTP server");
    err.println("    -verbose           Log debugging output");
    err.println("    -comment comment   Info for the Grader");
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

        submit.setEmailServerHostName(args[i]);

      } else if (args[i].equals("-verbose")) {
        submit.setDebug(true);

      } else if (args[i].equals("-savezip")) {
        submit.setSaveZip(true);

      } else if (args[i].equals("-comment")) {
        if (++i >= args.length) {
          usage("No comment specified");
        }

        submit.setComment(args[i]);

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

  public void setSendReceipt(boolean sendReceipt) {
    this.sendReceipt = sendReceipt;

  }

  static class ManifestAttributes {

    public static final Attributes.Name USER_NAME = new Attributes.Name("Submitter-User-Name");
    public static final Attributes.Name USER_ID = new Attributes.Name("Submitter-User-Id");
    public static final Attributes.Name USER_EMAIL = new Attributes.Name("Submitter-Email");
    public static final Attributes.Name PROJECT_NAME = new Attributes.Name("Project-Name");
    public static final Attributes.Name SUBMISSION_TIME = new Attributes.Name("Submission-Time");
    public static final Attributes.Name SUBMISSION_COMMENT = new Attributes.Name("Submission-Comment");

    private static final DateTimeFormatter LEGACY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static String formatSubmissionTime(LocalDateTime submitTime) {
      return submitTime.format(DATE_TIME_FORMATTER);
    }

    @VisibleForTesting
    static String formatSubmissionTimeUsingLegacyFormat(LocalDateTime submitTime) {
      return submitTime.format(LEGACY_DATE_TIME_FORMATTER);
    }

    public static LocalDateTime parseSubmissionTime(String string) {
      try {
        return LocalDateTime.parse(string, DATE_TIME_FORMATTER);

      } catch (DateTimeParseException ex) {
        return LocalDateTime.parse(string, LEGACY_DATE_TIME_FORMATTER);
      }
    }
  }

}
