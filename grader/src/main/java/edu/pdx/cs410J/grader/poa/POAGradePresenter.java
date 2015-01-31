package edu.pdx.cs410J.grader.poa;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.pdx.cs410J.grader.Assignment;
import edu.pdx.cs410J.grader.Grade;
import edu.pdx.cs410J.grader.GradeBook;
import edu.pdx.cs410J.grader.Student;

import java.util.Optional;

@Singleton
public class POAGradePresenter {
  private final EventBus bus;
  private final POAGradeView view;

  private POASubmission submission;
  private Assignment assignment;
  private Student student;
  private boolean isLate;
  private Double score;
  private CurrentGradeCalculator currentGradeCalculator;

  @Inject
  public POAGradePresenter(EventBus bus, POAGradeView view) {
    this.bus = bus;
    this.view = view;

    this.bus.register(this);

    this.view.addIsLateHandler(POAGradePresenter.this::setIsLate);
    this.view.addScoreValueHandler(this::setScoreValue);
    this.view.addRecordGradeHandler(this::publishScoreToMessageBus);
  }

  private void publishScoreToMessageBus() {
    if (this.score == null) {
      throw new IllegalStateException("No score specified");
    }

    RecordGradeEvent event = new RecordGradeEvent(this.score, this.student, this.assignment, this.isLate);
    this.bus.post(event);
    this.view.setScoreHasBeenRecorded(true);
  }

  @Subscribe
  public void determineIfSubmissionIsLate(POASubmissionSelected event) {
    this.submission = event.getSubmission();

    clearScore();
    determineIfPOAIsLate();
  }

  private void clearScore() {
    this.score = null;
    this.view.setErrorInScore(false);
    this.view.setScore("");
    this.view.setScoreHasBeenRecorded(false);
  }

  @Subscribe
  public void determineIfStudentsPOAIsLate(StudentSelectedEvent event) {
    this.student = event.getSelectedStudent();

    clearScore();
    determineIfPOAIsLate();
  }

  @Subscribe
  public void determineIfAssignmentIsLate(AssignmentSelectedEvent event) {
    this.assignment = event.getAssignment();

    determineIfPOAIsLate();

    this.view.setTotalPoints(formatTotalPoints(this.assignment.getPoints()));
    setDefaultValueOfScore();
  }

  private void setDefaultValueOfScore() {
    Optional<Double> currentGrade = this.currentGradeCalculator.getCurrentGradeFor(this.assignment, this.student);
    if (currentGrade.isPresent()) {
      this.score = currentGrade.get();
      this.view.setScore(formatTotalPoints(this.score));
      this.view.setScoreHasBeenRecorded(true);

    } else {
      this.score = this.assignment.getPoints();
      this.view.setScore(formatTotalPoints(this.score));
      this.view.setScoreHasBeenRecorded(false);
    }

    this.view.setErrorInScore(false);
  }

  @Subscribe
  public void createCurrentGradeCalculator(GradeBookLoaded event) {
    this.currentGradeCalculator = new CurrentGradeCalculator(event.getGradeBook());
  }

  private void determineIfPOAIsLate() {
    if (this.submission != null && this.assignment != null && this.student != null) {
      enableView();

      if (this.assignment.isSubmissionLate(this.submission.getSubmitTime())) {
        setIsLate(true);

      } else {
        setIsLate(false);
      }

    } else {
      disableView();
    }
  }

  private void setIsLate(boolean isLate) {
    this.isLate = isLate;
    this.view.setIsLate(isLate);
  }

  private void enableView() {
    this.view.setIsEnabled(true);
  }

  private void disableView() {
    setIsLate(false);
    this.view.setIsEnabled(false);
  }

  @VisibleForTesting
  boolean isLate() {
    return this.isLate;
  }

  @VisibleForTesting
  static String formatTotalPoints(double points) {
    return String.format("%.2f", points);
  }

  public void setScoreValue(String scoreValue) {
    if (Strings.isNullOrEmpty(scoreValue)) {
      this.score = null;
      this.view.setErrorInScore(false);
      return;
    }

    try {
      double score = Double.parseDouble(scoreValue);

      validateScoreIsBetweenZeroAndAssignmentsTotalPoints(score);

    } catch (NumberFormatException ex) {
      this.score = null;
      this.view.setErrorInScore(true);
    }
  }

  private void validateScoreIsBetweenZeroAndAssignmentsTotalPoints(double score) {
    if (score < 0.0) {
      this.score = null;
      this.view.setErrorInScore(true);

    } else if (score > this.assignment.getPoints()) {
      this.score = null;
      this.view.setErrorInScore(true);

    } else {
      this.score = score;
      this.view.setErrorInScore(false);
    }
  }

  public Double getScore() {
    return score;
  }

  private class CurrentGradeCalculator {
    private final GradeBook gradeBook;

    public CurrentGradeCalculator(GradeBook gradeBook) {
      this.gradeBook = gradeBook;
    }

    public Optional<Double> getCurrentGradeFor(Assignment assignment, Student student) {
      if (student == null) {
        return Optional.empty();
      }

      Grade grade = student.getGrade(assignment);
      if (grade != null) {
        return Optional.of(grade.getScore());
      } else {
        return Optional.empty();
      }
    }
  }
}