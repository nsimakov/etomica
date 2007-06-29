package etomica.threaded;

import etomica.atom.AtomAgentManager;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomSet;
import etomica.atom.IAtom;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.atom.iterator.AtomsetIterator;
import etomica.integrator.IntegratorBox;
import etomica.integrator.IntegratorVelocityVerlet.MyAgent;
import etomica.box.Box;
import etomica.potential.IPotential;
import etomica.potential.PotentialCalculation;
import etomica.potential.PotentialCalculationForceSum;
import etomica.space.IVector;

public class PotentialCalculationForceSumThreaded extends PotentialCalculationForceSum implements PotentialCalculationThreaded, AgentSource{

	final protected PotentialCalculationForceSum[] pc;
	protected AtomLeafAgentManager[] atomAgentManager;
    
	public PotentialCalculationForceSumThreaded(PotentialCalculationForceSum[] pc) {
		this.pc = pc;
	}

    public void reset(){
        super.reset();
        for (int i=0; i<pc.length; i++){
            pc[i].reset();
        }
    }
    
	public void setAgentManager(AtomAgentManager agentManager) {
        super.setAgentManager(agentManager);
        atomAgentManager = new AtomLeafAgentManager[pc.length];
        
        for (int i=0; i<pc.length; i++){
            atomAgentManager[i] = new AtomLeafAgentManager(this, agentManager.getBox());
            pc[i].setAgentManager(atomAgentManager[i]);
            agentManager.getBox();
		}
		
	}
	
	public void doCalculation(AtomsetIterator iterator, IPotential potential) {
		throw new RuntimeException("This is not the correct 'doCalculation' to call.");
	}
	
	/* (non-Javadoc)
	 * @see etomica.threads.PotentialCalculationThreaded#getPotentialCalculations()
	 */
	public PotentialCalculation[] getPotentialCalculations(){
		return pc;
	}
	
	public void writeData(){
       
		Box box = integratorAgentManager.getBox();
        AtomSet atomArrayList = box.getSpeciesMaster().getLeafList();
      
        for(int j=0; j<atomArrayList.getAtomCount(); j++){
            IVector force = ((IntegratorBox.Forcible)integratorAgentManager.getAgent(atomArrayList.getAtom(j))).force();
      
            for(int i=0; i<pc.length; i++){
                force.PE(((IntegratorBox.Forcible)atomAgentManager[i].getAgent(atomArrayList.getAtom(j))).force());
               
                
            }
        }
            
	}
    
    public Class getAgentClass() {
        return MyAgent.class;
    }

    public final Object makeAgent(IAtom a) {
        return new MyAgent(integratorAgentManager.getBox().getSpace());
    }
    
    public void releaseAgent(Object object, IAtom atom){
        
    }
    
}
