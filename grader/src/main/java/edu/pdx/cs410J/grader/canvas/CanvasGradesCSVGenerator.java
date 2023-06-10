package edu.pdx.cs410J.grader.canvas;

import com.opencsv.CSVWriter;
import edu.pdx.cs410J.grader.gradebook.Assignment;
import edu.pdx.cs410J.grader.gradebook.Grade;
import edu.pdx.cs410J.grader.gradebook.GradeBook;
import edu.pdx.cs410J.grader.gradebook.Student;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CanvasGradesCSVGenerator implements CanvasGradesCSVColumnNames {
  private final Writer writer;

  public CanvasGradesCSVGenerator(Writer writer) {
    this.writer = writer;
  }

  public void generate(GradeBook book) throws IOException {
    try (
      CSVWriter csv = new CSVWriter(writer)
    ) {
      writeHeaderRow(book, csv);
      writePossiblePointsRow(book, csv);
      writeStudentRows(book, csv);
      csv.flush();
    }
  }

  private void writePossiblePointsRow(GradeBook book, CSVWriter csv) {
    List<String> line = new ArrayList<>();
    line.add("Possible Points");
    line.add("");  // Login ID
    line.add("");  // Section

    addCellsForEachAssignment(book, line, this::getPossiblePoints);

    csv.writeNext(line.toArray(new String[0]));

  }

  private String getPossiblePoints(Assignment assignment) {
    return String.valueOf(assignment.getPoints());
  }

  private void writeStudentRows(GradeBook book, CSVWriter csv) {
    book.studentsStream().forEach(student -> writeStudentRow(book, student, csv));
  }

  private void writeStudentRow(GradeBook book, Student student, CSVWriter csv) {
    csv.writeNext(getStudentLine(book, student));
  }

  private String[] getStudentLine(GradeBook book, Student student) {
    List<String> line = new ArrayList<>();
    line.add(getStudentName(student));
    line.add(student.getCanvasId());
    line.add(book.getSectionName(student.getEnrolledSection()));

    addCellsForEachAssignment(book, line, assignment -> getGrade(student, assignment));

    return line.toArray(new String[0]);
  }

  private String getGrade(Student student, Assignment assignment) {
    Grade grade = student.getGrade(assignment);
    if (grade == null) {
      return null;

    } else {
      return String.valueOf(grade.getScore());
    }
  }

  private String getStudentName(Student student) {
    return student.getLastName() + ", " + student.getFirstName();
  }

  private void writeHeaderRow(GradeBook book, CSVWriter csv) {
    List<String> line = new ArrayList<>();
    line.add(STUDENT_COLUMN);
    line.add(ID_COLUMN);
    line.add(SECTION_COLUMN);

    addCellsForEachAssignment(book, line, this::getAssignmentHeaderCell);

    csv.writeNext(line.toArray(new String[0]));
  }

  private void addCellsForEachAssignment(GradeBook book, List<String> line, Function<Assignment, String> function) {
    book.getAssignmentNames().stream().map(book::getAssignment).forEach(assignment -> line.add(function.apply(assignment)));
  }

  private String getAssignmentHeaderCell(Assignment assignment) {
    return assignment.getName() + " (" + assignment.getCanvasId() + ")" ;
  }
}
