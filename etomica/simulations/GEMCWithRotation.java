package etomica.simulations;
import etomica.Default;
import etomica.IntegratorGEMC;
import etomica.MCMoveRotate;
import etomica.MeterDensity;
import etomica.P2LennardJones;
import etomica.Phase;
import etomica.PotentialMaster;
import etomica.Simulation;
import etomica.Space;
import etomica.Space2D;
import etomica.Species;
import etomica.SpeciesSpheresRotating;
import etomica.action.PhaseImposePbc;
import etomica.action.activity.ActivityIntegrate;

/**
 * Simple Gibbs-ensemble Monte Carlo simulation of rotating molecules.
 */

//in present form uses just a LJ potential, so orientation is irrelevant

public class GEMCWithRotation extends Simulation {
    
    public GEMCWithRotation() {
        this(Space2D.INSTANCE);
    }
    
    public GEMCWithRotation(Space space) {
        super(space, new PotentialMaster(space));
        Default.ATOM_SIZE = 1.2;
        Default.UNIT_SYSTEM = new etomica.units.systems.LJ();
        Default.TEMPERATURE = Default.UNIT_SYSTEM.temperature().toSim(0.420);
        IntegratorGEMC integrator = new IntegratorGEMC(potentialMaster, space);
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
	    activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
	    activityIntegrate.setInterval(400);
	    
	    species = new SpeciesSpheresRotating(this);
        species.setNMolecules(200);

	    phase1 = new Phase(space);	    
	    MCMoveRotate mcRotate1 = new MCMoveRotate(potentialMaster, space);
	    mcRotate1.setPhase(new Phase[] {phase1});
        integrator.addMCMove(mcRotate1);

	    phase2 = new Phase(space);
	    MeterDensity meter2 = new MeterDensity();	    
	    MCMoveRotate mcRotate2 = new MCMoveRotate(potentialMaster, space);
	    mcRotate2.setPhase(new Phase[] {phase2});
        integrator.addMCMove(mcRotate2);

        integrator.setPhase(new Phase[] {phase1, phase2});
        
        MeterDensity meterDensity = new MeterDensity();

	    potential = new P2LennardJones();
	    potential.setSigma(species.getDiameter());

        this.potentialMaster.setSpecies(potential,new Species[] {species, species});

        phase1.speciesMaster.addSpecies(species);
        phase2.speciesMaster.addSpecies(species);
        integrator.addIntervalListener(new PhaseImposePbc(phase1));
        integrator.addIntervalListener(new PhaseImposePbc(phase2));

	    phase2.setDensity(0.1);
    }//end of constructor        
    
    public Phase phase1, phase2;
    public IntegratorGEMC integrator;
    public SpeciesSpheresRotating species;
    public P2LennardJones potential;
}