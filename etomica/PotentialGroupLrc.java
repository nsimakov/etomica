package etomica;

/**
 * Group that contains potentials used for long-range correction.
 * One instance of this class is added to the PotentialManager of
 * a Simulation when the first Potential0Lrc class is instantiated.
 * All subsequently created Potential0Lrc classes are added to this group.
 *
 * @see Potential0Lrc
 * @author David Kofke
 */
 
public class PotentialGroupLrc extends PotentialGroup {

    public PotentialGroupLrc(PotentialMaster parent) {
        super(0, parent);
    }
    
    /**
     * Performs given PotentialCalculation on all LRC potentials added to this group.
     * Checks that group is enabled, phase is not null, that it has lrcEnabled,
     * and that the given IteratorDirective has includeLrc set to true; if all
     * are so, calculation is performed.
     */
    public void calculate(Phase phase, IteratorDirective id, PotentialCalculation pc) {
        if(!enabled || phase == null || !phase.isLrcEnabled() || !id.includeLrc) return;
		for(PotentialLinker link=first; link!=null; link=link.next) {
			if(id.excludes(link.potential)) continue; //see if potential is ok with iterator directive
			((Potential0)link.potential).calculate(phase, id, pc);
		}//end for
    }//end calculate
    
}//end of PotentialGroupLrc