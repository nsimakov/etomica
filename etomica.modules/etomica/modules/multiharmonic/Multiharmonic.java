package etomica.modules.multiharmonic;

import etomica.action.SimulationDataAction;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomType;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IController;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.box.Box;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.DataSourceCountTime;
import etomica.data.meter.MeterEnergy;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.P1Harmonic;
import etomica.potential.PotentialMaster;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space1d.Space1D;
import etomica.space1d.Vector1D;
import etomica.species.SpeciesSpheresMono;
import etomica.util.HistoryCollapsing;


/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 *
 * @author David Kofke
 *
 */
public class Multiharmonic extends Simulation {

    public Multiharmonic() {
        super(Space1D.getInstance());
        PotentialMaster potentialMaster = new PotentialMasterMonatomic(this);
        double x0 = 1;
        species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        ((IAtomTypeSphere)species.getLeafType()).setDiameter(0.02);
        box = new Box(space);
        addBox(box);
        box.getBoundary().setDimensions(new Vector1D(3.0));
        controller = getController();
        integrator = new IntegratorVelocityVerlet(this, potentialMaster, space);
        integrator.setBox(box);
        integrator.setTimeStep(0.02);
        integrator.setIsothermal(true);
        integrator.setTemperature(1.0);
        box.setNMolecules(species, 20);
        potentialA = new P1Harmonic(space);
        potentialA.setX0(new Vector1D(x0));
        potentialA.setSpringConstant(1.0);
        potentialMaster.addPotential(potentialA, new IAtomType[] {species.getLeafType()});
        
        box.setNMolecules(species, 20);
        
        AtomIteratorLeafAtoms iterator = new AtomIteratorLeafAtoms();
        iterator.setBox(box);
        iterator.reset();
        for (IAtomPositioned a = (IAtomPositioned)iterator.nextAtom(); a != null;
             a = (IAtomPositioned)iterator.nextAtom()) {
            a.getPosition().setX(0,x0);
        }
        activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);

        potentialB = new P1Harmonic(space);
        potentialB.setX0(new Vector1D(x0+1));
        potentialB.setSpringConstant(10);
        meter = new MeterFreeEnergy(potentialA, potentialB);
        meter.setBox(box);
        accumulator = new AccumulatorAverageCollapsing();
        dataPump = new DataPump(meter, accumulator);
        integrator.addIntervalAction(dataPump);
        
        meterEnergy = new MeterEnergy(potentialMaster, box);
        accumulatorEnergy = new AccumulatorAverageCollapsing();
        dataPumpEnergy = new DataPump(meterEnergy, accumulatorEnergy);
        integrator.addIntervalAction(dataPumpEnergy);
        
        historyEnergy = new AccumulatorHistory(new HistoryCollapsing(102, 3));
        accumulatorEnergy.addDataSink(historyEnergy, new AccumulatorAverage.StatType[] {AccumulatorAverage.StatType.AVERAGE});

        timeCounter = new DataSourceCountTime(integrator);
        
        historyEnergy.setTimeDataSource(timeCounter);
    }

    private static final long serialVersionUID = 1L;
    MeterEnergy meterEnergy;
    AccumulatorAverageCollapsing accumulatorEnergy;
    AccumulatorHistory historyEnergy;
    SpeciesSpheresMono species;
    IBox box;
    IController controller;
    P1Harmonic potentialA, potentialB;
    IntegratorVelocityVerlet integrator;
    ActivityIntegrate activityIntegrate;
    MeterFreeEnergy meter;
    AccumulatorAverageCollapsing accumulator;
    DataPump dataPump, dataPumpEnergy;
    SimulationDataAction resetAccumulators;
    DataSourceCountTime timeCounter;
}
