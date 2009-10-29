package etomica.models.nitrogen;

import etomica.api.IAtomTypeSphere;
import etomica.api.IMolecule;
import etomica.atom.Atom;
import etomica.atom.AtomLeafDynamic;
import etomica.atom.AtomTypeSphere;
import etomica.atom.Molecule;
import etomica.chem.elements.ElementSimple;
import etomica.chem.elements.Nitrogen;
import etomica.space.ISpace;
import etomica.species.Species;

/**
 * 
 * 
 * Species nitrogen molecule
 * 	with 4-points charges
 * 
 * @author Tai Boon Tan
 *
 */
public class SpeciesN2 extends Species {

    public SpeciesN2(ISpace space) {
        this(space, false);
    }
    
    public SpeciesN2(ISpace space, boolean isDynamic) {
        super();
        this.space = space;
        this.isDynamic = isDynamic;
        
        nType = new AtomTypeSphere(Nitrogen.INSTANCE, 3.1);
        pType = new AtomTypeSphere(new ElementSimple("P", 1.0), 0.0);
        addChildType(nType);
        addChildType(pType);

        setConformation(new ConformationNitrogen(space)); 
     }

     public IMolecule makeMolecule() {
         Molecule nitrogen = new Molecule(this, 6);
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, nType) : new Atom(space, nType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, nType) : new Atom(space, nType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, pType) : new Atom(space, pType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, pType) : new Atom(space, pType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, pType) : new Atom(space, pType));
         nitrogen.addChildAtom(isDynamic ? new AtomLeafDynamic(space, pType) : new Atom(space, pType));
         
         conformation.initializePositions(nitrogen.getChildList());
         return nitrogen;
     }

     public IAtomTypeSphere getNitrogenType() {
         return nType;
     }

     public AtomTypeSphere getPType() {
         return pType;
     }


     public int getNumLeafAtoms() {
         return 6;
     }
    
    public final static int indexN1 = 0;
    public final static int indexN2 = 1;
    public final static int indexP1left  = 2;
    public final static int indexP2left  = 3;
    public final static int indexP1right  = 4;
    public final static int indexP2right  = 5;
    
    
    private static final long serialVersionUID = 1L;
    protected final ISpace space;
    protected final boolean isDynamic;
    protected final AtomTypeSphere nType, pType;
}