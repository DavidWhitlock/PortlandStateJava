package edu.pdx.cs410J.grader.canvas;

import edu.pdx.cs410J.ParserException;
import edu.pdx.cs410J.grader.gradebook.GradeBook;
import edu.pdx.cs410J.grader.gradebook.XmlGradeBookParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public class GradesFromCanvasExporter {

  public static void exportGradesToCanvasCsv(FileWriter canvasCsv, GradeBook gradebook) throws IOException {
    CanvasGradesCSVGenerator generator = new CanvasGradesCSVGenerator(canvasCsv);
    generator.generate(gradebook);
  }

  public static void main(String[] args) throws IOException, ParserException {
    String canvasCsvFileName = null;
    String gradeBookFileName = null;

    for (String arg : args) {
      if (canvasCsvFileName == null) {
        canvasCsvFileName = arg;

      } else if (gradeBookFileName == null) {
        gradeBookFileName = arg;

      } else {
        usage("Extraneous command line argument: " + arg);
      }
    }

    if (canvasCsvFileName == null) {
      usage("Missing Canvas CSV file name");
      return;
    }

    if (gradeBookFileName == null) {
      usage("Missing grade book file name");
    }

    GradeBook gradebook = parseGradeBook(gradeBookFileName);

    FileWriter writer = new FileWriter(canvasCsvFileName);

    exportGradesToCanvasCsv(writer, gradebook);
  }

  private static GradeBook parseGradeBook(String gradeBookFileName) throws IOException, ParserException {
    File gradeBookFile = new File(gradeBookFileName);
    if (!gradeBookFile.exists()) {
      usage("Grade Book file \"" + gradeBookFile + "\" does not exist");
    }

    XmlGradeBookParser parser = new XmlGradeBookParser(gradeBookFile);
    return parser.parse();
  }

  private static void usage(String message) {
    PrintStream err = System.err;

    err.println("+++ " + message);
    err.println();
    err.println("usage: java GradesFromCanvasImporter canvasGradesCsvFileName gradeBookFileName");
    err.println("    canvasGradesCsvFileName       Name of the CSV grades file exported from Canvas");
    err.println("    gradeBookFileName             Gradebook file");
    err.println();
    err.println("Imports grades from Canvas into a gradebook");
    err.println();

    System.exit(1);
  }
}
