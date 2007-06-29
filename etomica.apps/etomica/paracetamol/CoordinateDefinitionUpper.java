package etomica.paracetamol;

import etomica.atom.AtomAgentManager;
import etomica.atom.IAtom;
import etomica.atom.ISpeciesAgent;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.box.Box;
import etomica.space.IVector;
import etomica.space.Space;

/**
 * An abstract class that defines the real-space generalized coordinates that are
 * summed (over molecules) to form collective coordinates (from which normal
 * coordinates are determined). Typically these generalized coordinates are
 * given by the displacement of the molecule from its nominal lattice position.
 * For non-spherical molecules it will also include elements related to the
 * orientation.  This class provides methods to compute the k-dependent collective generalized
 * coordinates, obtained by Fourier-summing the generalized coordinates over the atoms
 * in a box.
 * 
 * @author Andrew Schultz, David Kofke
 */
public abstract class CoordinateDefinitionUpper {

    public CoordinateDefinitionUpper(int coordinateDim) {
        this.coordinateDim = coordinateDim;
        iterator = new AtomIteratorAllMolecules();
    }

    /**
     * Returns the number of generalized coordinates associated with each
     * molecule. If no orientational coordinates are involved, this value is
     * typically equal to the space dimension.
     */
    public int getCoordinateDim() {
        return coordinateDim;
    }

    /**
     * Calculates the generalized coordinates for the given molecules in their
     * current position and orientation.  The result is stored in the |u| field.
     * 
     * @param molecule
     *            The molecule of interest, which should be those forming a unit cell of the lattice
     */
    protected abstract double[] calcU(IAtom molecule);

    /**
     * Initializes the CoordinateDefinition for the given molecule and
     * associates the molecule with the given index. Typically this will be
     * called when the molecule is in a nominal position and orientation, and
     * the generalized coordinates for the molecule will be defined with respect
     * to this nominal case.
     */
    protected void initNominalU(IAtom molecule){
    }
    /**
     * Set the molecule to a position and orientation that corresponds to the
     * given generalized coordinate. |u| must be of length getCoordinateDim()
     * 
     * @param molecule
     *            The molecule of interest
     * @param newU
     *            The generalized coordinate that defines the position and
     *            orientation to which the molecule will be set by this method.
     */
    public abstract void setToU(IAtom molecule, double[] newU);

    /**
     * Calculates the complex "T vector", which is collective coordinate given
     * by the Fourier sum (over atoms) of the generalized coordinate vector.
     * 
     * @param k
     *            the wave vector
     * @param realT
     *            outputs the real component of the T vector
     * @param imaginaryT
     *            outputs the imaginary component of the T vector
     */
    //in principle this should be returning Complex[] and not returning the values through the args
    public void calcT(IVector k, double[] realT, double[] imaginaryT) {
        for (int i = 0; i < coordinateDim; i++) {
            realT[i] = 0;
            imaginaryT[i] = 0;
        }
        iterator.reset();
        // sum T over atoms
        for (IAtom atom = iterator.nextAtom(); atom != null;
             atom = iterator.nextAtom()) {
            double[] u = calcU(atom);
            IVector latticePosition = (IVector)siteManager.getAgent(atom);
            double kR = k.dot(latticePosition);
            double coskR = Math.cos(kR);
            double sinkR = Math.sin(kR);
            for (int i = 0; i < coordinateDim; i++) {
                realT[i] += coskR * u[i];
                imaginaryT[i] += sinkR * u[i];
            }
        }

        for (int i = 0; i < coordinateDim; i++) {
            realT[i] /= sqrtN;
            imaginaryT[i] /= sqrtN;
        }

    }

    public Box getBox() {
        return box;
    }

    public void setBox(Box box) {
        this.box = box;
        siteManager = new AtomAgentManager(new SiteSource(box.getSpace()), box);
        int N = box.getSpeciesMaster().moleculeCount();
        sqrtN = Math.sqrt(N);

        iterator.setBox(box);
        iterator.reset();
        for (IAtom atom = iterator.nextAtom(); atom != null;
             atom = iterator.nextAtom()) {
            initNominalU(atom);
        }

    }
    
    public void setCell(Box box){
    	this.box = box;
    	cellManager = new AtomAgentManager(new SiteSource(box.getSpace()),box);
    	int N = box.getSpeciesMaster().moleculeCount();
    }

    public IVector getLatticePosition(IAtom atom) {
        return (IVector)siteManager.getAgent(atom);
    }

    protected final int coordinateDim;
    protected Box box;
    public double sqrtN;
    public final AtomIteratorAllMolecules iterator;
    private AtomAgentManager siteManager, cellManager;
    
    protected static class SiteSource implements AgentSource {
        
        protected SiteSource(Space space) {
            this.space = space;
        }
        public Class getAgentClass() {
            return IVector.class;
        }
        public Object makeAgent(IAtom atom) {
            IVector vector = space.makeVector();
            if(atom instanceof ISpeciesAgent) return null;
            vector.E(atom.getType().getPositionDefinition().position(atom));
            return vector;
        }
        public void releaseAgent(Object agent, IAtom atom) {
            //nothing to do
        }
        
        private final Space space;
    }

}
