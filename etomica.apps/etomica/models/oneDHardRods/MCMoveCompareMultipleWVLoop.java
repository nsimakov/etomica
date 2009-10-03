package etomica.models.oneDHardRods;

import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.mcmove.MCMoveBoxStep;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinition.BasisCell;

/**
 * A Monte Carlo move which compares several normal modes to harmonic normal
 * modes and then explores the phase space defined by the remaining normal
 * modes.
 * 
 * harmonicWVs are the wavevectors that are harmonic, and left out of the "change
 * a random mode" calculation of the doTrial() method.
 * 
 * comparedWVs are the wavevectors that are compared (removed).
 * 
 * @author cribbin
 * 
 */
public class MCMoveCompareMultipleWVLoop extends MCMoveBoxStep {

    private static final long serialVersionUID = 1L;
    protected CoordinateDefinition coordinateDefinition;
    private final AtomIteratorLeafAtoms iterator;
    protected double[][] uOld;
    protected double[] deltaU;
    protected final IRandom random;
    protected double energyOld,energyNew, energyEvenLater;
    protected final MeterPotentialEnergy energyMeter;
    private double[][][] eigenVectors;
    private IVectorMutable[] waveVectors;
    private double[] gaussian;
    protected double temperature;
    private double[][] stdDev;
    private double[] rRand, iRand, realT, imagT;
    private double[] waveVectorCoefficients;
    private double[][] omega2;
    double[] uNow;
    int changedWV, howManyChangesToHardRodModes;
    int[] comparedWVs, harmonicWVs;
    
    public MCMoveCompareMultipleWVLoop(IPotentialMaster potentialMaster,
            IRandom random) {
        super(potentialMaster);
        this.random = random;
        iterator = new AtomIteratorLeafAtoms();
        energyMeter = new MeterPotentialEnergy(potentialMaster);
        gaussian = new double[2];
        howManyChangesToHardRodModes = 1;
    }
    
    public boolean doTrial() {
        int coordinateDim = coordinateDefinition.getCoordinateDim();
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        rRand = new double[coordinateDim];
        iRand = new double[coordinateDim];
        realT = new double[coordinateDim];
        imagT = new double[coordinateDim];
        
        BasisCell cell = cells[0];
        uOld = new double[cells.length][coordinateDim];
        double normalization = 1 / Math.sqrt(cells.length);
        int numWV = comparedWVs.length;
        
        /*
         * This loop looks at each wavevector, asks if that wavevector is
         * removed, and calculates what happens if it is.
         */
// ZERO OUT A NORMAL MODE.
        
        //Store old positions
        for(int iCell = 0; iCell < cells.length; iCell++){
            //store old positions.
            uNow = coordinateDefinition.calcU(cells[iCell].molecules);
            System.arraycopy(uNow, 0, uOld[iCell], 0, coordinateDim);
        }
        for (int wvCount = 0; wvCount < numWV; wvCount++) {
            int comparedwv = comparedWVs[wvCount];
            
            // Get normal mode coordinate information
            coordinateDefinition.calcT(waveVectors[comparedwv], realT, imagT);
            
            for (int iCell = 0; iCell < cells.length; iCell++) {
                cell = cells[iCell];
                uNow = coordinateDefinition.calcU(cells[iCell].molecules);
                // rezero deltaU
                for (int j = 0; j < coordinateDim; j++) {
                    deltaU[j] = 0.0;
                }

                // Calculate the contributions to the current position of
                // the zeroed mode, and subtract it from the overall position
                double kR = waveVectors[comparedwv].dot(cell.cellPosition);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
                for (int i = 0; i < coordinateDim; i++) {
                    // Calculate the current coordinate:
                    double realCoord = 0, imagCoord = 0;
                    for (int j = 0; j < coordinateDim; j++) {
                        realCoord += eigenVectors[comparedwv][i][j] * realT[j];
                        imagCoord += eigenVectors[comparedwv][i][j] * imagT[j];
                    }
                    for (int j = 0; j < coordinateDim; j++) {
                        deltaU[j] -= waveVectorCoefficients[comparedwv] 
                                * eigenVectors[comparedwv][i][j] * 2.0
                                * (realCoord * coskR - imagCoord * sinkR);
                    }
                }
                for (int i = 0; i < coordinateDim; i++) {
                    deltaU[i] *= normalization;
                }

                for (int i = 0; i < coordinateDim; i++) {
                    uNow[i] += deltaU[i];
                }
                coordinateDefinition.setToU(cells[iCell].molecules, uNow);
            }//end of cell loop
        }// end of wvCount loop
        
        energyOld = energyMeter.getDataAsScalar();
        if (Double.isInfinite(energyOld)) {
            int limit = coordinateDefinition.getBox().getLeafList().getAtomCount();
            for (int k = 0; k < limit; k++) {
                System.out.println(k + " " + 
                        coordinateDefinition.getBox().getLeafList().getAtom(k)
                        .getPosition());
            }
            throw new IllegalStateException(
                    "Overlap after the removal of a mode!");
        }

// MOVE SOME NUMBER OF RANDOM HARD ROD POTENTIAL MODES, AND MEASURE energyNew
        // equivalent to MCMoveChangeMode for several modes.
//        int changedWV;
        for(int wvCount = 0; wvCount < howManyChangesToHardRodModes; wvCount++){
            // Select the wave vector whose eigenvectors will be changed.
            // The zero wavevector is center of mass motion, and is rejected as
            // a possibility.
            boolean isAccepted = true;
            do{
                isAccepted = true;
                changedWV = random.nextInt(waveVectorCoefficients.length);
                for(int i = 0; i < harmonicWVs.length; i++){
                    if (changedWV == harmonicWVs[i]) {
                        isAccepted = false;
                    }
                }
            } while (!isAccepted);
//            System.out.println(changedWV);
            
            // calculate the new positions of the atoms.
            // loop over cells
            double[] delta = new double[coordinateDim*2];
            for ( int i = 0; i < coordinateDim*2; i++) {
                delta[i] = (2*random.nextDouble()-1) * stepSize;
            }
            for (int iCell = 0; iCell < cells.length; iCell++) {
                uNow = coordinateDefinition.calcU(cells[iCell].molecules);
                cell = cells[iCell];
                // rezero deltaU
                for (int j = 0; j < coordinateDim; j++) {
                    deltaU[j] = 0.0;
                }
                // loop over the wavevectors, and sum contribution of each to
                // the generalized coordinates. Change the selected wavevectors
                // eigenvectors at the same time!
                double kR = waveVectors[changedWV].dot(cell.cellPosition);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
                for (int i = 0; i < coordinateDim; i++) {
                    if( !(Double.isInfinite(omega2[changedWV][i]))) {
                        for (int j = 0; j < coordinateDim; j++) {
                            deltaU[j] += waveVectorCoefficients[changedWV]
                                    * eigenVectors[changedWV][i][j] * 2.0
                                    * (delta[j] * coskR - delta[j+coordinateDim] * sinkR);
                        }
                    }
                }
                 for(int i = 0; i < coordinateDim; i++){
                     deltaU[i] *= normalization;
                 }
                for (int i = 0; i < coordinateDim; i++) {
                    uNow[i] += deltaU[i];
                }
                coordinateDefinition.setToU(cells[iCell].molecules, uNow);
            }
        }//end wvCount loop
        energyNew = energyMeter.getDataAsScalar();
        
//        System.out.println("after hardrod move: " + energyNew);
//        for(int i = 0; i < list.getAtomCount(); i++){
//            System.out.println(((IAtomPositioned)coordinateDefinition.getBox().getLeafList().getAtom(i)).getPosition());
//        }
        
// MOVE EACH NORMAL MODE THAT WAS ZEROED OUT.
        // set up the gaussian values
        double sqrtT = Math.sqrt(temperature);
        
        //This should loop over the wave vectors that we are comparing.
        for(int wvCount = 0; wvCount < numWV; wvCount++){
            int comparedwv = comparedWVs[wvCount];
            for (int j = 0; j < coordinateDim; j++) {
                if (stdDev[comparedwv][j] == 0) {continue;}
                // generate real and imaginary parts of random normal-emode
                // coordinate Q
                double realGauss = random.nextGaussian() * sqrtT;
                double imagGauss = random.nextGaussian() * sqrtT;
                
                // XXX we know that if c(k) = 0.5, one of the gaussians will be
                // ignored, but it's hard to know which. So long as we don't put
                // an atom at the  origin (which is true for 1D if c(k)=0.5), 
                // it's the real part that will be ignored.
                if (waveVectorCoefficients[comparedwv] == 0.5) {imagGauss = 0;}
                rRand[j] = realGauss * stdDev[comparedwv][j];
                iRand[j] = imagGauss * stdDev[comparedwv][j];
                gaussian[0] = realGauss;
                gaussian[1] = imagGauss;
            }
    
            // calculate the new positions of the atoms.
            for (int iCell = 0; iCell < cells.length; iCell++) {
                uNow = coordinateDefinition.calcU(cells[iCell].molecules);
                cell = cells[iCell];
                // rezero deltaU
                for (int j = 0; j < coordinateDim; j++) {
                    deltaU[j] = 0.0;
                }
                // Calculate the change in position due to the substitution of a
                // Gaussian.
                double kR = waveVectors[comparedwv].dot(cell.cellPosition);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
                for (int i = 0; i < coordinateDim; i++) {
                    for (int j = 0; j < coordinateDim; j++) {
                        deltaU[j] += waveVectorCoefficients[comparedwv] 
                                * eigenVectors[comparedwv][i][j] * 2.0
                                * (rRand[i] * coskR - iRand[i] * sinkR);
                    }
                }
                for (int i = 0; i < coordinateDim; i++) {
                    deltaU[i] *= normalization;
                }
                for (int i = 0; i < coordinateDim; i++) {
                    uNow[i] += deltaU[i];
                }
                coordinateDefinition.setToU(cells[iCell].molecules, uNow);
            }
                
        } // end wvCount loop
        
//        System.out.println("At end of move: " + energyMeter.getDataAsScalar());
//        for(int i = 0; i < list.getAtomCount(); i++){
//            System.out.println(((IAtomPositioned)coordinateDefinition.getBox().getLeafList().getAtom(i)).getPosition());
//        }
        return true;
    }

    public double getA() {
        return 1;
    }

    public double getB() {
        return -(energyNew - energyOld);
    }

    public void acceptNotify() {
//      System.out.println("accept MCMoveCompareMultipleWVLoop");
    }

    public double energyChange() {
        return energyNew - energyOld;
    }

    public void rejectNotify() {
//      System.out.println("reject MCMoveCompareMultipleWVLoop");
        // Set all the atoms back to the old values of u
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        for (int iCell = 0; iCell < cells.length; iCell++) {
            BasisCell cell = cells[iCell];
            coordinateDefinition.setToU(cell.molecules, uOld[iCell]);
        }
    }

    public void setBox(IBox newBox) {
        super.setBox(newBox);
        iterator.setBox(newBox);
        energyMeter.setBox(newBox);
    }

    public AtomIterator affectedAtoms() {
        return iterator;
    }

    public void setCoordinateDefinition(CoordinateDefinition newCD) {
        coordinateDefinition = newCD;
        deltaU = new double[coordinateDefinition.getCoordinateDim()];
        uOld = null;
        realT = new double[coordinateDefinition.getCoordinateDim()];
        imagT = new double[coordinateDefinition.getCoordinateDim()];
    }

    public CoordinateDefinition getCoordinateDefinition() {
        return coordinateDefinition;
    }

    /**
     * Set the wave vectors accessible to the move.
     * 
     * @param wv
     */
    public void setWaveVectors(IVectorMutable[] wv){
        waveVectors = new IVectorMutable[wv.length];
        waveVectors = wv;
    }
    public void setWaveVectorCoefficients(double[] newWaveVectorCoefficients) {
        waveVectorCoefficients = newWaveVectorCoefficients;
    }

    /**
     * Informs the move of the eigenvectors
     */
    public void setEigenVectors(double[][][] newEigenVectors) {
        eigenVectors = newEigenVectors;
    }

    public void setOmegaSquared(double[][] o2, double[] coeff) {
        this.omega2 = o2;
        stdDev = new double[omega2.length][omega2[0].length];
        for (int i = 0; i < stdDev.length; i++) {
            for (int j = 0; j < stdDev[i].length; j++) {
                stdDev[i][j] = Math.sqrt(1.0 / (2.0 * omega2[i][j] * coeff[i]));
            }
        }
    }

    public void setTemperature(double newTemperature) {
        temperature = newTemperature;
    
    }

    public double[] getGaussian() {
        return gaussian;
    }

    /**
     * Set the wavevectors that are removed.
     * @param wv the wavevectors that are removed.
     */
    public void setComparedWV(int[] wv){
        comparedWVs = wv;
    
        int dink = 0;
        for(int i = 0; i < wv.length; i++){
            if(wv[i] < wv[dink]){
                dink = i;
            }
        }
    }
    /**
     * Set the wavevectors that are always harmonic.
     * @param wv
     */
    public void setHarmonicWV(int[] wv){
        if(comparedWVs == null){ 
            throw new IllegalStateException("Must set comparedWVs before " +
                    "harmonicWVs");
        }
        harmonicWVs = new int[wv.length + comparedWVs.length];
        
        for(int i = 0; i < wv.length; i++) {
            for(int j = 0; j < comparedWVs.length; j++){
                if(wv[i] == comparedWVs[j]){
                    throw new IllegalArgumentException("A compared " +
                            "wavevector cannot be a harmonic wavevector");
                }
            }
            harmonicWVs[i] = wv[i];
        }

        for(int i = wv.length; i < harmonicWVs.length; i++){
            harmonicWVs[i] = comparedWVs[i-wv.length];
        }
        
        if(harmonicWVs.length+1 == waveVectors.length){
            howManyChangesToHardRodModes = 0;
        }
    }
}
