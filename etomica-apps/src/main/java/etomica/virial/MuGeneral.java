/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import etomica.atom.IAtomList;
import etomica.atom.IAtomOriented;
import etomica.box.Box;
import etomica.molecule.IMolecule;
import etomica.molecule.IMoleculeList;
import etomica.potential.IPotential;
import etomica.potential.IPotentialMolecular;
import etomica.space.Vector;

/**
 * @author shu
 *
 * mu_i * mu_j
 */
public class MuGeneral implements MayerFunction, java.io.Serializable {
	
	private double mu;
	private IPotentialMolecular potential;
	public MuGeneral(double mu, IPotentialMolecular potential) {
		this.mu=mu;
		this.potential=potential;
	}

	public double f(IMoleculeList pair, double r2, double beta) {

		IMolecule molecule1 = pair.getMolecule(0);
		IMolecule molecule2 = pair.getMolecule(1);
		IAtomList atomList1 = molecule1.getChildList();
		IAtomList atomList2 = molecule2.getChildList();
        IAtomOriented atom1 = (IAtomOriented)atomList1.get(0);
        IAtomOriented atom2 = (IAtomOriented)atomList2.get(0);
		// should have a loop to loop over all the atoms in the molecules 
        Vector v1 = atom1.getOrientation().getDirection();//dipole1
        Vector v2 = atom2.getOrientation().getDirection();//dipole2
        double cos12= v1.dot(v2);
		double mu_Dot_mu= mu*mu*cos12;
		double sum=mu_Dot_mu;
		//System.out.println("cos (mu1) and (mu2): " + cos12);
		//System.out.println("(mu) : " + mu);
		return sum;
			
	}

	/* (non-Javadoc)
	 * @see etomica.virial.MayerFunction#getPotential()
	 */  
	public IPotential getPotential() {
		// TODO Auto-generated method stub
		return potential;
	}
	
	public void setBox(Box newBox) {
	    potential.setBox(newBox);
	}
}
