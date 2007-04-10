package etomica.integrator;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import etomica.exception.ConfigurationOverlapException;
import etomica.potential.PotentialMaster;
import etomica.space.IVector;

/**
 * Integrator implements the algorithm used to move the atoms around and
 * generate new configurations in one or more phases. All integrator techniques,
 * such as molecular dynamics or Monte Carlo, are implemented via subclasses of
 * this Integrator class. The Integrator's activities are managed via the
 * actions of the governing Controller.
 * 
 * @author David Kofke and Andrew Schultz
 */
public abstract class Integrator implements java.io.Serializable {

    protected final PotentialMaster potential;
    protected boolean equilibrating = false;
    protected boolean initialized = false;
    private final LinkedList intervalListeners = new LinkedList();
    private final LinkedList listeners = new LinkedList();
    private ListenerWrapper[] listenerWrapperArray = new ListenerWrapper[0];
    protected int interval;
    private int iieCount;
    protected int stepCount;

    public Integrator(PotentialMaster potentialMaster) {
        this.potential = potentialMaster;
        setEventInterval(1);
        stepCount = 0;
    }

    /**
     * @return value of interval (number of doStep calls) between
     * firing of interval events by integrator.
     */
    public int getEventInterval() {
        return interval;
    }
    
    /**
     * Sets value of interval between successive firing of integrator interval events.
     * @param interval
     */
    public void setEventInterval(int interval) {
        this.interval = interval;
        if (iieCount > interval) {
            iieCount = interval;
        }
    }
    
    public final void doStep() {
        stepCount++;
        doStepInternal();
        if(--iieCount == 0) {
            fireIntervalEvent();
            iieCount = interval;
        }
    }
    
    /**
     * Performs the elementary integration step, such as a molecular dynamics
     * time step, or a Monte Carlo trial.
     */
    public abstract void doStepInternal();

    /**
     * Returns the number of steps performed by the integrator since it was
     * initialized.
     */
    public int getStepCount() {
        return stepCount;
    }
    
    /**
     * Defines the actions taken by the integrator to reset itself, such as
     * required if a perturbation is applied to the simulated phase (e.g.,
     * addition or deletion of a molecule). Also invoked when the
     * integrator is started or initialized.
     */
    //This should be called by subclasses /after/ they have performed their own
    //reset
    public void reset() throws ConfigurationOverlapException {
        fireNonintervalEvent(new IntegratorNonintervalEvent(this, IntegratorNonintervalEvent.RESET));
    }

    /**
     * Initializes the integrator, performing the following steps: (1) deploys
     * agents in all atoms; (2) call reset method; (3) fires an event
     * indicating to registered listeners indicating that initialization has
     * been performed (i.e. fires IntervalEvent of type field set to
     * INITIALIZE).
     */
    public final void initialize() throws ConfigurationOverlapException {
        stepCount = 0;
        initialized = false;
        setup();
        initialized = true;
        reset();
        iieCount = interval;
    }
    
    /**
     * Returns true if initialize method has been called.
     */
    public boolean isInitialized() {
        return initialized;
    }

    protected void setup() throws ConfigurationOverlapException {}

    /**
     * @return Returns the potential.
     */
    public PotentialMaster getPotential() {
        return potential;
    }

    /**
     * Arranges interval listeners registered with this iterator in order such that
     * those with the smallest (closer to zero) priority value are performed
     * before those with a larger priority value.  This is invoked automatically
     * whenever a listener is added or removed.  It should be invoked explicitly if
     * the priority setting of a registered interval listener is changed.
     */
    public synchronized void sortListeners() {
        //sort using linked list, but put into array afterwards
        //for rapid looping (avoid repeated construction of iterator)
        Collections.sort(intervalListeners);
        listenerWrapperArray = (ListenerWrapper[])intervalListeners.toArray(new ListenerWrapper[0]);
    }
    
    public synchronized void addListener(IntegratorListener listener) {
        if(listener instanceof IntegratorIntervalListener) {
            addIntervalListener((IntegratorIntervalListener)listener);
        }
        if(listener instanceof IntegratorNonintervalListener) {
            addNonintervalListener((IntegratorNonintervalListener)listener);
        }
    }

    public synchronized void removeListener(IntegratorListener listener) {
        if(listener instanceof IntegratorIntervalListener) {
            removeIntervalListener((IntegratorIntervalListener)listener);
        }
        if(listener instanceof IntegratorNonintervalListener) {
            removeNonintervalListener((IntegratorNonintervalListener)listener);
        }
    }

    /**
     * Removes all interval and noninterval listeners.
     */
    public synchronized void removeAllListeners() {
        intervalListeners.clear();
        listeners.clear();
        sortListeners();
    }
    
    /**
     * Notifies registered listeners that an interval has passed. Not
     * synchronized, so unpredictable behavior if listeners are added while
     * notification is in process (this should be rare).
     */
    protected void fireIntervalEvent() {
        for(int i=0; i<listenerWrapperArray.length; i++) {
            listenerWrapperArray[i].listener.intervalAction();
        }
    }

    /**
     * Notifies registered listeners of a non-interval event. 
     */
    protected synchronized void fireNonintervalEvent(IntegratorNonintervalEvent ie) {
        Iterator iter = listeners.iterator();
        while(iter.hasNext()) {
            ((IntegratorNonintervalListener)iter.next()).nonintervalAction(ie);
        }
    }

    /**
     * Registers with the given integrator all listeners currently registered
     * with this integrator. Removes all listeners from this integrator.
     */
    public synchronized void transferListenersTo(Integrator anotherIntegrator) {
        if (anotherIntegrator == this) return;
        synchronized(anotherIntegrator) {
            for(int i=0; i<listenerWrapperArray.length; i++) {
                anotherIntegrator.addListener(listenerWrapperArray[i].listener);
            }
            Iterator iter = listeners.iterator();
            while(iter.hasNext()) {//don't use addAll, to avoid duplicating listeners already in anotherIntegrator
                anotherIntegrator.addListener((IntegratorNonintervalListener)iter.next());
            }
            anotherIntegrator.sortListeners();
        }
        removeAllListeners();
    }

    /**
     * Adds the given interval listener to those that receive interval events fired by
     * this integrator.  If listener has already been added to integrator, it is
     * not added again.  If listener is null, NullPointerException is thrown.
     */
    private synchronized void addIntervalListener(IntegratorIntervalListener iil) {
        if(iil == null) throw new NullPointerException("Cannot add null as a listener to Integrator");
        ListenerWrapper wrapper = findWrapper(iil);
        if(wrapper == null) { //listener not already in list, so OK to add it now
            intervalListeners.add(new ListenerWrapper(iil));
            sortListeners();
        }
    }
        
    /**
     * Finds and returns the ListenerWrapper used to put the given listener in the list.
     * Returns null if listener is not in list.
     */
    private ListenerWrapper findWrapper(IntegratorIntervalListener iil) {
        Iterator iterator = intervalListeners.iterator();
        while(iterator.hasNext()) {
            ListenerWrapper wrapper = (ListenerWrapper)iterator.next();
            if(wrapper.listener == iil) return wrapper;//found it
        }
        return null;//didn't find it in list      
    }

    /**
     * Removes given interval listener from those notified of interval events fired
     * by this integrator.  No action results if given listener is null or is not registered
     * with this integrator.
     */
    private synchronized void removeIntervalListener(IntegratorIntervalListener iil) {
        ListenerWrapper wrapper = findWrapper(iil);
        intervalListeners.remove(wrapper);
        sortListeners();
    }

    /**
     * Adds the given listener to those that receive non-interval events fired by
     * this integrator.  If listener has already been added to integrator, it is
     * not added again.  If listener is null, NullPointerException is thrown.
     */
    private synchronized void addNonintervalListener(IntegratorNonintervalListener iil) {
        if(iil == null) throw new NullPointerException("Cannot add null as a listener to Integrator");
        if(!listeners.contains(iil)) {
            listeners.add(iil);
        }
    }
    
    /**
     * Removes given listener from those notified of non-interval events fired
     * by this integrator.  No action results if given listener is null or is not registered
     * with this integrator.
     */
    private synchronized void removeNonintervalListener(IntegratorNonintervalListener iil) {
        listeners.remove(iil);
    }

    /**
     * Integrator agent that holds a force vector. Used to indicate that an atom
     * could be under the influence of a force.
     */
    public interface Forcible {
        public IVector force();
    }
    
    public synchronized IntegratorIntervalListener[] getIntervalListeners() {
        IntegratorIntervalListener[] listenerArray = new IntegratorIntervalListener[listenerWrapperArray.length];
        for (int i=0; i<listenerArray.length; i++) {
            listenerArray[i] = listenerWrapperArray[i].listener;
        }
        return listenerArray;
    }

    public synchronized IntegratorNonintervalListener[] getNonintervalListeners() {
        IntegratorNonintervalListener[] listenerArray = new IntegratorNonintervalListener[listeners.size()];
        Iterator iter = listeners.iterator();
        int i=0;
        while(iter.hasNext()) {
            listenerArray[i++] = (IntegratorNonintervalListener)iter.next();
        }
        return listenerArray;
    }

    /**
     * Wraps interval listener an implements a Comparable interface based
     * on the listeners priority value.
     * This class has a natural ordering that is inconsistent with equals.
     */
    private static class ListenerWrapper implements Comparable, java.io.Serializable {
        private final IntegratorIntervalListener listener;
        private ListenerWrapper(IntegratorIntervalListener listener) {
            this.listener = listener;
        }
        public int compareTo(Object obj) {
            int priority = listener.getPriority();
            int objPriority = ((ListenerWrapper)obj).listener.getPriority();
            //we do explicit comparison of values (rather than returning
            //their difference) to avoid potential problems with large integers.
            if(priority < objPriority) return -1;
            if(priority == objPriority) return 0;
            return +1;
         }
    }
}