/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.models.water;

import etomica.action.MoleculeActionTranslateTo;
import etomica.box.Box;
import etomica.molecule.IMolecule;
import etomica.molecule.IMoleculeList;
import etomica.molecule.MoleculePair;
import etomica.space3d.Space3D;
import etomica.space3d.Vector3D;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by alex on 4/28/17.
 */
public class P2WaterTIP4PTest {

    private IMoleculeList molecules;
    private static final double EPSILON = 4e-7;
    @Before
    public void setUp() throws Exception {
        IMolecule mol1 = new SpeciesWater4P(Space3D.getInstance()).makeMolecule();
        IMolecule mol2 = new SpeciesWater4P(Space3D.getInstance()).makeMolecule();

        MoleculeActionTranslateTo act = new MoleculeActionTranslateTo(Space3D.getInstance());
        act.setDestination(new Vector3D(4, 4, 4));
        act.actionPerformed(mol2);

        molecules = new MoleculePair(mol1, mol2);


    }

    @Test
    public void testEnergy() throws Exception {
        P2WaterTIP4P potential = new P2WaterTIP4P(Space3D.getInstance());
        potential.setBox(new Box(Space3D.getInstance()));
        assertEquals(-9.757003126632299, potential.energy(molecules), EPSILON);
    }

}
