package etomica.models.oneDHardRods;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IRandom;
import etomica.box.Box;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.AccumulatorRatioAverage;
import etomica.data.DataPump;
import etomica.data.IEtomicaDataSource;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.exception.ConfigurationOverlapException;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.BasisMonatomic;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.listener.IntegratorListenerAction;
import etomica.math.SpecialFunctions;
import etomica.nbr.list.PotentialMasterList;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.MCMoveAtomCoupled;
import etomica.normalmode.MeterHarmonicCoordinate;
import etomica.normalmode.NormalModes;
import etomica.normalmode.NormalModes1DHR;
import etomica.normalmode.P2XOrder;
import etomica.normalmode.WaveVectorFactory;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential2;
import etomica.potential.Potential2HardSpherical;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Null;
import etomica.util.ParameterBase;
import etomica.util.RandomNumberGenerator;
import etomica.util.ReadParameters;
import etomica.virial.overlap.AccumulatorVirialOverlapSingleAverage;
import etomica.virial.overlap.DataSourceVirialOverlap;
import etomica.virial.overlap.IntegratorOverlap;

/**
 * MC simulation
 * 1D hard rods
 * No graphic display
 * Calculate free energy of solid
 * 
 * Treats modes as degrees of freedom; keeps one rod at each end that does not
 * move by central image.
 * 
 * 
 * Uses overlap sampling.
 */

/*
 * Starts in notes 7/09
 */
public class SimDifferentImage1DHR extends Simulation {

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "SimDifferentImage";
    public Primitive primitive;
    int[] nCellsTarget, nCellsRef;
    NormalModes nmRef, nmTarg;
    public BasisMonatomic basis;
    public ActivityIntegrate activityIntegrate;
    public CoordinateDefinition cDefTarget, cDefRef;
    WaveVectorFactory waveVectorFactoryRef, waveVectorFactoryTarg;
    
    MCMoveAtomCoupled mcMoveAtom;
    MCMoveChangeMultipleWV mcMoveMode;
    
    double bennettParam;       //adjustable parameter - Bennett's parameter
    public IntegratorOverlap integratorSim; //integrator for the whole simulation
    public DataSourceVirialOverlap dsvo;
    IntegratorMC[] integrators;
    public AccumulatorVirialOverlapSingleAverage[] accumulators;
    public DataPump[] accumulatorPumps;
    public IEtomicaDataSource[] meters;
    public IBox boxTarget, boxRef;
    public Boundary bdryTarget, bdryRef;
    MeterPotentialEnergy meterBinB, meterAinA, meterRef;
    MeterDifferentImageAdd meterAinB;
    MeterDifferentImageSubtract meterBinA;
    

    AccumulatorAverageFixed accHarmonic;
    
    
    public SimDifferentImage1DHR(Space _space, int numAtoms, double density, 
            int blocksize, double tems, int[] targWV, int[] refWV) {
        super(_space);
        System.out.println("Running " + APP_NAME);
        
        long seed = 3;
        System.out.println("Seed explicitly set to " + seed);
        IRandom rand = new RandomNumberGenerator(seed);
        this.setRandom(rand);
        
        int targAtoms = numAtoms + 1;
        int refAtoms = numAtoms;
        
        double temperature = tems;
        
        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        addSpecies(species);
        
        integrators = new IntegratorMC[2];
        accumulatorPumps = new DataPump[2];
        meters = new IEtomicaDataSource[2];
        accumulators = new AccumulatorVirialOverlapSingleAverage[2];
        
        basis = new BasisMonatomic(space);
        
//TARGET
        // Set up target system - A, 1
        PotentialMasterList potentialMasterTarget = new PotentialMasterList(this, space);
        
        boxTarget = new Box(space);
        addBox(boxTarget);
        boxTarget.setNMolecules(species, targAtoms);
        
        Potential2 potential = new P2HardSphere(space, 1.0, true);
        potential = new P2XOrder(space, (Potential2HardSpherical)potential);
        potential.setBox(boxTarget);
        potentialMasterTarget.addPotential(potential, new IAtomType[] {
                species.getLeafType(), species.getLeafType()});

        primitive = new PrimitiveCubic(space, 1.0/density);
        bdryTarget = new BoundaryRectangularPeriodic(space, targAtoms/density);
        nCellsTarget = new int[]{targAtoms};
        boxTarget.setBoundary(bdryTarget);
        
        cDefTarget = new CoordinateDefinitionLeaf(boxTarget, primitive, basis, space);
        cDefTarget.initializeCoordinates(nCellsTarget);
//        int cDimTarget = cDefTarget.getCoordinateDim();

        double neighborRange = 1.01/density;
        potentialMasterTarget.setRange(neighborRange);
        //find neighbors now.  Don't hook up NeighborListManager since the
        //  neighbors won't change
        potentialMasterTarget.getNeighborManager(boxTarget).reset();
        
        IntegratorMC integratorTarget = new IntegratorMC(potentialMasterTarget,
                random, temperature);
        integrators[1] = integratorTarget;
        integratorTarget.setBox(boxTarget);
        
        nmTarg = new NormalModes1DHR(boxTarget.getBoundary(), targAtoms);
        nmTarg.setHarmonicFudge(1.0);
        nmTarg.setTemperature(temperature);
        nmTarg.getOmegaSquared();
        waveVectorFactoryTarg = nmTarg.getWaveVectorFactory();
        waveVectorFactoryTarg.makeWaveVectors(boxTarget);
        
//        double[] wvc= nmTarg.getWaveVectorFactory().getCoefficients();
//        double[][] omega = nmTarg.getOmegaSquared();
        
        System.out.println("We have " + waveVectorFactoryTarg.getWaveVectors().length +" target wave vectors.");
        System.out.println("Target Wave Vector Coefficients:");
        
        System.out.println("Target WV: ");
        for (int i = 0; i < targWV.length; i++){
            System.out.println(targWV[i]);
        }
        
        mcMoveAtom = new MCMoveAtomCoupled(potentialMasterTarget, random, space);
        mcMoveAtom.setPotential(potential);
        mcMoveAtom.setBox(boxTarget);
        mcMoveAtom.setStepSizeMin(0.001);
        mcMoveAtom.setStepSize(0.01);
        integratorTarget.getMoveManager().addMCMove(mcMoveAtom);
        
        mcMoveMode = new MCMoveChangeMultipleWV(potentialMasterTarget, random);
        mcMoveMode.setCoordinateDefinition(cDefTarget);
        mcMoveMode.setEigenVectors(nmTarg.getEigenvectors());
        mcMoveMode.setOmegaSquared(nmTarg.getOmegaSquared());
        mcMoveMode.setWaveVectorCoefficients(nmTarg.getWaveVectorFactory().getCoefficients());
        mcMoveMode.setWaveVectors(nmTarg.getWaveVectorFactory().getWaveVectors());
        mcMoveMode.addChangeableWV(targWV);
        integratorTarget.getMoveManager().addMCMove(mcMoveMode);
        
        meterAinA = new MeterPotentialEnergy(potentialMasterTarget);
        meterAinA.setBox(boxTarget);
        integratorTarget.setMeterPotentialEnergy(meterAinA);
        
        
        //Fun with meters!
        MeterHarmonicCoordinate meterHarmonic = new MeterHarmonicCoordinate(cDefTarget);
        meterHarmonic.setEigenvectors(nmTarg.getEigenvectors()[1][0]);
        meterHarmonic.setWaveVector(nmTarg.getWaveVectorFactory().getWaveVectors()[1]);
        accHarmonic = new AccumulatorAverageFixed();
        DataPump pumpHarm = new DataPump(meterHarmonic, accHarmonic);
        IntegratorListenerAction pumpHarmListener = new IntegratorListenerAction(pumpHarm);
        pumpHarmListener.setInterval(1000);
        integratorTarget.getEventManager().addListener(pumpHarmListener);
        
        
//REFERENCE
        // Set up reference system - B, 0
        PotentialMasterList potentialMasterRef = new PotentialMasterList(this, space);
        
        boxRef = new Box(space);
        addBox(boxRef);
        boxRef.setNMolecules(species, refAtoms);
        
        potential = new P2HardSphere(space, 1.0, true);
        potential = new P2XOrder(space, (Potential2HardSpherical)potential);
        potential.setBox(boxRef);
        potentialMasterRef.addPotential(potential, new IAtomType[] {
                species.getLeafType(), species.getLeafType()});

        primitive = new PrimitiveCubic(space, 1.0/density);
        bdryRef = new BoundaryRectangularPeriodic(space, refAtoms/density);
        nCellsRef = new int[]{refAtoms};
        boxRef.setBoundary(bdryRef);
        
        cDefRef = new CoordinateDefinitionLeaf(boxRef, primitive, basis, space);
        cDefRef.initializeCoordinates(nCellsRef);
//        int cDimRef = cDefRef.getCoordinateDim();

        neighborRange = 1.01/density;
        potentialMasterRef.setRange(neighborRange);
        //find neighbors now.  Don't hook up NeighborListManager since the
        //  neighbors won't change
        potentialMasterRef.getNeighborManager(boxRef).reset();
        
        IntegratorMC integratorRef = new IntegratorMC(potentialMasterRef, 
                random, temperature);
        integratorRef.setBox(boxRef);
        integrators[0] = integratorRef;
        
        nmRef = new NormalModes1DHR(boxRef.getBoundary(), refAtoms);
        nmRef.setHarmonicFudge(1.0);
        nmRef.setTemperature(temperature);
        nmRef.getOmegaSquared();
        waveVectorFactoryRef = nmRef.getWaveVectorFactory();
        waveVectorFactoryRef.makeWaveVectors(boxRef);
        
//        wvc= nmRef.getWaveVectorFactory().getCoefficients();
//        omega = nmRef.getOmegaSquared();
        
        
        System.out.println("We have " + waveVectorFactoryRef.getWaveVectors().length +" reference wave vectors.");
        System.out.println("Reference Wave Vector Coefficients:");
        
        System.out.println("Ref WV: ");
        for (int i = 0; i < refWV.length; i++){
            System.out.println(refWV[i]);
        }
        
        mcMoveAtom = new MCMoveAtomCoupled(potentialMasterRef, random, space);
        mcMoveAtom.setPotential(potential);
        mcMoveAtom.setBox(boxRef);
        mcMoveAtom.setStepSizeMin(0.001);
        mcMoveAtom.setStepSize(0.01);
        integratorRef.getMoveManager().addMCMove(mcMoveAtom);
        
        mcMoveMode = new MCMoveChangeMultipleWV(potentialMasterRef, random);
        mcMoveMode.setBox(boxRef);
        mcMoveMode.setCoordinateDefinition(cDefRef);
        mcMoveMode.setEigenVectors(nmRef.getEigenvectors());
        mcMoveMode.setOmegaSquared(nmRef.getOmegaSquared());
        mcMoveMode.setWaveVectorCoefficients(nmRef.getWaveVectorFactory().getCoefficients());
        mcMoveMode.setWaveVectors(nmRef.getWaveVectorFactory().getWaveVectors());
        mcMoveMode.addChangeableWV(refWV);
        integratorRef.getMoveManager().addMCMove(mcMoveMode);
        
        meterBinB = new MeterPotentialEnergy(potentialMasterRef);
        meterBinB.setBox(boxRef);
        

        
//JOINT
        meterAinB = new MeterDifferentImageAdd("meterAinB", refAtoms, density, 
                this, primitive, basis, cDefRef, nmRef, temperature);
        MeterOverlapSameGaussian meterOverlapInB = new MeterOverlapSameGaussian("MeterOverlapInB", 
                Null.DIMENSION, meterBinB, meterAinB, temperature);

        
        meterBinA = new MeterDifferentImageSubtract("MeterBinA", targAtoms, 
                density, this, primitive, basis, cDefTarget, nmTarg, temperature);
        MeterOverlap meterOverlapInA = new MeterOverlap("MeterOverlapInA", 
                Null.DIMENSION, meterAinA, meterBinA, temperature);

        meters[1] = meterOverlapInA;
        meters[0] = meterOverlapInB;
        potentialMasterRef.getNeighborManager(boxRef).reset();
        potentialMasterTarget.getNeighborManager(boxTarget).reset();
        
        //Set up the rest of the joint stuff
        
        integratorSim = new IntegratorOverlap(new 
                IntegratorMC[]{integratorRef, integratorTarget});
        
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, true), 0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, false), 1);
        
        setBennettParameter(1.0, 30);
        
        activityIntegrate = new ActivityIntegrate(integratorSim, 0, true);
        getController().addAction(activityIntegrate);
        
        
    }
    public void setBennettParameter(double benParamCenter, double span) {
        bennettParam = benParamCenter;
        accumulators[0].setBennetParam(benParamCenter,span);
        accumulators[1].setBennetParam(benParamCenter,span);
    }
    
    public void setBennettParameter(double newBennettParameter) {
        System.out.println("setting ref pref (explicitly) to "+
                newBennettParameter);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
        setBennettParameter(newBennettParameter,1);
        
    }
    
    public void initBennettParameter(String fileName, int initSteps, int initBlockSize) {
        // benParam = -1 indicates we are searching for an appropriate value
        bennettParam = -1.0;
        integratorSim.getMoveManager().setEquilibrating(true);
        
        if (fileName != null) {
            try { 
                FileReader fileReader = new FileReader(fileName);
                BufferedReader bufReader = new BufferedReader(fileReader);
                String benParamString = bufReader.readLine();
                bennettParam = Double.parseDouble(benParamString);
                bufReader.close();
                fileReader.close();
                System.out.println("setting ref pref (from file) to "+bennettParam);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
                setBennettParameter(bennettParam,1);
            }
            catch (IOException e) {
                System.out.println("Bennett parameter not from file");
                // file not there, which is ok.
            }
        }
        
        if (bennettParam == -1) {
            
//            int oldBlockSize = blockSize;
//            long newBlockSize = initSteps*integratorSim.getNumSubSteps()/1000;
//            //Make sure the new block size is reasonable.
//            if(newBlockSize < 1000){
//                newBlockSize = 1000;
//            }
//            if(newBlockSize > 1000000){
//                newBlockSize = 1000000;
//            }
//            setAccumulatorBlockSize((int)newBlockSize);
            
            // equilibrate off the lattice to avoid anomolous contributions
            activityIntegrate.setMaxSteps(initSteps);
            
            getController().actionPerformed();
            getController().reset();

            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,41,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,41,false),1);
            setBennettParameter(1,10);
            activityIntegrate.setMaxSteps(initSteps);
            
            getController().actionPerformed();
            getController().reset();

            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            
//            double top = accumulators[0].getBennetAverage(newMinDiffLoc);
//            System.out.println("top " + top);
//            double bottom = accumulators[1].getBennetAverage(newMinDiffLoc);
//            System.out.println("bottom " + bottom);
            
            if (Double.isNaN(bennettParam) || bennettParam == 0 || Double.isInfinite(bennettParam)) {
                throw new RuntimeException("Simulation failed to find a valid ref pref");
            }
            System.out.println("setting ref pref to "+bennettParam);
//            setAccumulatorBlockSize(oldBlockSize);
            
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,false),1);
            setBennettParameter(bennettParam,2);
            
            // set benParam back to -1 so that later on we know that we've been looking for
            // the appropriate value
            bennettParam = -1;
            getController().reset();
        }
        integratorSim.getMoveManager().setEquilibrating(false);
    }
    
    public void setAccumulator(AccumulatorVirialOverlapSingleAverage 
            newAccumulator, int iBox) {
        accumulators[iBox] = newAccumulator;
        if (accumulatorPumps[iBox] == null) {
            accumulatorPumps[iBox] = new DataPump(meters[iBox], newAccumulator);
            IntegratorListenerAction pumpListener = new IntegratorListenerAction(accumulatorPumps[iBox]);
            pumpListener.setInterval(1);
            integrators[iBox].getEventManager().addListener(pumpListener);
//            integrators[iBox].setActionInterval(accumulatorPumps[iBox], 
//                    boxRef.getLeafList().getAtomCount()*2);
        }
        else {
            accumulatorPumps[iBox].setDataSink(newAccumulator);
        }
        if (integratorSim != null && accumulators[0] != null && 
                accumulators[1] != null) {
            dsvo = new DataSourceVirialOverlap(accumulators[0],accumulators[1]);
            integratorSim.setDSVO(dsvo);
        }
        
    }
    
    public void setAccumulatorBlockSize(int newBlockSize) {
        for (int i=0; i<2; i++) {
            accumulators[i].setBlockSize(newBlockSize);
        }
        try {
            // reset the integrator so that it will re-adjust step frequency
            // and ensure it will take enough data for both ref and target
            integratorSim.reset();
        }
        catch (ConfigurationOverlapException e) { /* meaningless */ }
    }
    public void equilibrate(String fileName, int initSteps, int initBlockSize) {
        // run a short simulation to get reasonable MC Move step sizes and
        // (if needed) narrow in on a reference preference
        activityIntegrate.setMaxSteps(initSteps);
        
        integratorSim.getMoveManager().setEquilibrating(true);
        
        //This code allows the computer to set the block size for the main
        //simulation and equilibration/finding alpha separately.
//        int oldBlockSize = blockSize;
//        long newBlockSize = initSteps*integratorSim.getNumSubSteps()/1000;
//        //make sure new block size is reasonablel
//        if(newBlockSize < 1000){
//            newBlockSize = 1000;
//        }
//        if (newBlockSize >1000000) {
//            newBlockSize = 1000000;
//        }
//        setAccumulatorBlockSize((int)newBlockSize);
        
//        setAccumulatorBlockSize((int)eqBlockSize);
        
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(true);
        }
        getController().actionPerformed();
        getController().reset();
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(false);
        }
        
        if (bennettParam == -1) {
            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            System.out.println("setting ref pref to "+bennettParam+" ("+newMinDiffLoc+")");
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,1,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,1,false),1);
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            setBennettParameter(bennettParam,1);
            if (fileName != null) {
                try {
                    FileWriter fileWriter = new FileWriter(fileName);
                    BufferedWriter bufWriter = new BufferedWriter(fileWriter);
                    bufWriter.write(String.valueOf(bennettParam)+"\n");
                    bufWriter.close();
                    fileWriter.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("couldn't write to Bennet parameter file");
                }
            }
        }
        else {
            dsvo.reset();
        }
        
        integratorSim.getMoveManager().setEquilibrating(false);
//        setAccumulatorBlockSize(oldBlock Size);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        SimParam params = new SimParam();
        String inputFilename = null;
        if(args.length > 0) {
            inputFilename = args[0];
        }
        if(inputFilename != null){
            ReadParameters readParameters = new ReadParameters(inputFilename, params);
            readParameters.readParameters();
            inputFilename = params.inputfilename;
        }
        
        int nA = params.numAtoms;
        double density = params.density;
        int D = params.D;
        double harmonicFudge = params.harmonicFudge;
        String filename = params.filename;
        if(filename.length() == 0){
            filename = "1DHR";
        }
        double temperature = params.temperature;
//        String outputfn = params.outputname;
        int[] targWVs = params.targWVs;
        int[] refWVs = params.refWVs;
        int runNumSteps = params.numSteps;
        int runBlockSize = params.runBlockSize;
        int subBlockSize = params.subBlockSize;
        int eqNumSteps = params.eqNumSteps;
        int eqBlockSize = params.eqBlockSize;
        int benNumSteps = params.bennettNumSteps;
        int benBlockSize = params.benBlockSize;
        
        String refFileName = args.length > 0 ? filename+"_ref" : null;
        
        // instantiate simulation
        SimDifferentImage1DHR sim = new SimDifferentImage1DHR(Space.getInstance(D), nA, 
                density, runBlockSize, temperature, targWVs, refWVs);
        System.out.println(nA + " atoms at density " + density);
        System.out.println(runNumSteps + " steps, " + runBlockSize + " blocksize");
        System.out.println("input data from " + inputFilename);
        System.out.println("output data to " + filename);System.out.println("instantiated");
        
        //Divide out all the steps, so that the subpieces have the proper # of steps
        runNumSteps /= (int)subBlockSize;
        eqNumSteps /= (int)subBlockSize;
        benNumSteps /= subBlockSize;
        
        //start simulation & equilibrate
        sim.integratorSim.getMoveManager().setEquilibrating(true);
        sim.integratorSim.setNumSubSteps(subBlockSize);
        
        System.out.println("Init Bennett");
        sim.setAccumulatorBlockSize(benBlockSize);
        sim.initBennettParameter(filename, benNumSteps, benBlockSize);
        if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || 
                Double.isInfinite(sim.bennettParam)){
            throw new RuntimeException("Simulation failed to find a valid " +
                    "Bennett parameter");
        }
        
//        System.exit(5);
        
        System.out.println("equilibrate");
        sim.setAccumulatorBlockSize(eqBlockSize);
        sim.equilibrate(refFileName, eqNumSteps, eqBlockSize);
        if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || 
                Double.isInfinite(sim.bennettParam)){
            throw new RuntimeException("Simulation failed to find a valid " +
                    "Bennett parameter");
        }
        System.out.println("equilibration finished.");
        
        // start simulation
        sim.setAccumulatorBlockSize((int)runBlockSize);
        sim.integratorSim.getMoveManager().setEquilibrating(false);
        sim.activityIntegrate.setMaxSteps(runNumSteps);
        sim.getController().actionPerformed();
        System.out.println("final reference optimal step frequency " + 
                sim.integratorSim.getStepFreq0() + " (actual: " + 
                sim.integratorSim.getActualStepFreq0() + ")");
        
        
        //CALCULATION OF HARMONIC ENERGY
        
        double ratio = sim.dsvo.getDataAsScalar();
        double error = sim.dsvo.getError();
        System.out.println("ratio average: "+ratio+", error: "+error);
        System.out.println("free energy difference: " + (-Math.log(ratio)) + 
                ", error: "+(error/ratio));
        DataGroup allYourBase = 
            (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("reference ratio average: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                 + " error: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.accumulators[1].getNBennetPoints() -
                sim.dsvo.minDiffLocation()-1);
        System.out.println("target ratio average: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                 + " error: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
    
        if(D==1) {
            double AHR = -(nA-1)*Math.log(nA/density-nA) + SpecialFunctions.lnFactorial(nA-1) ;
            System.out.println("Hard-rod free energy for " + nA + ": "+AHR);
            
            AHR = -(nA)*Math.log((nA+1)/density-(nA+1)) + SpecialFunctions.lnFactorial(nA) ;
            System.out.println("Hard-rod free energy for " + (nA+1) + ": "+AHR);
        }
        
        
        DataGroup dork = (DataGroup)sim.accHarmonic.getData();
        System.out.println("Measurement of eta: " + dork.getValue(0));
        
        
        System.out.println("Fini.");
    }
    
    public static class SimParam extends ParameterBase {
        public int numAtoms = 1;
        public double density = 0.70;
        public int D = 1;
        public double harmonicFudge = 1.0;
        public String filename = "HR1D_";
        public String inputfilename = "input";
        public String outputname = "hists";
        public double temperature = 1.0;
        
        public int numSteps = 10000000;
        public int runBlockSize = 1000;
        public int subBlockSize = 1000;    //# of steps in subintegrator per integrator step
        
        public int eqNumSteps = 4000000;  
        public int eqBlockSize = 1000;
        
        public int bennettNumSteps = 4000000;
        public int benBlockSize = 1000;
        
        public int[] targWVs = {0, 1};
        public int[] refWVs = {0};
    }

}