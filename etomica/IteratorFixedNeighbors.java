package simulate;

/**
 * Iterator that performs pair iterations over a fixed set of atoms neighboring each atom
 * Used for systems (e.g., solids) in which the neighbors are not expected to change during the simulation
 * Creates an up list and a down list of neighbors for each atom.  Whether a neighbor is on the up or down list of a 
 * particular atom depends on whether it was generated by the upAtomIterator or downAtomIterator of a
 * base iterator associated with this class
 */
 
 //Not yet completed

    public class IteratorFixedNeighbors extends Iterator {
        private int neighborCount = 2;
      //baseIterator is used to generate atoms and pairs for making neighbor lists
      //It determines which atoms are up or down from a given atom
        private Iterator baseIterator;
        public IteratorFixedNeighbors(Phase p) {
            super(p);
            baseIterator = new Iterator(p);
        }
        public void setNeighborCount(int c) {
            if(neighborCount < 1) return;
            neighborCount = c; 
            reset();
        }
        public int getNeighborCount() {return neighborCount;}
        
        public void setBaseIterator(Iterator it) {baseIterator = it; reset();}
        public Iterator getBaseIterator() {return baseIterator;}
        
        public Atom.Iterator makeAtomIteratorUp() {return baseIterator.makeAtomIteratorUp();}
        public Atom.Iterator makeAtomIteratorDown() {return baseIterator.makeAtomIteratorDown();}
        public AtomPair.Iterator.A makeAtomPairIteratorUp() {System.out.println("up");return new AtomPairUp(phase);}  //inner class defined below
        public AtomPair.Iterator.A makeAtomPairIteratorDown() {System.out.println("down");return new AtomPairDown(phase);} //inner class defined below
        public void addMolecule(Molecule m) {reset();}
        public void deleteMolecule(Molecule m) {reset();}
        public void reset() {  //loop through all atoms, setting lists of neighbors for each
            Atom.Iterator atomUp = baseIterator.makeAtomIteratorUp();
            AtomPair.Iterator.A apiUp = baseIterator.makeAtomPairIteratorUp();
            AtomPair.Iterator.A apiDown = baseIterator.makeAtomPairIteratorDown();
            atomUp.reset();
            while(atomUp.hasNext()) {      //atom loop
                Atom a = atomUp.next();
                a.atomLink = new Atom.Linker[2];  //set array for first upList and downList linkers
                Atom.Linker lastUp = null;        //last linker added to uplist
                Atom.Linker lastDown = null;      //last linker added to downlist
                apiUp.reset(a,true);              //reset pair iterators
                apiDown.reset(a,true);
                AtomPair.Linker upPairLink = AtomPair.distanceSort(apiUp);  //created distance-ordered list of pairs up from atom
                AtomPair.Linker downPairLink = AtomPair.distanceSort(apiDown); //create distance-ordered list down from atom
                double upR2 = (upPairLink!=null) ? upPairLink.pair().r2() : Double.MAX_VALUE;  //get separation for first pair in uplist
                double downR2 = (downPairLink!=null) ? downPairLink.pair().r2() : Double.MAX_VALUE; //separation for first pair in downlist
                for(int i=0; i<neighborCount; i++) {  //neighbor setup loop
                    if(upR2 < downR2) {  //next neighbor is from uplist
                        if(lastUp != null) {  //this is not first in uplist
                            lastUp.setNext(new Atom.Linker(upPairLink.pair().atom2()));
                            lastUp = lastUp.next();
                        }
                        else {                //first in uplist
                            a.atomLink[0] = new Atom.Linker(upPairLink.pair().atom2());
                            lastUp = a.atomLink[0];
                        }
                        upPairLink = upPairLink.next();  //advance to next atompair in uplist
                        if(upPairLink == null) {         //end of uplist
                            if(downPairLink == null) {break;}  //no more atoms in either list; break out of for-loop
                            upR2 = Double.MAX_VALUE;
                        }
                        else {upR2 = upPairLink.pair().r2();}
                    }
                    else {             //next neighbor is from downList; procedure same as above
                        if(lastDown != null) {
                            lastDown.setNext(new Atom.Linker(downPairLink.pair().atom2()));
                            lastDown = lastDown.next();
                        }
                        else {
                            a.atomLink[1] = new Atom.Linker(downPairLink.pair().atom2());
                            lastDown = a.atomLink[1];
                        }
                        downPairLink = downPairLink.next();
                        if(downPairLink == null) {  //end of uplist
                            if(upPairLink == null) {break;}  //no more atoms in either list
                            downR2 = Double.MAX_VALUE;
                        }
                        else {downR2 = downPairLink.pair().r2();}
                    } //end of if/else block
                } //end of for loop
            } //end of while loop
        } //end of reset method
        
        protected static class AtomPairUp implements AtomPair.Iterator.A {
            private Atom.Linker nextLink;
            private final AtomPair pair;
            AtomPairUp(Phase p) {pair = new AtomPair(p);}
            public void reset(Atom a, boolean b) {pair.atom1 = a; nextLink = a.atomLink[0];}
            public boolean hasNext() {return nextLink != null;}
            public AtomPair next() {
                pair.atom2 = nextLink.atom();
                pair.reset();
                nextLink = nextLink.next(); 
                return pair;
            }
        }
        
        protected static class AtomPairDown implements AtomPair.Iterator.A {
            private Atom.Linker nextLink;
            private final AtomPair pair;
            AtomPairDown(Phase p) {pair = new AtomPair(p);}
            public void reset(Atom a, boolean b) {pair.atom1 = a; nextLink = a.atomLink[1];}
            public boolean hasNext() {return nextLink != null;}
            public AtomPair next() {
                pair.atom2 = nextLink.atom();
                pair.reset();
                nextLink = nextLink.next(); 
                return pair;
            }
        }  //end of AtomPairDown
   
    } //end of IteratorFixedNeighbors
