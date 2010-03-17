package etomica.normalmode;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IPotentialAtomic;
import etomica.api.IVectorMutable;
import etomica.atom.AtomArrayList;
import etomica.lattice.crystal.Primitive;
import etomica.space.ISpace;

public class P1ConstraintNbr implements IPotentialAtomic{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// this could take a NeighborListManager to try to speed up finding neighbors
	public P1ConstraintNbr(ISpace space, Primitive primitive, IBox box) {
	    boundary = box.getBoundary();
	    
		double l = primitive.getSize()[0];
		neighborRadiusSq = l*l/2.0;
		
		IAtomList list = box.getLeafList();
		
		//Check for neighboring sites
		drj = space.makeVector();
        drk = space.makeVector();
		neighborAtoms = new int[list.getAtomCount()][12];
        AtomArrayList tmpList = new AtomArrayList(12);

		for (int i=0; i<list.getAtomCount(); i++) {
		    IAtom atomi = list.getAtom(i);
		    tmpList.clear();
		    for (int j=0; j<list.getAtomCount(); j++) {
		        if (i==j) continue;
		        IAtom atomj = list.getAtom(j);
		        drj.Ev1Mv2(atomi.getPosition(), atomj.getPosition());
		        boundary.nearestImage(drj);
		        if (drj.squared() < neighborRadiusSq*1.01) {
		            tmpList.add(atomj);
		        }
		    }
		    for (int j=0; j<6; j++) {
		        IAtom atomj = tmpList.getAtom(0);
                drj.Ev1Mv2(atomi.getPosition(), atomj.getPosition());
                boundary.nearestImage(drj);
                drj.TE(1.0/neighborRadiusSq);
    		    neighborAtoms[i][j*2] = atomj.getLeafIndex();
    		    tmpList.remove(0);
    		    int indexj2 = findOppositeAtomIndex(atomi, tmpList);
    		    neighborAtoms[i][j*2+1] = tmpList.getAtom(indexj2).getLeafIndex();
    		    tmpList.remove(indexj2);
		    }
		}
	}
	
	public int nBody() {
	    return 1;
	}
	
	public double getRange() {
	    return Double.POSITIVE_INFINITY;
	}
	
	public void setBox(IBox box) {
	    leafList = box.getLeafList();
	}
	
	/**
	 * find atom in list that is opposite atomi from atomj (as defined by drj)
	 */
	protected int findOppositeAtomIndex(IAtom atomi, IAtomList list) {
	    for (int k=0; k<list.getAtomCount(); k++) {
	        IAtom atomk = list.getAtom(k);
            drk.Ev1Mv2(atomi.getPosition(), atomk.getPosition());
            boundary.nearestImage(drk);
            double dot = drj.dot(drk);
            if (Math.abs(dot + 1.0) < 1e-10) {
                return k;
            }
	    }
	    throw new RuntimeException("couldn't find opposite atom");
	}

	/**
	 * Returns sum of energy for all triplets containing the given atom
	 */
	public double energy(IAtomList atoms) {
	    IAtom atom = atoms.getAtom(0);
	    double u = energyi(atom);
	    if (u == Double.POSITIVE_INFINITY) {
	        return u;
	    }
		
		int atomIndex = atom.getLeafIndex();
		int[] list = neighborAtoms[atomIndex];
		for (int i=0; i<12; i++) {
		    u += energyij(leafList.getAtom(list[i]), atom);
		    if (u == Double.POSITIVE_INFINITY) {
		        return u;
		    }
		}
		return u;
	}

	/**
	 * 
	 * @param atom
	 * @return
	 */
	public double energyi(IAtom atom) {

        IVectorMutable posAtom = atom.getPosition();

        int atomIndex = atom.getLeafIndex();
        int[] list = neighborAtoms[atomIndex];
        for (int i=0; i<12; i+=2) {
            IAtom atomj = leafList.getAtom(list[i]);
            IAtom atomk = leafList.getAtom(list[i+1]);
            drj.Ev1Mv2(posAtom, atomj.getPosition());
            boundary.nearestImage(drj);
            // second-nearest neighbor should be at 2.66, 3rd nearest around 3.5
            // 3 seems to work OK
            if (drj.squared() > neighborRadiusSq*3.0) {
                return Double.POSITIVE_INFINITY;
            }
            drk.Ev1Mv2(posAtom, atomk.getPosition());
            boundary.nearestImage(drk);
            if (drk.squared() > neighborRadiusSq*3.0) {
                return Double.POSITIVE_INFINITY;
            }
            if (drj.dot(drk) > 0) {
                return Double.POSITIVE_INFINITY;
            }
        }
        return 0;
    }
	
	public double energyij(IAtom atomi, IAtom atomj) {
        IVectorMutable posAtom = atomi.getPosition();

        int atomIndex = atomi.getLeafIndex();
        int[] list = neighborAtoms[atomIndex];
        for (int i=0; i<12; i+=2) {
            IAtom atomk = leafList.getAtom(list[i]);
            if (atomk == atomj) {
                atomk = leafList.getAtom(list[i+1]);
            }
            else {
                IAtom atomkk = leafList.getAtom(list[i+1]);
                if (atomkk != atomj) {
                    continue;
                }
                // we found atomj, which means list[i] was atomk as we assumed!
            }
            drj.Ev1Mv2(posAtom, atomj.getPosition());
            boundary.nearestImage(drj);
            drk.Ev1Mv2(posAtom, atomk.getPosition());
            boundary.nearestImage(drk);
            if (drj.dot(drk) > 0) {
                return Double.POSITIVE_INFINITY;
            }
            // we tested the pair we want, so we're done
            return 0;
        }
        throw new RuntimeException("couldn't find "+atomj+" in "+atomi+" neighbors");
	}

	protected final int[][] neighborAtoms;
	protected final IVectorMutable drj, drk;
	protected double neighborRadiusSq;
	protected final IBoundary boundary;
	protected IAtomList leafList;
}