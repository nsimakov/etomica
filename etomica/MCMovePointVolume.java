package etomica;

import java.util.Random;
import etomica.units.Dimension;
import java.awt.*;

/** 
 * Monte Carlo move that changes the system volume by distorting the space, causing
 * more volume to be placed at one point selected at random.  Space is distorted from
 * this point until reaching the boundaries, where it becomes a uniform translation.
 *
 * @author Nandou Lu
 * @author David Kofke
 */

public class MCMovePointVolume extends MCMove {
    
    private final Random rand = new Random();
    protected double pressure;
    protected PhaseAction.Inflate inflate;

    public MCMovePointVolume() {
        super();
        setStepSizeMax(1.0);
        setStepSizeMin(0.0);
        setStepSize(0.10);
        setPressure(Default.PRESSURE);
//        lattice = new MyLattice(6,new VelocityField(2,20), 0.015);
    }
        
    public void thisTrial() {
        double hOld, hNew, vOld, vNew;
        vOld = phase.volume();
        hOld = phase.energy.potential() + pressure*vOld;
        
        if(Math.random() < 0.5) { //transform square to distorted
        }
        else { //transform distorted back to square
        }
        
        double vScale = (2.*Math.random()-1.)*stepSize;
        vNew = vOld * Math.exp(vScale); //Step in ln(V)
        double rScale = Math.exp(vScale/(double)phase.parentSimulation().space().D());
        inflate.actionPerformed(phase,rScale);
        hNew = phase.energy.potential() + pressure*vNew;
        if(hNew >= Double.MAX_VALUE ||
             Math.exp(-(hNew-hOld)/parentIntegrator.temperature+(phase.moleculeCount+1)*vScale)
                < Math.random()) 
            {  //reject
              inflate.retractAction();
            }
        nAccept++;   //accept
    }
    
    public void setPressure(double p) {pressure = p;}
    public final double getPressure() {return pressure;}
    public final double pressure() {return pressure;}
    public Dimension getPressureDimension() {return Dimension.PRESSURE;}
    public final void setLogPressure(int lp) {setPressure(Math.pow(10.,(double)lp));}
        
    public static void main(String args[]) {
        
        java.awt.Frame f = new java.awt.Frame();   //create a window
        f.setSize(600,350);
        
        Default.ATOM_SIZE = 1.0;
        
        Simulation sim = new Simulation(new Space2D());
        Simulation.instance = sim;
        Species species = new SpeciesDisks(sim);
        Potential2 potential = new P2SimpleWrapper(sim,new PotentialHardDisk(sim));
        IntegratorMC integrator = new IntegratorMC(sim);
        MCMove mcMoveAtom = new MCMoveAtom();
        MCMovePointVolume mcMovePointVolume = new MCMovePointVolume();
        Controller controller = new Controller(sim);
        final Phase phase = new Phase(sim);
        final DisplayPhase displayPhase = new DisplayPhase(sim);
        displayPhase.setScale(0.7);
        DeviceSlider slider = new DeviceSlider(mcMovePointVolume, "pressure");
        slider.setMinimum(0);
        slider.setMaximum(100);
        
        mcMovePointVolume.setPhase(phase);
        integrator.add(mcMoveAtom);
        integrator.add(mcMovePointVolume);

        species.setNMolecules(0);
                
		Simulation.instance.elementCoordinator.go();
		
//		phase.boundary().dimensions().TE(0,lattice.scale);
		
		
/*		ColorSchemeBySpecies colorScheme = new ColorSchemeBySpecies();
		colorScheme.addSpecies(species, new ColorScheme.Simple(java.awt.Color.red));
		colorScheme.addSpecies(deformed, new ColorScheme.Simple(java.awt.Color.black));
		displayPhase.setColorScheme(colorScheme);
		
		Atom atom = phase.firstAtom();
		for(int i=0; i<lattice.size; i++) {
		    for(int j=0; j<lattice.size; j++) {
		        atom.coordinate.position().E(lattice.sites[i][j].originalPosition);
		        atom.coordinate.position().TE(phase.boundary().dimensions());
		        atom = atom.nextAtom();
		    }
		}
		for(int i=0; i<lattice.size; i++) {
		    for(int j=0; j<lattice.size; j++) {
		        atom.coordinate.position().E(lattice.sites[i][j].deformedPosition);
		        atom.coordinate.position().TE(phase.boundary().dimensions());
		        atom = atom.nextAtom();
		    }
		}
*/		        
        f.add(Simulation.instance.panel()); //access the static instance of the simulation to
                                            //display the graphical components
        f.pack();
        f.show();
        f.addWindowListener(new java.awt.event.WindowAdapter() {   //anonymous class to handle window closing
            public void windowClosing(java.awt.event.WindowEvent e) {System.exit(0);}
        });
    }
    
    /**
     * Array of 2D triangular cells formed by bisecting the cells of a square lattice.
     */
 /*   private class MyLattice implements AbstractLattice {
        
        SquareLattice squareLattice;
        SiteIterator.List iterator = new SiteIterator.List();
        MySite[][] sites;
        int size, size1;
        double scale;
        /**
         * Size is the number of sites in each dimension; considered non-periodically
         */
  /*      MyLattice(int size, final VelocityField vField, double deltaT) {
            this.size = size;
            size1 = size-1;
            squareLattice = new SquareLattice(size, new MySiteFactory(), 1.0/(double)(size-1));
            sites = new MySite[size][size];
            int[] index = new int[2];
            for(index[0]=0; index[0]<size; index[0]++) { //make array of sites for convenience of access
                for(index[1]=0; index[1]<size; index[1]++) {
                    sites[index[0]][index[1]] = (MySite)squareLattice.site(index);
                }
            }
            //assign site positions and make vectors for deformed positions
            for(int i=0; i<size; i++) {    
                for(int j=0; j<size; j++) {
                    MySite site = sites[i][j];
                    iterator.addSite(site);
                    site.originalPosition = (Space2D.Vector)((BravaisLattice.Coordinate)site.coordinate()).position();
         //           site.originalPosition.PE(-0.5);
                    site.deformedPosition = new Space2D.Vector();
                    site.deformedPosition.E(site.originalPosition);
                } 
            }
            //Deform lattice
            //make a local rhs class from the velocity field suitable for input to the ode solver
            OdeSolver.Rhs rhs = new OdeSolver.Rhs() {
                public double[] dydx(OdeSolver.Variables xy) {
                    return vField.v(xy.y);
                }
            };
            //do integration
            for(int i=0; i<size; i++) {
                for(int j=0; j<size; j++) {
                    MySite site = sites[i][j];
                    OdeSolver.Variables xy0 = new OdeSolver.Variables(0.0, site.originalPosition.toArray());
                    OdeSolver.Variables[] xy = OdeSolver.rungeKuttaAdaptive(xy0,deltaT,1.e-7,rhs);
                    site.deformedPosition.E(xy[xy.length-1].y);
                } 
            }
            //reshape to rectangle
            double avg = 0.0;
            for(int i=0; i<size; i++) {
                double x0 = sites[0][i].deformedPosition.component(0);
                for(int j=0; j<size; j++) {
                    sites[j][i].deformedPosition.PE(0,-x0);
                }
                avg += sites[size-1][i].deformedPosition.component(0);
            }
            avg /= size;
            for(int i=0; i<size; i++) {
                double factorX = avg/sites[size-1][i].deformedPosition.component(0);
                for(int j=0; j<size; j++) {
                    Space.Vector position = sites[j][i].deformedPosition;
                    position.setComponent(0,factorX*position.component(0));//scale each row to uniform width
                }
            }
            for(int i=0; i<size; i++) {
                double y0 = sites[i][0].deformedPosition.component(1);
                double factorY = 1.0/(sites[i][size-1].deformedPosition.component(1)-y0);
                for(int j=0; j<size; j++) {
                    Space.Vector position = sites[i][j].deformedPosition;
                    position.PE(1,-y0);
                    position.setComponent(1,factorY*position.component(1));
                }
            }
         //   scale = sites[size-1][0].deformedPosition.component(0) - sites[0][0].deformedPosition.component(0);
            scale = avg;
            
            //make cells
            for(int i=0; i<size1; i++) {    //loops don't do last row/column of lattice
                for(int j=0; j<size1; j++) {
                    MySite site = sites[i][j];
                    site.c1 = new MyCell(site, sites[i][j+1], sites[i+1][j+1]);//bottom left triangle
                    site.c2 = new MyCell(site, sites[i+1][j], sites[i+1][j+1]);//top right triangle
                } 
            }
            //set up cell neighbors
            for(int i=0; i<size1; i++) {    //loops don't do last row/column of lattice
                for(int j=0; j<size1; j++) {
                    MySite site = sites[i][j];
                    site.c1.nbr[0] = sites[i][j+1].c2;
                    site.c1.nbr[1] = site.c2;
                    if(i > 0) site.c1.nbr[2] = sites[i-1][j].c2;
                    site.c2.nbr[0] = sites[i+1][j].c1;
                    site.c2.nbr[1] = site.c1;
                    if(j > 0) site.c2.nbr[2] = sites[i][j-1].c1;
                } 
            }
            
        }//end of MyLattice constructor
        
        //returns the deformed-lattice cell that contains the point r
        public MyCell getDeformedCell(Space.Vector r) {
            //initial guess it cell from original lattice
            MyCell cell = getOriginalCell((Space2D.Vector)r);
            //if point is not in cell, move to the cell lying on other side of an edge
            //that separates point from its interior.  Keep doing this until the containing
            //cell is located
            MyCell newCell = cell.outside((Space2D.Vector)r);
            while(newCell != null) {
                cell = newCell;
                newCell = cell.outside((Space2D.Vector)r);//returns null if cell contains r
            }
            return cell;
        }
        
        //returns the original-lattice cell that contains the point r
        public MyCell getOriginalCell(Space2D.Vector r) {
            //find and return cell containing r in undeformed lattice
            int ix = (int)Math.floor(r.x * size1);
            int iy = (int)Math.floor(r.y * size1);
            ix = (ix >= size1) ? size1-1 : ix;
            MySite site = sites[ix][iy];
            double dx = r.x - site.originalPosition.x;
            double dy = r.y - site.originalPosition.y;
            return (dy > dx) ? site.c1 : site.c2;
        }
        
        //AbstractLattice methods
        public int D() {return 2;}
        public int siteCount() {return 2*squareLattice.siteCount();}
        public Site site(AbstractLattice.Coordinate coord) {return null;} //not used
        public Site randomSite() {return iterator.randomSite();}
        public SiteIterator iterator() {iterator.reset(); return iterator;}     //iterator for all sites in lattice
    }
    
    private class MySite extends Site {
        MyCell c1, c2;
        Space2D.Vector originalPosition, deformedPosition;
        MySite(AbstractLattice parent, SiteIterator.Neighbor iterator, AbstractLattice.Coordinate coord) {
            super(parent, iterator, coord);
        }
    }
    private class MySiteFactory implements SiteFactory {
       public Site makeSite(AbstractLattice parent, 
                            SiteIterator.Neighbor iterator, 
                            AbstractLattice.Coordinate coord) {
           return new MySite(parent, iterator, coord);
       }
    }
    private class MyCell {
        public MySite origin;
        public double jacobian;
        public double a11, a12, a21, a22;
        public double deltaX, deltaY;
        double m21, m10, m20, q0, q1, q2;
        public MySite[] vertex = new MySite[3];
        public MyCell[] nbr = new MyCell[3];
        MyCell(MySite s0, MySite s1, MySite s2) {
            origin = s0;
            vertex[0] = s0;
            vertex[1] = s1;
            vertex[2] = s2;
            deltaX = origin.deformedPosition.x - origin.originalPosition.x;
            deltaY = origin.deformedPosition.y - origin.deformedPosition.y;
            double xbn = s1.deformedPosition.x - s0.deformedPosition.x;
            double xan = s2.deformedPosition.x - s0.deformedPosition.x;
            double ybn = s1.deformedPosition.y - s0.deformedPosition.y;
            double yan = s2.deformedPosition.y - s0.deformedPosition.y;
            double xb  = s1.originalPosition.x - s0.originalPosition.x;
            double xa  = s2.originalPosition.x - s0.originalPosition.x;
            double yb  = s1.originalPosition.y - s0.originalPosition.y;
            double ya  = s2.originalPosition.y - s0.originalPosition.y;
            double denominator = ya*xb - yb*xa;
            a11 = (xbn*ya - xan*yb)/denominator;
            a12 = (xan*xb - xa*xbn)/denominator;
            a21 = (ya*ybn - yan*yb)/denominator;
            a22 = (yan*xb - xa*ybn)/denominator;
            jacobian = a11*a22 - a12*a21;
            
            //set up constants used to determined whether a point is in the cell
            m10 = ybn/xbn;
            m20 = yan/xan;
            m21 = (yan - ybn)/(xan - xbn);
            if(m21 == 0.0) q0 = ybn;
            else if(Double.isInfinite(m21)) q0 = xbn;
            else q0 = (ybn - m21*xbn);// 1 - 0
            q0 *= q0;
            
            if(m20 == 0.0) q1 = -ybn;
            else if(Double.isInfinite(m20)) q1 = -xbn;
            else q1 = (yan-ybn - m20*(xan-xbn));// 2 - 1
            q1 *= q1;
            
            if(m10 == 0.0) q2 = -yan;
            else if(Double.isInfinite(m10)) q2 = -xan;
            else q2 = (yan - m10*xan); // 0 - 2
            q2 *= q2;
        }
        
        //returns null if point is inside this cell, otherwise returns neighbor cell
        //on other side of an edge that the point lies beyond
        //looks at each vertex and sees if point is closer than the opposite edge is to it
        public MyCell outside(Space2D.Vector r) {
            if(outsideEdge(r, vertex[0].deformedPosition, q0, m21)) return nbr[0];
            if(outsideEdge(r, vertex[1].deformedPosition, q1, m20)) return nbr[1];
            if(outsideEdge(r, vertex[2].deformedPosition, q2, m10)) return nbr[2];
            return null; //point is inside the cell
        }
        
        private boolean outsideEdge(Space2D.Vector r, Space2D.Vector r0, double q, double m) {
            double dx = r.x - r0.x;
            double dy = r.y - r0.y;
            if(m == 0.0) return q < dy*dy; //opposite edge is horizontal
            if(Double.isInfinite(m)) return q < dx*dx; //opposite edge is vertical
            //opposite edge is neither horziontal nor vertical
            double mr0 = dy/dx;
            double delta = m - mr0;
            double L2 = q * (mr0*mr0 + 1)/(delta*delta); //distance from vertex to opposite edge along a line through r
            return L2 < (dx*dx + dy*dy);
        }
        
        public void transform(Space2D.Vector r, double scale) {
            double dx = r.x - origin.originalPosition.x;
            double dy = r.y - origin.originalPosition.y;
            double dxn = a11*dx + a12*dy;
            double dyn = a21*dx + a22*dy;
            r.x = origin.originalPosition.x + scale*(deltaX + dxn);
            r.y = origin.originalPosition.y + scale*(deltaY + dyn);
        }
        
        //returns a polygon object, suitable for drawing, corresponding to the original cell
        public java.awt.Polygon getOriginalPolygon(int[] origin, double toPixelsX, double toPixelsY) {
            java.awt.Polygon triangle = new Polygon(
                new int[] {(int)(vertex[0].originalPosition.x*toPixelsX),
                           (int)(vertex[1].originalPosition.x*toPixelsX),
                           (int)(vertex[2].originalPosition.x*toPixelsX)},
                new int[] {(int)(vertex[0].originalPosition.y*toPixelsY),
                           (int)(vertex[1].originalPosition.y*toPixelsY),
                           (int)(vertex[2].originalPosition.y*toPixelsY)}, 3);
            triangle.translate(origin[0],origin[1]); 
            return triangle;
        }//end of getOriginalPolygon
        //returns a polygon object, suitable for drawing, corresponding to the deformed cell
        public java.awt.Polygon getDeformedPolygon(int[] origin, double toPixelsX, double toPixelsY) {
            java.awt.Polygon triangle = new Polygon(
                new int[] {(int)(vertex[0].deformedPosition.x*toPixelsX),
                           (int)(vertex[1].deformedPosition.x*toPixelsX),
                           (int)(vertex[2].deformedPosition.x*toPixelsX)},
                new int[] {(int)(vertex[0].deformedPosition.y*toPixelsY),
                           (int)(vertex[1].deformedPosition.y*toPixelsY),
                           (int)(vertex[2].deformedPosition.y*toPixelsY)}, 3);
            triangle.translate(origin[0],origin[1]); 
            return triangle;
        }//end of getDeformedPolygon
        
    }*/
}