package etomica.dimer;

import etomica.action.BoxImposePbc;
import etomica.action.BoxInflate;
import etomica.action.CalcVibrationalModes;
import etomica.action.WriteConfiguration;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomType;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISpecies;
import etomica.api.IVectorMutable;
import etomica.atom.MoleculeArrayList;
import etomica.box.Box;
import etomica.chem.elements.ElementSimple;
import etomica.config.Configuration;
import etomica.config.ConfigurationFile;
import etomica.config.ConfigurationLattice;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.lattice.LatticeCubicFcc;
import etomica.listener.IntegratorListenerAction;
import etomica.potential.P2LennardJones;
import etomica.potential.PotentialMaster;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularSlit;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.util.numerical.CalcGradientDifferentiable;

/**
 * Simulation using Henkelman's Dimer method to find a saddle point for
 * an adatom on a surface, modeled with LJ.
 * 
 * @author msellers
 *
 */

public class SimDimerLJadatom extends Simulation{

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DimerLJadatom";
    public final PotentialMaster potentialMaster;
    public IntegratorVelocityVerlet integratorMD;
    public IntegratorDimerRT integratorDimer;
    public IntegratorDimerMin integratorDimerMin;
    public IBox box;
    public IVectorMutable [] saddle, normal;
    public SpeciesSpheresMono fixed, movable;
//    public P2LennardJones potential;
    public ActivityIntegrate activityIntegrateMD, activityIntegrateDimer, activityIntegrateMin;
    public CalcGradientDifferentiable calcGradientDifferentiable;
    public CalcVibrationalModes calcVibrationalModes;
    public double [][] dForces;
    public int [] d, modeSigns;
    public double [] positions;
    public double [] lambdas, frequencies;
    public IMoleculeList movableSet;
    public IVectorMutable adAtomPos;
    public Boolean saddleFine, calcModes, minSearch, normalDir;
    

    public SimDimerLJadatom() {
    	super(Space3D.getInstance(), true);
    	potentialMaster = new PotentialMasterMonatomic(this);
    	
    //SIMULATION BOX
        box = new Box(new BoundaryRectangularSlit(0, 5, space), space);
        addBox(box);
        
    //SPECIES
    	double sigma = 1.0;
        fixed = new SpeciesSpheresMono(this, space, new ElementSimple("A", Double.POSITIVE_INFINITY));
        movable = new SpeciesSpheresMono(this, space);      
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        ((IAtomTypeSphere)fixed.getLeafType()).setDiameter(sigma);
        ((IAtomTypeSphere)movable.getLeafType()).setDiameter(sigma);
    	
        // Must be in same order as the respective species is added to SpeciesManager
        box.setNMolecules(fixed, 256);    	
    	
        BoxInflate inflater = new BoxInflate(box, space);
        inflater.setTargetDensity(1);
        inflater.actionPerformed();
    	
//    	potential = new P2LennardJones(space, sigma, 1.0);
//		potentialMaster.addPotential(, new IAtomTypeLeaf[]{fixed.getLeafType(), fixed.getLeafType()});
		potentialMaster.addPotential(new P2LennardJones(space, sigma, 1.0), new IAtomType[]{movable.getLeafType(), fixed.getLeafType()});
		potentialMaster.addPotential(new P2LennardJones(space, sigma, 1.0), new IAtomType[]{movable.getLeafType(), movable.getLeafType()});
        
    //CRYSTAL
        Configuration config = new ConfigurationLattice(new LatticeCubicFcc(space), space);
        config.initializeCoordinates(box); 
       
        //ADATOM CREATION AND PLACEMENT
        
        IMolecule iMolecule = movable.makeMolecule();
        box.addMolecule(iMolecule);
        adAtomPos = ((IAtomPositioned)iMolecule.getChildList().getAtom(0)).getPosition();
        //adAtomPos = getSpace().makeVector();
        adAtomPos.setX(0, 3.5);
        adAtomPos.setX(1, -0.30);
        adAtomPos.setX(2, -0.30);
        IVectorMutable newBoxLength = space.makeVector();
        newBoxLength.E(box.getBoundary().getDimensions());
        newBoxLength.setX(0, 2.0*adAtomPos.x(0)+2.0);
        box.getBoundary().setDimensions(newBoxLength);

    }
    
    public void setMovableAtoms(double distance, IVectorMutable center){
        //distance = distance*distance;
        IVectorMutable rij = space.makeVector();
        MoleculeArrayList movableList = new MoleculeArrayList();
        IMoleculeList loopSet = box.getMoleculeList();
        for (int i=0; i<loopSet.getMoleculeCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)loopSet.getMolecule(i).getChildList().getAtom(0)).getPosition());
            if(rij.x(0) > (box.getBoundary().getDimensions().x(0) - 3.0)){continue;}
            //box.getBoundary().nearestImage(rij);
            if(rij.x(0)< distance){
               movableList.add(loopSet.getMolecule(i));
            } 
        }
        for (int i=0; i<movableList.getMoleculeCount(); i++){
            IMolecule newMolecule = movable.makeMolecule();
            box.addMolecule(newMolecule);
            ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)movableList.getMolecule(i).getChildList().getAtom(0)).getPosition());
            box.removeMolecule(movableList.getMolecule(i));
        }
        movableSet = box.getMoleculeList(movable);
    }

    
    //Must be run after setMovableAtoms
    public void removeAtoms(double distance, IVectorMutable center){
        distance = distance*distance;
        IVectorMutable rij = space.makeVector();
        
        IMoleculeList loopSet = box.getMoleculeList(movable);
        for (int i=0; i<loopSet.getMoleculeCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)loopSet.getMolecule(i).getChildList().getAtom(0)).getPosition());
            box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               box.removeMolecule(loopSet.getMolecule(i));
            } 
        }   
    }
    
    public void initializeConfiguration(String fileName){
        ConfigurationFile config = new ConfigurationFile(fileName);
        config.initializeCoordinates(box);
    }
    
    public void generateConfigs(String fileName, double percentd){       
        
        IVectorMutable workVector = space.makeVector();
        IVectorMutable [] currentPos = new IVectorMutable [movableSet.getMoleculeCount()];
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i].E(((IAtomPositioned)movableSet.getMolecule(i).getChildList().getAtom(0)).getPosition());
        }
        
        //Create multiple configurations
        for(int m=0; m<50; m++){
            WriteConfiguration genConfig = new WriteConfiguration(space);
            genConfig.setBox(box);
            genConfig.setConfName(fileName+"_config_"+m);
            //Displaces atom's by at most +/-0.03 in each coordinate
            for(int i=0; i<movableSet.getMoleculeCount(); i++){
                IVectorMutable atomPosition = ((IAtomPositioned)movableSet.getMolecule(i).getChildList().getAtom(0)).getPosition();
                for(int j=0; j<3; j++){
                    workVector.setX(j,percentd*random.nextGaussian());
                }
                atomPosition.Ev1Pv2(currentPos[i],workVector);
            }
            genConfig.actionPerformed();            
        }
    }
        
    public void enableMolecularDynamics(long maxSteps){
        integratorMD = new IntegratorVelocityVerlet(this, potentialMaster, space);
        integratorMD.setTimeStep(0.01);
        integratorMD.setTemperature(0.1);
        integratorMD.setThermostatInterval(100);
        integratorMD.setIsothermal(true);
        integratorMD.setBox(box);
        activityIntegrateMD = new ActivityIntegrate(integratorMD);
        BoxImposePbc imposePbc = new BoxImposePbc(box, space);
        integratorMD.getEventManager().addListener(new IntegratorListenerAction(imposePbc));
        getController().addAction(activityIntegrateMD);
        activityIntegrateMD.setMaxSteps(maxSteps);
    }
    
    public void enableDimerSearch(String fileName, long maxSteps, Boolean orthoSearch, Boolean fine){
        
        integratorDimer = new IntegratorDimerRT(this, potentialMaster, new ISpecies[]{movable}, space);
        integratorDimer.setBox(box);
        integratorDimer.setOrtho(orthoSearch, false);
        if(fine){
            ConfigurationFile configFile = new ConfigurationFile(fileName+"_saddle");
            configFile.initializeCoordinates(box);
            
            integratorDimer.setFileName(fileName+"_fine");
            integratorDimer.deltaR = 0.0005;
            integratorDimer.dXl = 10E-5;       
            integratorDimer.deltaXmax = 0.005;
            integratorDimer.dFsq = 0.0001*0.0001;
            integratorDimer.dFrot = 0.01;
        }
        integratorDimer.setFileName(fileName);
       activityIntegrateDimer = new ActivityIntegrate(integratorDimer);
        integratorDimer.setActivityIntegrate(activityIntegrateDimer);
        getController().addAction(activityIntegrateDimer);
        activityIntegrateDimer.setMaxSteps(maxSteps);
    }
        
    public void enableMinimumSearch(String fileName, Boolean normalDir){
        
        integratorDimerMin = new IntegratorDimerMin(this, potentialMaster, new ISpecies[]{movable}, normalDir, space);
        integratorDimerMin.setBox(box);
        integratorDimerMin.setFileName(fileName);
        activityIntegrateMin = new ActivityIntegrate(integratorDimerMin);
        integratorDimerMin.setActivityIntegrate(activityIntegrateMin);
        getController().addAction(activityIntegrateMin);
    }
    
    public void randomizePositions(){
        IVectorMutable workVector = space.makeVector();
        IMoleculeList loopSet3 = box.getMoleculeList(movable);
        IVectorMutable [] currentPos = new IVectorMutable [loopSet3.getMoleculeCount()];
        double offset = 0;
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i] = (((IAtomPositioned)loopSet3.getMolecule(i).getChildList().getAtom(0)).getPosition());
            for(int j=0; j<3; j++){
                offset = random.nextGaussian()/10.0;
                if(Math.abs(offset)>0.1){offset=0.1;}
                workVector.setX(j,offset);
            }
            currentPos[i].PE(workVector);
        }
    }
    
    public static void main(String[] args) {
       
        final SimDimerLJadatom sim = new SimDimerLJadatom();
        IVectorMutable vect = sim.getSpace().makeVector();
        vect.setX(0, 3.5);
        vect.setX(1, 0.0);
        vect.setX(2, 0.0);
        
        sim.setMovableAtoms(2.0, vect);
        
        //sim.initializeConfiguration("0");

        //sim.enableDimerSearch("0", 800, false, false);
        //sim.randomizePositions();
        //sim.integratorDimer.setRotNum(0);
        sim.enableMinimumSearch("s_1", false);


        SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME,1, sim.getSpace(), sim.getController());
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));

        sim.integratorDimerMin.getEventManager().addListener(new IntegratorListenerAction(simGraphic.getPaintAction(sim.box)));

        ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());
        
        colorScheme.setColor(sim.fixed.getLeafType(),java.awt.Color.gray);
        colorScheme.setColor(sim.movable.getLeafType(),java.awt.Color.red);

        simGraphic.makeAndDisplayFrame(APP_NAME);
    }

}
