package etomica.nbr.list.molecule;

import etomica.api.IBox;
import etomica.api.ISimulation;
import etomica.atom.IAtomPositionDefinition;
import etomica.nbr.cell.BoxAgentSourceCellManager;
import etomica.space.ISpace;

/**
 * BoxAgentSource responsible for creating a NeighborCellManager.
 * 
 * @author Tai Boon Tan
 *
 */
public class BoxAgentSourceCellManagerListMolecular extends BoxAgentSourceCellManager {

    public BoxAgentSourceCellManagerListMolecular(ISimulation sim, IAtomPositionDefinition positionDefinition, ISpace _space) {
        super(sim, positionDefinition, _space);
    }

    public void setPotentialMaster(PotentialMasterListMolecular newPotentialMaster) {
        potentialMaster = newPotentialMaster;
    }

    public Object makeAgent(IBox box) {
        NeighborCellManagerListMolecular cellManager = new NeighborCellManagerListMolecular(sim, box,range,positionDefinition, space);
        cellManager.setPotentialMaster(potentialMaster);
        box.getBoundary().getEventManager().addListener(cellManager);
        return cellManager;
    }

    public Class getAgentClass() {
        return NeighborCellManagerListMolecular.class;
    }
    
    private static final long serialVersionUID = 1L;
    protected PotentialMasterListMolecular potentialMaster;
}