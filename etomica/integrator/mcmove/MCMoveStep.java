package etomica.integrator.mcmove;

import etomica.potential.PotentialMaster;
import etomica.units.Dimension;
import etomica.units.Length;

public abstract class MCMoveStep extends MCMove implements MCMoveStepDependent {

    public MCMoveStep(PotentialMaster potentialMaster, int nBoxs) {
        this(potentialMaster, new MCMoveStepTracker());
    }

    public MCMoveStep(PotentialMaster potentialMaster,
            MCMoveStepTracker stepTracker) {
        super(potentialMaster, stepTracker);
        ((MCMoveStepTracker)moveTracker).setMCMove(this);
    }

    public double getStepSize() {
        return stepSize;
    }

    public double getStepSizeMax() {
        return stepSizeMax;
    }

    public double getStepSizeMin() {
        return stepSizeMin;
    }

    public void setStepSize(double newStepSize) {
        if (newStepSize > stepSizeMax || newStepSize < stepSizeMin) {
            throw new IllegalArgumentException("step size must be between "+stepSizeMin+" and "+stepSizeMax);
        }
        stepSize = newStepSize;
        ((MCMoveStepTracker)moveTracker).resetAdjustStep();
    }

    public void setStepSizeMax(double newStepSizeMax) {
        stepSizeMax = newStepSizeMax;
    }

    public void setStepSizeMin(double newStepSizeMin) {
        stepSizeMin = newStepSizeMin;
    }

    public final Dimension getStepSizeDimension() {return Length.DIMENSION;}
    public final Dimension getStepSizeMaxDimension() {return Length.DIMENSION;}
    public final Dimension getStepSizeMinDimension() {return Length.DIMENSION;}

    protected double stepSize = 1.0;
    protected double stepSizeMax = Double.MAX_VALUE;
    protected double stepSizeMin = 0.0;
}
