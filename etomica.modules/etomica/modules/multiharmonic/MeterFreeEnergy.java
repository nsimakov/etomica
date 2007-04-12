package etomica.modules.multiharmonic;

import etomica.atom.IAtom;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.data.DataSource;
import etomica.data.DataSourceScalar;
import etomica.phase.Phase;
import etomica.potential.P1Harmonic;
import etomica.units.Energy;


/**
 * Computes the free-enery difference through free-energy perturbation.
 * Written specifically for harmonic 1-body potential, but wouldn't be hard
 * to modify for more general cases.
 *
 * @author David Kofke
 *
 */
public class MeterFreeEnergy extends DataSourceScalar implements DataSource {
    
    public MeterFreeEnergy(P1Harmonic reference, P1Harmonic target) {
        super("Free energy", Energy.DIMENSION);
        this.reference = reference;
        this.target = target;
    }
    
    public double getDataAsScalar() {
        iterator.reset();
        double sum = 0.0;
        while(iterator.hasNext()) {
            IAtom a = iterator.nextAtom();
            sum += target.energy(a) - reference.energy(a);
        }
        return Math.exp(-sum);
    }
    
    public void setPhase(Phase phase) {
        iterator.setPhase(phase);
        this.phase = phase;
    }
    
    public Phase getPhase() {
        return phase;
    }

    private static final long serialVersionUID = 1L;
    AtomIteratorLeafAtoms iterator = new AtomIteratorLeafAtoms();
    Phase phase;
    P1Harmonic reference, target;
}
