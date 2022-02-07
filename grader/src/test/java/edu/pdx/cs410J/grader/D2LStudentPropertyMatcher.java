package edu.pdx.cs410J.grader;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class D2LStudentPropertyMatcher extends TypeSafeMatcher<List<GradesFromCanvas.CanvasStudent>> {

  private final String expectedValue;
  private final Function<GradesFromCanvas.CanvasStudent, String> accessor;
  private final String propertyName;

  public D2LStudentPropertyMatcher(Function<GradesFromCanvas.CanvasStudent, String> accessor, String propertyName, String expectedValue) {
    this.expectedValue = expectedValue;
    this.accessor = accessor;
    this.propertyName = propertyName;
  }

  @Override
  protected boolean matchesSafely(List<GradesFromCanvas.CanvasStudent> students) {
    for (GradesFromCanvas.CanvasStudent student : students) {
      if (this.accessor.apply(student).equals(expectedValue)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a student with a " + propertyName + " of ").appendValue(expectedValue);
  }

  @Override
  protected void describeMismatchSafely(List<GradesFromCanvas.CanvasStudent> students, Description mismatchDescription) {
    mismatchDescription.appendText("the students' " + propertyName + "s were: ");
    Stream<String> actualValues = students.stream().map(accessor);
    mismatchDescription.appendValueList("", ",", "", toIterable(actualValues));
  }

  private <T> Iterable<T> toIterable(final Stream<T> stream) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return stream.iterator();
      }

      @Override
      public void forEach(Consumer<? super T> action) {
        stream.forEach(action);
      }

      @Override
      public Spliterator<T> spliterator() {
        return stream.spliterator();
      }
    };
  }
}
