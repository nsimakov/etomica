package etomica.modules.rheology;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.data.DataSourceScalar;
import etomica.space.ISpace;
import etomica.units.Null;

/**
 * Meter to measure the viscosity of a polymer in a shear field.
 *
 * @author Andrew Schultz
 */
public class MeterViscosity extends DataSourceScalar {

    public MeterViscosity(ISpace space) {
        super("viscosity", Null.DIMENSION);
        dr = space.makeVector();
    }

    public void setIntegrator(IntegratorPolymer newIntegrator) {
        integrator = newIntegrator;
    }

    public void setBox(IBox newBox) {
        box = newBox;
    }
    
    public double getDataAsScalar() {
        double shearRate = integrator.getShearRate();
        if (shearRate == 0) {
            return Double.NaN;
        }
        IAtomList list = box.getMoleculeList().getMolecule(0).getChildList();
        double v = 0;
        for (int i=0; i<list.getAtomCount()-1; i++) {
            IVector p0 = list.getAtom(i).getPosition();
            IVector p1 = list.getAtom(i+1).getPosition();
            dr.Ev1Mv2(p1, p0);
            v += dr.getX(0)*dr.getX(1);
        }
        return v/shearRate;
    }
    
    private static final long serialVersionUID = 1L;
    protected IBox box;
    protected IVectorMutable dr;
    protected IntegratorPolymer integrator;
    protected int count;
}
