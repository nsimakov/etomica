package etomica;

/**
 * Evaluates the virial summed over all iterated atoms.
 *
 * @author David Kofke
 */
 
 /* History
  * 10/12/02 (DAK) new
  * 08/29/03 (DAK) added actionPerformed(AtomSet) method because method made
  * abstract in PotentialCalculation
  */
public class PotentialCalculationVirialSum extends PotentialCalculation 
											 implements PotentialCalculation.Summable {
											 	
	private Potential2.Soft p2Soft;
	
    protected double sum = 0.0;
        
    public PotentialCalculationVirialSum() {}
        
    public PotentialCalculation.Summable reset() {sum = 0.0; return this;}
    public double sum() {return sum;}

	public PotentialCalculation set(Potential2 p2) {
		if(!(p2 instanceof Potential2.Soft)) throw new RuntimeException("Error: PotentialCalculationVirialSum being used with potential that is not soft 2-body type");
		p2Soft = (Potential2.Soft)p2;
		return super.set(p2);
	}
    
    //pair
    public void actionPerformed(AtomPair pair) {
         sum += p2Soft.virial(pair);
    }//end of calculate
    
	public void actionPerformed(Atom atom) {
		throw new etomica.exception.MethodNotImplementedException();
	}

	public void actionPerformed(Atom3 atom3) {
		throw new etomica.exception.MethodNotImplementedException();
	}

	public void actionPerformed(AtomSet atomN) {
		throw new etomica.exception.MethodNotImplementedException();
	}

 }//end VirialSum
