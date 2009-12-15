package etomica.virial.cluster2.mvc.view;

import java.awt.Component;
import java.awt.Font;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.border.EmptyBorder;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.looks.Options;

public abstract class ClusterWizardPageTemplate extends DefaultWizardPage {

  protected void attachPanel(String key, JPanel panel) {

    if (panel == getData().getProperty(ClusterWizard.KEY_FIGURE_PANE)) {
      FormLayout layout = new FormLayout("left:pref", "4dlu, pref, pref");
      DefaultFormBuilder builder = new DefaultFormBuilder(layout);
      builder.setDefaultDialogBorder();
      builder.setOpaque(false);
      // builder.setBackground(Color.white);
      builder.add(createGuide(), new CellConstraints(1, 2));
      builder.add(createGuideEntries(), new CellConstraints(1, 3));
      panel.add(builder.getPanel());
    }
    else if (panel == getData().getProperty(ClusterWizard.KEY_MODEL_PANE)) {
      FormLayout layout = new FormLayout("center:270dlu", "pref, 20dlu, pref");
      DefaultFormBuilder builder = new DefaultFormBuilder(layout);
      builder.setDefaultDialogBorder();
      builder.setOpaque(false);
      builder.add(createTitle(), new CellConstraints(1, 1));
      builder.add(createControls(), new CellConstraints(1, 3));
      panel.add(builder.getPanel());
    }
  }

  private JComponent createGuide() {

    FormLayout layout = new FormLayout("left:pref", "pref");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setBorder(new EmptyBorder(0, 0, 0, 0));
    // builder.setDefaultDialogBorder();
    builder.setOpaque(false);

    JLabel title = new JLabel("Completion Status");
    Font font = title.getFont();
    title.setFont(font.deriveFont(font.getStyle() ^ Font.BOLD));
    builder.add(title, new CellConstraints(1, 1));
    return builder.getPanel();
  }

  protected JComponent createGuideEntries() {

    FormLayout layout = new FormLayout("4dlu, pref", "2dlu, 18dlu:grow, 18dlu:grow, 18dlu:grow, 18dlu:grow");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setBorder(new EmptyBorder(0, 0, 0, 0));
    // builder.setDefaultDialogBorder();
    builder.setOpaque(false);

    JLabel[] labels = new JLabel[4];
    labels[0] = builder.addLabel("1. General Properties", new CellConstraints(2, 2));
    labels[1] = builder.addLabel("2. Connectivity Properties", new CellConstraints(2, 3));
    labels[2] = builder.addLabel("3. Color Properties", new CellConstraints(2, 4));
    labels[3] = builder.addLabel("4. Summary", new CellConstraints(2, 5));
    int selectedIndex = getPageIndex();
    if (selectedIndex >= 1 && selectedIndex <= 4) {
      Font font = labels[selectedIndex - 1].getFont();
      labels[selectedIndex - 1].setForeground(UIManager.getColor("TitledBorder.titleColor"));
      labels[selectedIndex - 1].setFont(font.deriveFont(font.getStyle() ^ Font.BOLD));
    }
    return builder.getPanel();
  }

  protected JComponent createTitle() {

    FormLayout layout = new FormLayout("270dlu", "pref");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setBorder(new EmptyBorder(0, 0, 0, 0));
    // builder.setDefaultDialogBorder();
    builder.setOpaque(false);

    JTextArea component = new JTextArea(getTitle());
    component.setOpaque(false);
    component.setEditable(false);
    component.setEnabled(true);
    component.setLineWrap(true);
    component.setWrapStyleWord(true);
    builder.add(component, new CellConstraints(1, 1));
    return builder.getPanel();
  }

  protected JCheckBox createCheckBox(String text) {

    return new JCheckBox(text);
  }

  protected JRadioButton createRadioButton(String text) {

    return new JRadioButton(text);
  }

  protected JComponent buildButtonRow(AbstractButton button, boolean selected, boolean enabled,
      boolean borderPainted, boolean contentAreaFilled) {

    button.setEnabled(enabled);
    button.setSelected(selected);
    button.setBorderPainted(borderPainted);
    button.setContentAreaFilled(contentAreaFilled);

    return buildGrid(button, FormFactory.BUTTON_COLSPEC);
  }

  protected JTextField createText(String text) {

    return new JTextField(text);
  }

  protected JComboBox createComboBox(String[] values, boolean enabled) {

    JComboBox box = new JComboBox(values);
    box.setEnabled(enabled);
    box.setEditable(false);
    box.putClientProperty(Options.COMBO_POPUP_PROTOTYPE_DISPLAY_VALUE_KEY, "A Quite Long Label");
    return box;
  }

  protected JSpinner createSpinner(SpinnerModel model, boolean enabled, boolean editable) {

    JSpinner spinner = new JSpinner(model);
    spinner.setEnabled(enabled);
    JComponent editor = spinner.getEditor();
    if (editor instanceof DefaultEditor) {
      ((DefaultEditor) editor).getTextField().setEditable(editable);
    }
    return spinner;
  }

  protected JComponent buildGrid(Component c1) {

    return buildGrid(c1, new ColumnSpec(ColumnSpec.DEFAULT, Sizes.dluX(20), ColumnSpec.DEFAULT_GROW));
  }

  protected JComponent buildGrid(Component c1, ColumnSpec colSpec) {

    FormLayout layout = new FormLayout("", "pref");
    for (int i = 0; i < 1; i++) {
      layout.appendColumn(colSpec);
      layout.appendColumn(FormFactory.RELATED_GAP_COLSPEC);
    }
    PanelBuilder builder = new PanelBuilder(layout);
    builder.setOpaque(false);
    CellConstraints cc = new CellConstraints();
    builder.add(c1, cc.xy(1, 1));
    return builder.getPanel();
  }

  protected JTextArea buildTextArea(String text, boolean editable, boolean enabled) {

    JTextArea area = new JTextArea();
    area.setText(text);
    area.setEditable(editable);
    area.setEnabled(enabled);
    return area;
  }

  protected abstract int getPageIndex();

  protected abstract String getTitle();

  protected abstract JComponent createControls();
}