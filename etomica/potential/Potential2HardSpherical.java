package etomica.potential;

import etomica.atom.AtomSet;
import etomica.atom.IAtomPositioned;
import etomica.box.Box;
import etomica.space.IVector;
import etomica.space.NearestImageTransformer;
import etomica.space.Space;

/**
 * Methods for a hard (impulsive), spherically-symmetric pair potential.
 * Subclasses must provide a concrete definition for the energy (method u(double)).
 */

public abstract class Potential2HardSpherical extends Potential2 implements PotentialHard, Potential2Spherical {
   
	public Potential2HardSpherical(Space space) {
	    super(space);
        dr = space.makeVector();
	}
	
	/**
    * The pair energy u(r^2) with no truncation applied.
    * @param the square of the distance between the particles.
    */
    public abstract double u(double r2);

    /**
     * Energy of the pair as given by the u(double) method, with application
     * of any PotentialTruncation that may be defined for the potential.  This
     * does not take into account any false positioning that the Integrator may
     * be using.
     */
    public double energy(AtomSet pair) {
        IAtomPositioned atom0 = (IAtomPositioned)pair.getAtom(0);
        IAtomPositioned atom1 = (IAtomPositioned)pair.getAtom(1);

        dr.Ev1Mv2(atom1.getPosition(), atom0.getPosition());
        nearestImageTransformer.nearestImage(dr);
        return u(dr.squared());
    }
    
    public void setBox(Box box) {
        nearestImageTransformer = box.getBoundary();
    }

    protected final IVector dr;
    protected NearestImageTransformer nearestImageTransformer;
}
