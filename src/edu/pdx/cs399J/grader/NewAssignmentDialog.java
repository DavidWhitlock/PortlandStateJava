package edu.pdx.cs410J.grader;

import edu.pdx.cs410J.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * This dialog is used to create a new <code>Assignment</code>
 */
public class NewAssignmentDialog extends JDialog {

  private Assignment assignment;

  // GUI components we care about
  private AssignmentPanel assignmentPanel;

  /**
   * Creates a new <code>NewAssignmentDialog</code> and populates with
   * GUI components.
   */
  public NewAssignmentDialog(JFrame owner) {
    super(owner, "New Assignment", true /* modal */);

    Container pane = this.getContentPane();
    pane.setLayout(new BorderLayout());

    this.assignmentPanel = new AssignmentPanel(true);
    pane.add(this.assignmentPanel, BorderLayout.CENTER);

    JPanel buttons = new JPanel();
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
    buttons.add(Box.createHorizontalGlue());
    
    JButton ok = new JButton("OK");
    ok.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // Create a new Assignment from the contents of assignment
          // panel
          assignment = assignmentPanel.createAssignment();
          NewAssignmentDialog.this.dispose();
        }
      });
    buttons.add(ok);

    buttons.add(Box.createHorizontalGlue());

    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // Do not create a new assignment
          assignment = null;
          NewAssignmentDialog.this.dispose();
        }
      });
    buttons.add(cancel);

    buttons.add(Box.createHorizontalGlue());

    pane.add(buttons, BorderLayout.SOUTH);
  }

  /**
   * Returns the <code>Assignment</code> created by this dialog.  If
   * no assignment was created, <code>null</code> is returned.
   */
  public Assignment getAssignment() {
    return(this.assignment);
  }

}
