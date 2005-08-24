package etomica.graphics;
import etomica.action.activity.Controller;

/**
 * Button that attaches to a controller to toggle its pause/resume state.
 * Performs toggling of button label with state.
 */
 
public class DeviceControllerButton extends DeviceButton {
    
    public DeviceControllerButton(Controller controller) {
        super(null);
        toggleAction = new Toggle();
        setController(controller);
    }
    
    //final because called by constructor
    public final void setController(Controller c) {
        toggleAction.setController(c);
        setAction(toggleAction);
        setLabel("  Start  ");
    }
    public Controller getController() {
    	return toggleAction.getController();
    }
    
    /**
     * Sets label of button to display "Start".
     */
    public void reset() {setLabel("  Start  ");}
    
    private Toggle toggleAction;
    
    private class Toggle extends etomica.action.ControllerToggle {
         public void actionPerformed() {
         	if(controller == null) return;
            String text = " Pause ";
            if(controller.isActive() && !controller.isPaused()) text = "Continue";
            super.actionPerformed();
            DeviceControllerButton.this.setLabel(text);
        }
    }//end Toggle
}//end DeviceControllerButton