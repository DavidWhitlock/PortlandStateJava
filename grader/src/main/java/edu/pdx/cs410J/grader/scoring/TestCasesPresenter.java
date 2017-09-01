package edu.pdx.cs410J.grader.scoring;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.pdx.cs410J.grader.mvp.PresenterOnEventBus;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class TestCasesPresenter extends PresenterOnEventBus {
  private final TestCasesView view;
  private List<TestCaseOutput> testCaseOutputs;

  @Inject
  public TestCasesPresenter(EventBus bus, TestCasesView view) {
    super(bus);
    this.view = view;

    this.view.addTestCaseNameSelectedListener(this::publishTestCaseNameSelectedEvent);
  }

  private void publishTestCaseNameSelectedEvent(int index) {
    TestCaseOutput selected = this.testCaseOutputs.get(index);
    publishEvent(new TestCaseSelected(selected));
  }

  @Subscribe
  public void populateViewWhenSubmissionIsSelected(ProjectSubmissionSelected selected) {
    testCaseOutputs = selected.getProjectSubmission().getTestCaseOutputs();
    setTestCaseNamesInView();

    view.setSelectedTestCaseName(0);
  }

  private void setTestCaseNamesInView() {
    List<String> testCaseNames = testCaseOutputs.stream().map(this::getDisplay).collect(Collectors.toList());
    view.setTestCaseNames(testCaseNames);
  }

  private String getDisplay(TestCaseOutput testCase) {
    return formatTestCase(testCase.getName(), testCase.getPointsDeducted());
  }

  @Subscribe
  public void repopulateViewAndSelectNextTestCaseWhenTestCaseOutputIsUpdated(TestCaseOutputUpdated update) {
    int index = this.testCaseOutputs.indexOf(update.getTestCaseOutput());
    if (index >= 0) {
      setTestCaseNamesInView();
      if (index < this.testCaseOutputs.size() - 1) {
        int nextIndex = index + 1;
        this.view.setSelectedTestCaseName(nextIndex);
        TestCaseOutput next = this.testCaseOutputs.get(nextIndex);
        publishEvent(new TestCaseSelected(next));
      }
    }
  }

  @VisibleForTesting
  static String formatTestCase(String testCaseName, Double pointsDeducted) {
    if (pointsDeducted == null || pointsDeducted == 0.0) {
      return testCaseName;

    } else {
      return testCaseName + " (-" + pointsDeducted + ")";
    }
  }
}
