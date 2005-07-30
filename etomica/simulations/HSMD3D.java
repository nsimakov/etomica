// Source file generated by Etomica

package etomica.simulations;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import etomica.AtomType;
import etomica.ConfigurationLattice;
import etomica.Default;
import etomica.Phase;
import etomica.Simulation;
import etomica.Space;
import etomica.Species;
import etomica.SpeciesSpheresMono;
import etomica.action.activity.ActivityIntegrate;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.CriterionSimple;
import etomica.nbr.NeighborCriterion;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterNbr;
import etomica.potential.P2HardSphere;
import etomica.space3d.Space3D;

public class HSMD3D extends Simulation {

    public Phase phase;
    public IntegratorHard integrator;
    public SpeciesSpheresMono species;
    public P2HardSphere potential;
    
    public HSMD3D() {
        this(Space3D.getInstance());
    }
    

    private HSMD3D(Space space) {
//        super(space, new PotentialMaster(space));
        super(space, true, new PotentialMasterNbr(space));

        int numAtoms = 256;
        double neighborRangeFac = 1.6;
        Default.makeLJDefaults();
        Default.ATOM_SIZE = 1.0;
        Default.BOX_SIZE = 14.4573*Math.pow((numAtoms/2020.0),1.0/3.0);
        int nCells = (int)(Default.BOX_SIZE/neighborRangeFac);
        System.out.println("nCells: "+nCells);
        ((PotentialMasterNbr)potentialMaster).setNCells(nCells);

        integrator = new IntegratorHard(potentialMaster);
        integrator.setIsothermal(false);
        integrator.setTimeStep(0.01);
        this.register(integrator);

        NeighborListManager nbrManager = ((PotentialMasterNbr)potentialMaster).getNeighborManager();
        nbrManager.setRange(Default.ATOM_SIZE*1.6);
        nbrManager.getPbcEnforcer().setApplyToMolecules(false);
        integrator.addListener(nbrManager);

        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheresMono(this);
        species.setNMolecules(numAtoms);
//        Crystal crystal = new LatticeCubicFcc(space);
//        ConfigurationLattice configuration = new ConfigurationLattice(space, crystal);
//        phase.setConfiguration(configuration);
        potential = new P2HardSphere(space);
//        this.potentialMaster.setSpecies(potential,new Species[]{species,species});

        NeighborCriterion criterion = new CriterionSimple(space,potential.getRange(),neighborRangeFac*potential.getRange());
        potential.setCriterion(criterion);
        potentialMaster.setSpecies(potential,new Species[]{species,species});

        nbrManager.addCriterion(criterion,new AtomType[]{species.getFactory().getType()});

        phase = new Phase(this);
        new ConfigurationLattice(new LatticeCubicFcc()).initializeCoordinates(phase);
        integrator.addPhase(phase);
 //       integrator.addIntervalListener(new PhaseImposePbc(phase));
        
        //ColorSchemeByType.setColor(speciesSpheres0, java.awt.Color.blue);

 //       MeterPressureHard meterPressure = new MeterPressureHard(integrator);
 //       DataAccumulator accumulatorManager = new DataAccumulator(meterPressure);
        // 	DisplayBox box = new DisplayBox();
        // 	box.setDatumSource(meterPressure);
 //       phase.setDensity(0.7);
    } //end of constructor

}//end of class
