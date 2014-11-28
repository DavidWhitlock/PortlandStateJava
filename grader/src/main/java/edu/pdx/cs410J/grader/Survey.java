package edu.pdx.cs410J.grader;

import org.w3c.dom.Document;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.*;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.function.Consumer;

/**
 * This program presents a survey that all students in CS410J should
 * answer.  It emails the results of the survey to the TA and emails a
 * receipt back to the student.
 */
public class Survey extends EmailSender {
  private static PrintWriter out = new PrintWriter(System.out, true);
  private static PrintWriter err = new PrintWriter(System.err, true);
  private static BufferedReader in = 
    new BufferedReader(new InputStreamReader(System.in));

  private static final String TA_EMAIL = "sjavata@gmail.com";

  /**
   * Returns a textual summary of a <code>Student</code>
   */
  private static String getSummary(Student student) {
    StringBuilder sb = new StringBuilder();
    sb.append("Name: ").append(student.getFullName()).append("\n");
    sb.append("UNIX login: ").append(student.getId()).append("\n");
    if (student.getEmail() != null) {
      sb.append("Email: ").append(student.getEmail()).append("\n");
    }
    if (student.getSsn() != null) {
      sb.append("Student id: ").append(student.getSsn()).append("\n");
    }
    if (student.getMajor() != null) {
      sb.append("Major: ").append(student.getMajor()).append("\n");
    }
    return sb.toString();
  }

  /**
   * Ask the student a question and return his response
   */
  private static String ask(String question) {
    out.print(question + " ");
    out.flush();

    String response = null;
    try {
      response = in.readLine();

    } catch (IOException ex) {
      err.println("** IOException while reading response: " + ex);
      System.exit(1);
    }

    return response;
  }

  /**
   * Prints out usage information for this program
   */
  private static void usage() {
    err.println("\nusage: java Survey [options]");
    err.println("  where [options] are:");
    err.println("  -mailServer serverName    Mail server to send mail");
    err.println("\n");
    System.exit(1);
  }

  public static void main(String[] args) {
    parseCommandLine(args);

    printIntroduction();

    Student student = gatherStudentInformation();

    String learn = ask("What do you hope to learn in CS410J?");
    String comments = ask("What else would you like to tell me?");

    addNotesToStudent(student, learn, comments);

    emailSurveyResults(student, learn, comments);
  }

  private static void addNotesToStudent(Student student, String learn, String comments) {
    if (isNotEmpty(learn)) {
      student.addNote(student.getFullName() + " would like to learn " + learn);
    }

    if (isNotEmpty(comments)) {
      student.addNote(student.getFullName() + " has these comments: " + comments);
    }
  }

  private static Student gatherStudentInformation() {
    String firstName = ask("What is your first name?");
    String lastName = ask("What is your last name?");
    String nickName = ask("What is your nickname? (Leave blank if " +
                          "you don't have one)");
    String id = ask("MANDATORY: What is your UNIX login id?");

    if (isEmpty(id)) {
      err.println("** You must enter a valid UNIX login id");
      System.exit(1);
    }

    Student student = new Student(id);
    setValueIfNotEmpty(firstName, student::setFirstName);
    setValueIfNotEmpty(lastName, student::setLastName);
    setValueIfNotEmpty(nickName, student::setNickName);

    askQuestionAndSetValue("What is your email address (doesn't have to be PSU)?", student::setEmail);
    askQuestionAndSetValue("What is your student id (XXXXXXXXX)?", student::setSsn);
    askQuestionAndSetValue("What is your major?", student::setMajor);

    return student;
  }

  private static void askQuestionAndSetValue(String question, Consumer<String> setter) {
    String answer = ask(question);
    setValueIfNotEmpty(answer, setter);
  }

  static void setValueIfNotEmpty(String string, Consumer<String> setter) {
    if (isNotEmpty(string)) {
      setter.accept(string);
    }
  }

  private static void emailSurveyResults(Student student, String learn, String comments) {
    String summary = verifyInformation(student);

    // Email the results of the survey to the TA and CC the student

    MimeMessage message = createEmailMessage(student);
    MimeBodyPart textPart = createEmailText(learn, comments, summary);
    MimeBodyPart xmlFilePart = createXmlAttachment(student);
    addAttachmentsAndSendEmail(message, textPart, xmlFilePart);
  }

  private static void addAttachmentsAndSendEmail(MimeMessage message, MimeBodyPart textPart, MimeBodyPart filePart) {
    // Finally, add the attachments to the message and send it
    try {
      Multipart mp = new MimeMultipart();
      mp.addBodyPart(textPart);
      mp.addBodyPart(filePart);

      message.setContent(mp);

      Transport.send(message);

    } catch (MessagingException ex) {
      err.println("** Exception while adding parts and sending: " +
		  ex);
      System.exit(1);
    }
  }

  private static MimeBodyPart createXmlAttachment(Student student) {
    byte[] xmlBytes = getXmlBytes(student);

    DataSource ds = new ByteArrayDataSource(xmlBytes);
    DataHandler dh = new DataHandler(ds);
    MimeBodyPart filePart = new MimeBodyPart();
    try {
      String xmlFileTitle = student.getId() + ".xml";

      filePart.setDataHandler(dh);
      filePart.setFileName(xmlFileTitle);
      filePart.setDescription("XML file for " + student.getFullName());

    } catch (MessagingException ex) {
      err.println("** Exception with file part: " + ex);
      System.exit(1);
    }
    return filePart;
  }

  private static MimeBodyPart createEmailText(String learn, String comments, String summary) {
    // Create the text portion of the message
    StringBuilder text = new StringBuilder();
    text.append("Results of CS410J Survey:\n\n");
    text.append(summary);
    text.append("\n\nWhat do you hope to learn in CS410J?\n\n");
    text.append(learn);
    text.append("\n\nIs there anything else you'd like to tell me?\n\n");
    text.append(comments);
    text.append("\n\nThanks for filling out this survey!\n\nDave");

    MimeBodyPart textPart = new MimeBodyPart();
    try {
      textPart.setContent(text.toString(), "text/plain");

      // Try not to display text as separate attachment
      textPart.setDisposition("inline");

    } catch (MessagingException ex) {
      err.println("** Exception with text part: " + ex);
      System.exit(1);
    }
    return textPart;
  }

  private static MimeMessage createEmailMessage(Student student) {
    MimeMessage message = null;
    try {
      message = newEmailTo(newEmailSession(false), TA_EMAIL, "CS410J Survey for " + student.getFullName());

      String studentEmail = student.getEmail();
      if (studentEmail != null) {
        InternetAddress[] cc = {new InternetAddress(studentEmail)};
        message.setRecipients(Message.RecipientType.CC, cc);
      }

    } catch (AddressException ex) {
      err.println("** Exception with email address " + ex);
      System.exit(1);

    } catch (MessagingException ex) {
      err.println("** Exception while setting recipients email:" +
                  ex);
      System.exit(1);
    }
    return message;
  }

  private static byte[] getXmlBytes(Student student) {
    // Create a temporary "file" to hold the Student's XML file.  We
    // use a byte array so that potentially sensitive data (SSN, etc.)
    // is not written to disk
    byte[] bytes = null;

    Document xmlDoc = XmlDumper.toXml(student);


    try {
      bytes = XmlHelper.getBytesForXmlDocument(xmlDoc);

    } catch (TransformerException ex) {
      ex.printStackTrace(System.err);
      System.exit(1);
    }
    return bytes;
  }

  private static String verifyInformation(Student student) {
    String summary = getSummary(student);

    out.println("\nYou entered the following information about " +
                "yourself:\n");
    out.println(summary);

    String verify = ask("\nIs this information correct (y/n)?");
    if (!verify.equals("y")) {
      err.println("** Not sending information.  Exiting.");
      System.exit(1);
    }
    return summary;
  }

  private static boolean isNotEmpty(String string) {
    return string != null && !string.equals("");
  }

  private static boolean isEmpty(String string) {
    return string == null || string.equals("");
  }

  private static void printIntroduction() {
    // Ask the student a bunch of questions
    out.println("\nWelcome to the CS410J Survey Program.  I'd like " +
                "to ask you a couple of");
    out.println("questions about yourself.  Except for your UNIX " +
                "login id, no question");
    out.println("is mandatory.  Your answers will be emailed to " +
                "the TA and a receipt");
    out.println("will be emailed to you.");
    out.println("");
  }

  private static void parseCommandLine(String[] args) {
    // Parse the command line
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-mailServer")) {
        if (++i >= args.length) {
          err.println("** Missing mail server name");
          usage();
        }

        serverName = args[i];

      } else if (args[i].startsWith("-")) {
        err.println("** Unknown command line option: " + args[i]);
        usage();

      } else {
        err.println("** Spurious command line: " + args[i]);
        usage();
      }
    }
  }

  /**
   * A <code>DataSource</code> that is built around a byte array
   * containing XML data.
   *
   * @since Winter 2004
   */
  static class ByteArrayDataSource implements DataSource {

    /** The byte array containing the XML data */
    private byte[] bytes;

    /**
     * Creates a new <code>ByteArrayDataSource</code> for a given
     * <code>byte</code> array.
     */
    public ByteArrayDataSource(byte[] bytes) {
      this.bytes = bytes;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(this.bytes);
    }

    /**
     * We do not support writing to a
     * <code>ByteArrayDataSource</code>.
     *
     * @throws UnsupportedOperationException
     *         If this method is invoked
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
      String s = "We do not support writing to a ByteArrayDataSource";
      throw new UnsupportedOperationException(s);
    }

    /**
     * The content type for a <code>ByteArrayDataSource</code> is
     * <code>text/xml</code>.
     */
    @Override
    public String getContentType() {
      return "text/xml";
    }

    @Override
    public java.lang.String getName() {
      return "XML Data";
    }

  }

}
