package edu.pdx.cs410J.grader;

import java.util.*;

public class GradesFromD2L {
  private List<D2LStudent> students = new ArrayList<>();

  public static D2LStudentBuilder newStudent() {
    return new D2LStudentBuilder();
  }

  public void addStudent(D2LStudent student) {
    this.students.add(student);
  }

  public Student findStudentInGradebookForD2LStudent(D2LStudent d2lStudent, GradeBook book) {
    for (String studentId : book.getStudentIds()) {
      Student student = book.getStudent(studentId);
      if (haveSameD2LId(d2lStudent, student)) {
        return student;

      } else if (studentIdIsSameAsD2LId(d2lStudent, student)) {
        student.setD2LId(d2lStudent.getD2lId());
        return student;

      } else if (haveSameFirstAndLastNameIgnoringCase(d2lStudent, student)) {
        student.setD2LId(d2lStudent.getD2lId());
        return student;
      }
    }

    return null;
  }

  private boolean studentIdIsSameAsD2LId(D2LStudent d2lStudent, Student student) {
    return d2lStudent.getD2lId().equals(student.getId());
  }

  private boolean haveSameFirstAndLastNameIgnoringCase(D2LStudent d2lStudent, Student student) {
    return d2lStudent.getFirstName().equalsIgnoreCase(student.getFirstName()) &&
      d2lStudent.getLastName().equalsIgnoreCase(student.getLastName());
  }

  private boolean haveSameD2LId(D2LStudent d2lStudent, Student student) {
    return d2lStudent.getD2lId().equals(student.getD2LId());
  }

  public List<D2LStudent> getStudents() {
    return this.students;
  }

  public Optional<Assignment> findAssignmentInGradebookForD2lQuiz(String quizName, GradeBook gradebook) {
    return Optional.ofNullable(gradebook.getAssignment(quizName));
  }

  static class D2LStudent {
    private final String firstName;
    private final String lastName;
    private final String d2lId;
    private Map<String, Double> scores = new HashMap<>();

    public D2LStudent(String firstName, String lastName, String d2lId) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.d2lId = d2lId;
    }

    public String getD2lId() {
      return d2lId;
    }

    public String getFirstName() {
      return firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setScore(String quizName, double score) {
      this.scores.put(quizName, score);
    }

    public Double getScore(String quizName) {
      return this.scores.get(quizName);
    }

    public Iterable<String> getQuizNames() {
      return this.scores.keySet();
    }
  }

  static class D2LStudentBuilder {
    private String firstName;
    private String lastName;
    private String d2lId;

    private D2LStudentBuilder() {

    }

    public D2LStudentBuilder setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public D2LStudentBuilder setLastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public D2LStudentBuilder setD2LId(String d2lId) {
      this.d2lId = d2lId;
      return this;
    }

    public D2LStudent create() {
      if (firstName == null) {
        throw new IllegalStateException("Missing first name");
      }

      if (lastName == null) {
        throw new IllegalStateException("Missing last name");
      }

      if (d2lId == null) {
        throw new IllegalStateException("Missing d2l Id");
      }

      return new D2LStudent(firstName, lastName, d2lId);
    }
  }
}
