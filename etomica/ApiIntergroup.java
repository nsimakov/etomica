/*
 * History
 * Created on Aug 30, 2004 by kofke
 */
package etomica;

/**
 * @author kofke
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public final class ApiIntergroup extends AtomsetIteratorAdapter implements
		AtomsetIteratorBasisDependent {

	public ApiIntergroup() {
		super(new ApiInnerFixed(
				new AtomIteratorBasis(),
				new AtomIteratorSequencerList()));
		pairIterator = (ApiInnerFixed)iterator;
		aiOuter = (AtomIteratorBasis)pairIterator.getOuterIterator();
		aiInner = (AtomIteratorListSimple)pairIterator.getInnerIterator();
	}

	/* (non-Javadoc)
	 * @see etomica.AtomsetIteratorBasisDependent#setDirective(etomica.IteratorDirective)
	 */
	public void setTarget(Atom[] targetAtoms) {
		aiOuter.setTarget(targetAtoms);
	}

	/* (non-Javadoc)
	 * @see etomica.AtomsetIteratorBasisDependent#setBasis(etomica.Atom[])
	 */
	public void setBasis(Atom[] atoms) {
		atom[0] = atoms[0];
		aiOuter.setBasis(atom);
		aiInner.setList(((AtomTreeNodeGroup)atoms[1].node).childList);
	}

	/* (non-Javadoc)
	 * @see etomica.AtomsetIteratorBasisDependent#basisSize()
	 */
	public int basisSize() {
		return 2;
	}

	private final ApiInnerFixed pairIterator;
	private final AtomIteratorBasis aiOuter;
	private final AtomIteratorListSimple aiInner;
	private final Atom[] atom = new Atom[1];

}
