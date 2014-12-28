package edu.pdx.cs410J.grader.poa;

import com.google.common.eventbus.Subscribe;
import edu.pdx.cs410J.grader.Assignment;
import edu.pdx.cs410J.grader.GradeBook;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;

import static edu.pdx.cs410J.grader.poa.POAAssignmentsView.AssignmentSelectedHandler;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class POAAssignmentsPresenterTest extends EventBusTestCase {

  private POAAssignmentsView view;
  private GradeBook book;
  private Assignment assignment0;
  private Assignment assignment1;
  private Assignment assignment2;

  @Override
  public void setUp() {
    super.setUp();

    this.view = mock(POAAssignmentsView.class);

    new POAAssignmentsPresenter(this.bus, this.view);

    book = new GradeBook("Test Grade Book");
    assignment0 = new Assignment("POA 0", 1.0).setType(Assignment.AssignmentType.POA);
    book.addAssignment(assignment0);
    assignment1 = new Assignment("POA 1", 1.0).setType(Assignment.AssignmentType.POA);
    book.addAssignment(assignment1);
    assignment2 = new Assignment("POA 2", 1.0).setType(Assignment.AssignmentType.POA);
    book.addAssignment(assignment2);
    book.addAssignment(new Assignment("Quiz 0", 3.0).setType(Assignment.AssignmentType.QUIZ));
  }

  @Test
  public void assignmentListPopulatedInViewWhenGradeBookLoaded() {
    this.bus.post(new GradeBookLoaded(book));

    verify(this.view).setAssignments(Arrays.asList("POA 0", "POA 1", "POA 2"));
    verify(this.view).setSelectedAssignment(0);
  }

  @Test
  public void assignmentSelectedFireWhenAssignmentSelectedInView() {
    ArgumentCaptor<AssignmentSelectedHandler> viewHandler = ArgumentCaptor.forClass(AssignmentSelectedHandler.class);
    verify(this.view).addAssignmentSelectedHandler(viewHandler.capture());

    AssignmentSelectedEventHandler eventHandler = mock(AssignmentSelectedEventHandler.class);
    this.bus.register(eventHandler);

    this.bus.post(new GradeBookLoaded(book));

    viewHandler.getValue().assignmentSelected(1);

    ArgumentCaptor<AssignmentSelectedEvent> event = ArgumentCaptor.forClass(AssignmentSelectedEvent.class);
    verify(eventHandler).handle(event.capture());

    assertThat(event.getValue().getAssignment(), equalTo(assignment1));
  }

  private interface AssignmentSelectedEventHandler {
    @Subscribe
    void handle(AssignmentSelectedEvent event);
  }

  @Test
  public void initiallySelectAssignmentWhoseDueDateHasMostRecentlyPassed() {
    assignment0.setDueDate(LocalDateTime.now().minusDays(3));
    assignment1.setDueDate(LocalDateTime.now().minusDays(2));
    assignment2.setDueDate(LocalDateTime.now().plusDays(2));

    this.bus.post(new GradeBookLoaded(book));

    verify(this.view).setSelectedAssignment(1);
  }


  @Test
  public void selectAssignmentWhenPOASubmittedWithAssignmentNumberInSubject() {
    AssignmentSelectedEventHandler eventHandler = mock(AssignmentSelectedEventHandler.class);
    this.bus.register(eventHandler);

    this.bus.post(new GradeBookLoaded(book));

    this.bus.post(new POASubmission("POA for Project 2", "me", LocalDateTime.now()));

    verify(this.view).setSelectedAssignment(2);

    ArgumentCaptor<AssignmentSelectedEvent> event = ArgumentCaptor.forClass(AssignmentSelectedEvent.class);
    verify(eventHandler).handle(event.capture());

    assertThat(event.getValue().getAssignment(), equalTo(assignment2));
  }

  @Test
  public void testFindNumbersInString() {
    assertThat(POAAssignmentsPresenter.findNumbersInString("POA for Project 1"), hasItem("1"));
    assertThat(POAAssignmentsPresenter.findNumbersInString("Project 5 POA"), hasItem("5"));
    assertThat(POAAssignmentsPresenter.findNumbersInString("POA for Project 42"), hasItem("42"));
    assertThat(POAAssignmentsPresenter.findNumbersInString("3 for Project 42"), hasItems("3", "42"));
  }

  @Test
  public void populateDueDateInViewWhenGradeBookLoaded() {
    assignment0.setDueDate(LocalDateTime.now().minusDays(3));
    assignment1.setDueDate(LocalDateTime.now().minusDays(2));
    assignment2.setDueDate(LocalDateTime.now().plusDays(2));

    this.bus.post(new GradeBookLoaded(book));

    verify(this.view).setSelectedAssignmentDueDate(POAAssignmentsPresenter.formatDueDate(assignment1));
  }

  @Test
  public void populateDueDateInViewWhenAssignmentSelected() {
    assignment0.setDueDate(LocalDateTime.now().minusDays(2));
    assignment1.setDueDate(LocalDateTime.now().minusDays(3));

    ArgumentCaptor<AssignmentSelectedHandler> viewHandler = ArgumentCaptor.forClass(AssignmentSelectedHandler.class);
    verify(this.view).addAssignmentSelectedHandler(viewHandler.capture());

    AssignmentSelectedEventHandler eventHandler = mock(AssignmentSelectedEventHandler.class);
    this.bus.register(eventHandler);

    this.bus.post(new GradeBookLoaded(book));
    verify(this.view).setSelectedAssignmentDueDate(POAAssignmentsPresenter.formatDueDate(assignment0));

    viewHandler.getValue().assignmentSelected(1);

    verify(this.view).setSelectedAssignmentDueDate(POAAssignmentsPresenter.formatDueDate(assignment1));
  }

  @Test
  public void clearDueDateInViewWhenAssignmentHasNoDueDate() {
    assignment0.setDueDate(LocalDateTime.now().minusDays(2));
    assignment1.setDueDate(null);

    ArgumentCaptor<AssignmentSelectedHandler> viewHandler = ArgumentCaptor.forClass(AssignmentSelectedHandler.class);
    verify(this.view).addAssignmentSelectedHandler(viewHandler.capture());

    AssignmentSelectedEventHandler eventHandler = mock(AssignmentSelectedEventHandler.class);
    this.bus.register(eventHandler);

    this.bus.post(new GradeBookLoaded(book));
    verify(this.view).setSelectedAssignmentDueDate(POAAssignmentsPresenter.formatDueDate(assignment0));

    viewHandler.getValue().assignmentSelected(1);

    verify(this.view).setSelectedAssignmentDueDate("");
  }

}
