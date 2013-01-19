package etomica.virial;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IVectorMutable;
import etomica.config.IConformation;
import etomica.space.ISpace;

 /**
 *  Conformation of SF6
 *  7 LJ sites, rigid, no charge
 *  Reference: Samios, Molecular force field investigation for sulfur hexafluoride: A computer simulation study
 * 
 * @author shu
 * 01-18-2013
 */
public class Conformation7SiteRigidSF6 implements IConformation, java.io.Serializable{

	protected final ISpace space;
	protected final double bondL = 1.565;
	protected IVectorMutable vector;
	private static final long serialVersionUID = 1L;
	
	public Conformation7SiteRigidSF6(ISpace space){
		this.space = space;
		vector = space.makeVector();
	}

	public void initializePositions(IAtomList atomList) {
		
		IAtom n1 = atomList.getAtom(Species7SiteRigidSF6.indexS);
		n1.getPosition().E(new double[] {0, 0, 0});
		
		IAtom n2 = atomList.getAtom(Species7SiteRigidSF6.indexF1);
		n2.getPosition().E(new double[] {0, bondL, 0});
		
		IAtom n3 = atomList.getAtom(Species7SiteRigidSF6.indexF2);
		n3.getPosition().E(new double[] {0, -bondL, 0});
		
		IAtom n4 = atomList.getAtom(Species7SiteRigidSF6.indexF3);
		n4.getPosition().E(new double[] {0, 0, bondL});
		
		IAtom n5 = atomList.getAtom(Species7SiteRigidSF6.indexF4);
		n5.getPosition().E(new double[] {0, 0, -bondL});
		
		IAtom n6 = atomList.getAtom(Species7SiteRigidSF6.indexF5);
		n6.getPosition().E(new double[] {bondL,0,0});
		
		IAtom n7 = atomList.getAtom(Species7SiteRigidSF6.indexF6);
		n7.getPosition().E(new double[] {-bondL,0,0});
			
	}
	
 }
