/*
 * History
 * Created on Nov 21, 2004 by kofke
 */
package etomica.nbratom.cell;

import etomica.Atom;
import etomica.Integrator;
import etomica.Phase;
import etomica.PhaseEvent;
import etomica.PhaseListener;
import etomica.SimulationEvent;
import etomica.Space;
import etomica.atom.AtomPositionDefinition;
import etomica.atom.AtomPositionDefinitionSimple;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.atom.iterator.AtomIteratorPhaseDependent;
import etomica.lattice.CellLattice;

/**
 * Class that defines and manages construction and use of lattice of cells 
 * for cell-based neighbor listing.
 */

//TODO modify assignCellAll to loop through cells to get all atoms to be assigned
//no need for index when assigning cell

public class NeighborCellManager implements Integrator.IntervalListener {

    private final CellLattice lattice;
    private final Space space;
    private final Phase phase;
    private int listCount;
    private final AtomIteratorPhaseDependent atomIterator;
    private int iieCount;
    private int updateInterval;
    private int priority;
    private final AtomPositionDefinition positionDefinition;
    
    /**
     * Constructs manager for neighbor cells in the given phase.  The number of
     * cells in each dimension is given by nCells. 
     */
    public NeighborCellManager(Phase phase, int nCells) {
        this(phase,nCells,new AtomPositionDefinitionSimple());
    }
    
    public NeighborCellManager(Phase phase, int nCells, AtomPositionDefinition positionDefinition) {
        this.phase = phase;
        this.positionDefinition = positionDefinition;
        space = phase.space();
        atomIterator = new AtomIteratorAllMolecules(phase);
        setPriority(150);
        setUpdateInterval(1);

        lattice = new CellLattice(phase.boundary().dimensions(), NeighborCell.FACTORY);
        phase.setLattice(lattice);
        int[] size = new int[space.D()];
        for(int i=0; i<space.D(); i++) size[i] = nCells;
        lattice.setSize(size);

        //listener to phase to detect addition of new SpeciesAgent
        //or new atom
        phase.speciesMaster.addListener(new PhaseListener() {
            public void actionPerformed(SimulationEvent evt) {
                actionPerformed((PhaseEvent)evt);
            }
           public void actionPerformed(PhaseEvent evt) {
                if(evt.type() == PhaseEvent.ATOM_ADDED) {
                    Atom atom = evt.atom();
                    //new species agent requires another list in each cell
                    if(atom.type.getNbrManagerAgent().getPotentials().length > 0) {
                        assignCell(atom);
                    }
                }
            }
        });
    }

    /**
     * @return the lattice of cells.
     */
    public CellLattice getCellLattice() {
        return lattice;
    }
    
    /**
     * Assigns cells to all molecules in the phase.
     */
    public void assignCellAll() {
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            assignCell(atomIterator.nextAtom());
        }
    }
    
    /**
     * Assigns the cell for the given atom.
     * @param atom
     */
    public void assignCell(Atom atom) {
        AtomSequencerCell seq = (AtomSequencerCell)atom.seq;
        NeighborCell newCell = (NeighborCell)lattice.site(positionDefinition.position(atom));
        if(newCell != seq.cell) {assignCell(seq, newCell, atom.type.getSpeciesIndex());}
    }
    
    /**
     * Assigns atom sequencer to given cell in the list of the given index.
     */
    public void assignCell(AtomSequencerCell seq, NeighborCell newCell, int listIndex) {
        if(seq.cell != null) seq.cell.occupants().remove(seq.nbrLink);
        seq.cell = newCell;
//        seq.nbrLink.remove();
        if(newCell != null) {
            newCell.occupants().add(seq.nbrLink);
        }
    }//end of assignCell
    
    public void intervalAction(Integrator.IntervalEvent event) {
        if (event.type() == Integrator.IntervalEvent.INITIALIZE) {
            assignCellAll();
        }
        else if (event.type() == Integrator.IntervalEvent.INTERVAL) {
            if (--iieCount == 0) {
                assignCellAll();
                iieCount = updateInterval;
            }
        }
    }
    
    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }
    
    /**
     * @return the priority of this as an integrator interval-listenter (default is 150)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param priority the priority of this as an integrator interval-listenter (default is 150)
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
}//end of NeighborCellManager
