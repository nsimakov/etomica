package etomica;

/**
* Interface for atom iterators,  yield a sequence of atoms in successive calls 
* to the next() method.
* When all atoms have been returned, hasNext() returns false.
* Iterators are often defined to progress "Up" or "Down" the set of atoms.
* "Up" and "Down" are arbitrary designations, except that the iterators guarantee
* that if atom 1 is "up list" of atom 2, then atom 2 is "down list" of atom 1.
* "Up" and "Down" relation between any atoms may change during the course of the 
* simulation, but at any instant the order is consistent and reproducible.
*
* @see IteratorFactory
* @author David Kofke
*/

public interface AtomIterator {
    
    public boolean hasNext();
    
    public boolean contains(Atom atom);
    
    public Atom reset(IteratorDirective id);
    
    public Atom reset();
    
    /**
     * Returns the next atom in the iteration sequence.  Assumes that hasNext is
     * true; calling when hasNext is false can lead to unpredictable results, and
     * may or may not cause an error or exception.
     */
    public Atom next();
    
    public void allAtoms(AtomAction act);
    
    /**
     * Defines generally the atoms subject to iteration.  Explicit meaning of basis depends
     * on specific iterator.  A call to setBasis does not leave the iterator prepared
     * for iteration; a subsequent call to one of the reset methods is required to do that.
     */
    public void setBasis(Atom atom);
    
    public Atom getBasis();
    
    public int size(); 
    
    public void setAsNeighbor(boolean b);

    /**
     * A placeholder iterator that contains no atoms and always returns false for hasNext.
     */
    public static final AtomIterator NULL = new Null();
    static final class Null implements AtomIterator {
        public boolean hasNext() {return false;}
        public boolean contains(Atom atom) {return false;}
        public Atom reset(IteratorDirective id) {return null;}
        public void setAsNeighbor(boolean b) {}
        public Atom reset() {return null;}
        public Atom next() {return null;}
        public void allAtoms(AtomAction act) {}
        public void setBasis(Atom a) {}
        public Atom getBasis() {return null;}
        public int size() {return 0;}
    }//end of Null    

}//end of AtomIterator