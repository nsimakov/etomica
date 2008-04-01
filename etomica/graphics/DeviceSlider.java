//This class includes a main method to demonstrate its use
package etomica.graphics;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import etomica.EtomicaInfo;
import etomica.api.IAction;
import etomica.api.IController;
import etomica.modifier.Modifier;
import etomica.modifier.ModifierGeneral;
import etomica.modifier.ModifyAction;
import etomica.units.Unit;
import etomica.units.systems.UnitSystem;
import etomica.util.Strings;

/**
 * Device the changes a property using a graphical slider, via a Modifier.
 *
 * @see ModifierGeneral
 */
public class DeviceSlider extends Device {
    
    /**
     * Descriptive text label to be displayed with the value
     * No longer used -- instead apply label via a title border on the slider itself
     */
    private String label;
    /**
     * Modifier connecting the slider to the property
     */
    protected ModifyAction modifyAction;
    /**
     * Subclass of Swing slider displayed to screen
     * located in utility package 
     */
    protected DecimalSlider slider;
    /**
     * Object with property being modulated
     */
    protected Object component;
    /**
     * Property being modulated
     */
    protected String property;
    /**
     * Values for maximum and minimum values of slider in double form
     */
    private double minimum, maximum;
    /**
     * boolean showValues for showing values on textfield
     * boolean editvalus for editing the values in textfield, which change slider tip
     */
    private boolean showValues, editValues;
    /*
     * To show slider with value in one 
     */
    private JPanel panel; 
    /**
     * To show the values of slider
     */
    protected JTextField textField;
    
    /** 
     * column of textfield to show the value of slider correctly with horizontal view
     * default value is five and affected if precision is greater than 5
     */    
    private int column;
    
    /** 
     * horizontal position of TitledBorder.  Default is TitledBorder.LEFT
     */    
    private int borderAlignment = TitledBorder.LEFT;
    
    /**
     * Layout instance to show slider and textfield 
     */    
    private GridBagLayout gbLayout;    
    private GridBagConstraints gbConst; 
    private boolean showBorder = false;
    private int nMajor = 3;
    protected IAction targetAction;
    private boolean showMinorTicks = false;

    public DeviceSlider(IController controller) {
        super(controller);
        init();
    }
    
    /**
     * Constructs a slider connected to the given property of the given object
     */
    public DeviceSlider(IController controller, Object object, String property) {
        this(controller, new ModifierGeneral(object, property));
        component = object;
        this.property = property;
    }
    /**
     * Constructs a slider connected to the get/set Value methods of the given Modifier
     */
    public DeviceSlider(IController controller, Modifier m) {
        this(controller);
        //set component and property in some way
        setModifier(m);
    }

    private void init() {
        textField = new JTextField("");
        textField.setFont(new java.awt.Font("",0,15));
        textField.setHorizontalAlignment(JTextField.CENTER);
        gbLayout = new GridBagLayout();
        gbConst = new GridBagConstraints();    
        panel = new JPanel();  
//        panel.setBorder(new javax.swing.border.TitledBorder("JPST")); //JPanel of Slider and TextField
        setLabel("");
        panel.setLayout(gbLayout);
        column = 5;
        slider = new DecimalSlider();
        slider.setSize(200,40);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setDecimalSliderValue(300);
        setMinimum(0);
        setMaximum(500);
        slider.addChangeListener(new SliderListener());
        slider.setDecimalSliderMajorTickSpacing(100);
        slider.setDecimalSliderMinorTickSpacing(50);

        gbConst.gridx = 0; gbConst.gridy = 0;
        gbLayout.setConstraints(slider, gbConst);
        panel.add(slider);        

        setShowValues(false); // default is false to show values of slider
        setEditValues(false); // default is false to edit values of slider thru textField
    }
    
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo();
        info.setDescription("Slider-type device for changing a property");
        return info;
    }

    /**
     * Override superclass setUnit method to update label when unit is changed
     */
    public void setUnit(Unit u) {
        super.setUnit(u);
        setLabelDefault();
    }

    public void setBorderAlignment(int align) {
    	borderAlignment = align;
    }

    public final void setModifier(Modifier m) {
        if(m == null) throw new NullPointerException();
        modifyAction = null;
        if(unit == null) {
            setUnit(m.getDimension().getUnit(UnitSystem.SIM));
        }
        slider.setDecimalSliderValue(unit.fromSim(m.getValue()));
        modifyAction = new ModifyAction(m);
        targetAction = modifyAction;//need to keep this distinct from modifyAction, in case subclasses want to do more than modifyAction when slider is moved
        setLabelDefault();
        setMinimum(getMinimum());
        setMaximum(getMaximum());
    }
    public final Modifier getModifier() {return modifyAction.getWrappedModifier();}
    
    public void doUpdate() {
        double value = unit.fromSim(modifyAction.getValue());
        slider.setDecimalSliderValue(value);
        textField.setText(String.valueOf(value));
    }
    
    public String getProperty() {return property;}
    public void setProperty(String s) {
        property = s;
        if(component != null) setModifier(new ModifierGeneral(component, property));
    }
    
    public Object getComponent() {return component;}
    public void setComponent(Object obj) {
        component = obj;
        if(property != null) setModifier(new ModifierGeneral(component,property));
    }
    
    public double getMinimum() {return minimum;}
    /**
     * Sets minimum value of slider; should be called after
     * any calls to setPrecision.
     */
    public void setMinimum(double min) {
        minimum = min;
        ModifyAction tmpModifier = modifyAction;
        modifyAction = null;
        slider.setDecimalSliderMinimum(min);
        slider.setInverted(maximum < minimum);
        setTicks();
        modifyAction = tmpModifier;
    }
        
    public double getMaximum() {return maximum;}
    /**
     * Sets maximum value of slider; should be called after
     * any calls to setPrecision.
     */
    public void setMaximum(double max) {
        maximum = max;
        ModifyAction tmpModifier = modifyAction;
        modifyAction = null;
        slider.setDecimalSliderMaximum(max);
        slider.setInverted(maximum < minimum);
        setTicks();
        modifyAction = tmpModifier;
    }
    
    public void setShowMinorTicks(boolean b){
        showMinorTicks = b;
    }
    
    public void setNMajor(int n) {
        nMajor = n;
        setTicks();
    }
    public int getNMajor() {return nMajor;}
    
    private void setTicks() {
        double minorTick = 1.0 ;
        if(showMinorTicks){ minorTick = 2.0;}
        double spacing = (getMaximum()-getMinimum())/nMajor; 
        if(spacing <= 0) return;
        slider.setDecimalSliderMajorTickSpacing(spacing);
        slider.setDecimalSliderMinorTickSpacing(spacing/minorTick);
        //need to do the following because JSlider does not automatically
        //reset labels if they have been set before
        slider.setDecimalSliderLabelTable(slider.createDecimalSliderStandardLabels(spacing/minorTick));
    }

    public boolean getShowValues(){ return showValues;}
    public void setShowValues(boolean b){
        showValues = b;
        if(showValues){ 
            setSliderValueShape("VERTICAL");
            textField.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    double oldX = slider.getDecimalSliderValue();
                    double newX = 0;
                    try {
                        newX = Double.parseDouble(textField.getText());
                    }
                    catch (NumberFormatException ex) {
                        //user entered a bogus number (like "J")
                        textField.setText(String.valueOf(oldX));
                        return;
                    }
                    if (newX < minimum) {
            	        newX = minimum;
                    }
                    else if (newX > maximum) {
                    	newX = maximum;
                    }
                    if (newX != oldX) {
                        // user tabbed or clicked out of the text field
                        setValue(newX);
                    }
                    else {
                        //revert text field to its original value if it changed (300 => 300.0)
                        textField.setText(String.valueOf(oldX));
                    }
                }
            });
            textField.addFocusListener(new FocusListener(){
                public void focusGained(FocusEvent evt) {
                }
                public void focusLost(FocusEvent evt) {
                    double oldX = slider.getDecimalSliderValue();
                    double newX = 0;
                    try {
                        newX = Double.parseDouble(textField.getText());
                    }
                    catch (NumberFormatException ex) {
                        //user entered a bogus number (like "J")
                        textField.setText(String.valueOf(oldX));
                        return;
                    }
                    if (newX != oldX) {
                        // user tabbed or clicked out of the text field
                        setValue(newX);
                    }
                    else {
                        //revert text field to its original value if it changed (300 => 300.0)
                        textField.setText(String.valueOf(oldX));
                    }
                }
            });
        }
    }

    public void setSliderValueShape(String s){
        panel.removeAll();
        textField.setColumns(column);                                                                  
        gbConst.gridx = 0; gbConst.gridy = 0;
        gbLayout.setConstraints(slider, gbConst);
        panel.add(slider);        
        if(s=="HORIZONTAL") {
            gbConst.gridx = 1; gbConst.gridy = 0; 
            gbLayout.setConstraints(textField, gbConst);
            panel.add(textField);}
        if(s=="VERTICAL") { 
            gbConst.fill = GridBagConstraints.HORIZONTAL;
            gbConst.gridx = 0; gbConst.gridy = 1; 
            gbLayout.setConstraints(textField, gbConst);
            panel.add(textField);
        }
    }
    
    public void setSliderVerticalOrientation(boolean b){
        if(b){slider.setOrientation(SwingConstants.VERTICAL);}
    }
    
    public boolean getEditValues(){ return editValues;}
    public void setEditValues(boolean b){
       editValues = b;
       textField.setEditable(editValues);
    }
    
    public int getPrecision(){return slider.getPrecision();}
    public void setPrecision(int n) {
        if(n>5){column = n;}
        else {column = 5;}
        slider.setPrecision(n);
        if (modifyAction != null) {
            doUpdate();
        }
    }    
    
    public double getValue(){return slider.getDecimalSliderValue();}    
    public void setValue(double d){
        //updating the slider will update the text field
        slider.setDecimalSliderValue(d);
    }
    
    /**
     * Returns the GUI element for display in the simulation.
     */
    public java.awt.Component graphic(Object obj) {
//        if(showValues){ return panel;
//        } else {return slider;}
        return panel;
    }
    
    
    /**
     * Sets the value of a descriptive label using the meter's label and the unit's symbol (abbreviation).
     */
    private void setLabelDefault() {
        String suffix = (unit.symbol().length() > 0) ? " ("+unit.symbol()+")" : "";
        if(modifyAction != null) 
            setLabel(Strings.capitalize(modifyAction.getWrappedModifier().getLabel())+suffix);
    }

    /**
     * Sets the value of a descriptive label using the given string.
     */
    public void setLabel(String s){
        label = s;
        if(s == null || s.equals("") || !showBorder) panel.setBorder(new javax.swing.border.EmptyBorder(0,0,0,0));
        else {
        	TitledBorder border = new TitledBorder(s);
        	border.setTitleJustification(borderAlignment);
        	panel.setBorder(border);
        }
    }
    
    /**
     * @return the current instance of the descriptive label.
     */
    public String getLabel() {return label;}
    
    public void setShowBorder(boolean b) {
        showBorder = b;
        setLabel(label);
    }
    public boolean isShowBorder() {return showBorder;}
    
    /**
     * @return a handle to the DecimalSlider instance used by this slider device
     */
    public DecimalSlider getSlider() {return slider;}
    
    /**
     * @return a handle to the JTextField instance used by this slider device
     */    
    public JTextField getTextField() {return textField;}
    
    /**
     * @return a handle to the JPanel instance used by this slider device
     */    
    public JPanel getPanel() { return panel; }    
     
     /**
      * Slider listener, which relays the slider change events to the modifier
      */
     private class SliderListener implements ChangeListener, java.io.Serializable {
         public void stateChanged(ChangeEvent evt) {
             double newValue = slider.getDecimalSliderValue();
             if(modifyAction!=null) {
                 modifyAction.setValueForAction(unit.toSim(newValue));
                 try {
                     doAction(targetAction);
                 }
                 catch (RuntimeException e) {
                     //XXX We want to put the slider back where it was (which is hopefully
                     //what the backend element has now), but changing the 
                     //slider position fires an event that brings us back here.  
                     //null-out modifyAction to prevent recursion!
                     ModifyAction actualModifyAction = modifyAction;
                     modifyAction = null;
                     // new value is the old value!
                     newValue = unit.fromSim(actualModifyAction.getValue());
                     slider.setDecimalSliderValue(newValue);
                     modifyAction = actualModifyAction;
                 }
             }
             textField.setText(String.valueOf(newValue));
         }
     }
}
