package etomica.graphics;

import java.util.Hashtable;

import javax.swing.JLabel;

/**
 * Slider that permits variation of decimal (non-integer) values.  Extends
 * the swing JSlider class (which permit input/output of integer values only)
 * and overrides methods in which the user sets/gets current values, maximum and
 * minimum values, etc., and scales the user value by a factor of to increase
 * the apparent precision of the slider.  Thus, if the user wants values in 
 * from 0 to 1 in increments of 0.01, this slider intercepts all communications with user
 * and scales values by factor of 100, so that (unknown to user) slider is actually 
 * working with values from 0 to 100 in incremnts of 1.  set/getPrecision method is
 * used to specify the precision of the slider.
 * 
 * @author S.K. Kwak
 */
public class DecimalSlider extends javax.swing.JSlider {
    
    private Hashtable labelTable;
    private int maximum, minimum;
    private int precision;
    private int spacing, halfSpacing;
    private int anyValues;
    private int standardSpacingLabel;
    
    public DecimalSlider() {
        super();
        setPrecision(0);
    }
    
    /**
     * Accessor method for precision of slider, defined as number of
     * digits after the decimal that will be distinguished by slider.
     * For example, if value is 3, then slider will give values in increments
     * of 0.001.  Precision = 0 means to work with integer values.  Maximum
     * allowable value is 16.
     */
    public int getPrecision(){
        return (int)Math.round(Math.log(precision)/Math.log(10));
    }
    
    /**
     * Mutator method for precision of slider.  See getPrecision.
     */
    public void setPrecision(int p){
        if(p < 0) p = 0;
        if(p > 16) p = 16;
        precision = (int)Math.pow(10,p);
    }
   
    public int getDecimalSliderMaximum(){return super.getMaximum();}
    public void setDecimalSliderMaximum(double max){
        maximum = (int)(max*precision);
        super.setMaximum(maximum);
    }
    
    public int getDecimalSliderMinimum(){return super.getMinimum();}    
    public void setDecimalSliderMinimum(double min){
        minimum = (int)(min*precision);
        super.setMinimum(minimum);   
    }
        
    public int getDecimalSliderMajorTickSpacing(){return super.getMajorTickSpacing();}
    public void setDecimalSliderMajorTickSpacing(double s){
        spacing = (int)(s*precision);
        super.setMajorTickSpacing(spacing);
    }
    
    public int getDecimalSliderMinorTickSpacing(){return super.getMinorTickSpacing();}
    public void setDecimalSliderMinorTickSpacing(double hs){
        halfSpacing = (int)(hs*precision);
        super.setMinorTickSpacing(halfSpacing);
    }
        
    public double getDecimalSliderValue(){
        double realValue =((double) super.getValue())/(double)(precision);
        return realValue;
    }
    public void setDecimalSliderValue(double n){
        anyValues = (int)Math.round(n*precision);
        super.setValue(anyValues);
    }
   
    public Hashtable createDecimalSliderStandardLabels(double n){
        standardSpacingLabel = Math.max((int)(n*precision),1);
        return super.createStandardLabels(standardSpacingLabel);
    }
        
    public void setDecimalSliderLabelTable(Hashtable labels){
        labelTable = new Hashtable();
        
        double mm = minimum/(double)precision;               
        labelTable.put(new Integer(minimum), new JLabel(labelValue(mm)));
        
        for(int i =1; i<labels.size(); i++){
                if(labels.size()-1!=i){
                       int nn = minimum+standardSpacingLabel*i;
                       mm = nn/(double)(precision);
                       labelTable.put(new Integer(nn), new JLabel(labelValue(mm)));    
                 }
                 else if(labels.size()-1==i){
                       mm = maximum/(double)precision;
                       labelTable.put(new Integer(maximum), new JLabel(labelValue(mm)));    
                 }
         }        
        super.setLabelTable(labelTable);
    }
    private String labelValue(double d) {
        if(precision == 1) return String.valueOf((int)d);
        return String.valueOf(d);
    }
       
}
