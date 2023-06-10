package edu.pdx.cs410J.grader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.annotations.VisibleForTesting;
import edu.pdx.cs410J.grader.canvas.GradesFromCanvasExporter;
import edu.pdx.cs410J.grader.canvas.GradesFromCanvasImporter;
import edu.pdx.cs410J.grader.gradebook.ui.GradeBookGUI;
import edu.pdx.cs410J.grader.poa.ui.PlanOfAttackGrader;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GraderTools {

  public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    String tool = null;
    List<String> toolArgs = new ArrayList<>();
    for (String arg : args) {
      if (arg.equals("-debug")) {
        setLoggingLevelToDebug();

      } else if (tool == null) {
        tool = arg;

      } else {
        toolArgs.add(arg);
      }
    }

    if (tool == null) {
      usage("Missing tool");
    }


    invokeMainMethod(getToolClass(tool), toolArgs.toArray(new String[toolArgs.size()]));
  }

  @VisibleForTesting
  static void setLoggingLevelToDebug() {
    Logger logger = (Logger) LoggerFactory.getLogger("edu.pdx.cs410J.grader");
    logger.setLevel(Level.DEBUG);
  }

  private static Class getToolClass(String tool) {
    switch (tool) {
      case "gradebook":
        return GradeBookGUI.class;

      case "fetch":
        return FetchAndProcessGraderEmail.class;

      case "importFromCanvas" :
        return GradesFromCanvasImporter.class;

      case "exportToCanvasCsv" :
        return GradesFromCanvasExporter.class;

      case "importFromProjectReports" :
        return ProjectGradesImporter.class;

      case "mailFileToStudent" :
        return StudentFileMailer.class;

      case "gradePOAs":
        return PlanOfAttackGrader.class;

      case "generateGradeSummary":
        return SummaryReport.class;

      case "htmlForSurveyResults":
        return SurveyResponsesFromD2LGenerator.class;

      case "fixAndroidZips":
        return AndroidZipFixer.class;

      case "projectTimeEstimates":
        return ProjectTimeEstimatesSummary.class;

      default:
        usage("Unknown tool: " + tool);
        return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static void invokeMainMethod(Class aClass, String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method main = aClass.getMethod("main", args.getClass());
    main.invoke(aClass, new Object[] { args });

  }

  private static void usage(String message) {
    PrintStream err = System.err;
    err.println("** " + message);
    err.println();
    err.println("Executes one of the Grader tools");
    err.println();
    err.println("usage: GraderTools tool toolArg*");
    err.println("  tool                        The tool to execute");
    err.println("    gradebook                 The Grade Book GUI");
    err.println("    fetch                     Fetch student surveys or projects from the Grader's");
    err.println("                              emails account");
    err.println("    importFromCanvas          Import grades from a Canvas CSV");
    err.println("    importFromProjectReports  Import grades from graded project reports");
    err.println("    mailFileToStudent         Email text files to students");
    err.println("    gradePOAs                 Tool for downloading and grading POAs");
    err.println("    generateGradeSummary      Generate grade summary report for one or more students");
    err.println("    htmlForSurveyResults      Generate an html file for the responses to a D2L survey");
    err.println("    fixAndroidZips            Fix zip files for the Android project to work with grading script");
    err.println("    projectTimeEstimates      Generate markdown that summarizes the estimated project hours");
    err.println("  toolArg                     A command line argument to send to the tool");
    err.println();

    System.exit(1);
  }
}
