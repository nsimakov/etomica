package etomica.normalmode;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IBox;
import etomica.api.IVectorMutable;
import etomica.box.Box;
import etomica.graphics.SimulationGraphic;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisOrthorhombicHexagonal;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveOrthorhombicHexagonal;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryDeformablePeriodic;
import etomica.space.ISpace;
import etomica.space2d.Space2D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Pixel;

/**
 * 
 * Harmonic anaylsis for 2-D soft-sphere model
 * 
 * @author Tai Boon Tan
 */
public class NormalModeAnalysisDisplay2D extends Simulation {

    private static final long serialVersionUID = 1L;
	private static final String APP_NAME = "2-D Harmonic Oscillator";

	public NormalModeAnalysisDisplay2D(ISpace _space){
        super(_space, true);
        this.space = _space;
        
        setNCells(new int[]{getDimx(), getDimy()});
        
        species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        
        box = new Box(space);
        addBox(box);
        box.setNMolecules(species, 2*nCells[0]*nCells[1]);
        
        primitive = new PrimitiveOrthorhombicHexagonal(space, 1);
        IVectorMutable[] dimension = space.makeVectorArray(space.D());
        for (int i=0; i<space.D(); i++){
        	dimension[i].Ea1Tv1(nCells[i], primitive.vectors()[i]);
        }
        boundary = new BoundaryDeformablePeriodic(space, dimension);
        basis = new BasisOrthorhombicHexagonal();
        box.setBoundary(boundary);

        coordinateDefinition = new CoordinateDefinitionLeaf(this, box, primitive, basis, space);
        coordinateDefinition.initializeCoordinates(nCells);
        
        //String filename = "2D_CB_FCC_n12_T01_Mode01a";
        nm = new NormalModes2D(space, boundary, primitive, basis);
        nm.setTemperature(temperature);
                
        waveVectorFactory = nm.getWaveVectorFactory();
        waveVectorFactory.makeWaveVectors(box);
        
        
        //IVectorMutable[] waveVectors = waveVectorFactory.getWaveVectors();
        //double[] coefficients = waveVectorFactory.getCoefficients();
        
        //for (int i=0; i<waveVectors.length; i++) {
        //    System.out.println(coefficients[i]+" "+waveVectors[i]);
        //}
             
        
        integrator = new IntegratorHarmonic(random, 0.0005, temperature, space);
        integrator.setCoordinateDefinition(coordinateDefinition);
        integrator.setWaveVectors(waveVectorFactory.getWaveVectors());
        integrator.setWaveVectorCoefficients(waveVectorFactory.getCoefficients());
        integrator.setOmegaSquared(nm.getOmegaSquared(), waveVectorFactory.getCoefficients());
        integrator.setEigenVectors(nm.getEigenvectors());
        integrator.setTemperature(temperature);
        integrator.setOneWV(true);
        integrator.setWaveVectorNum(0);
        
        
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setSleepPeriod(0);
        
        getController().addAction(activityIntegrate);
        integrator.setBox(box);
        
	}
	

	public int getDimx() {
		return dimx;
	}

	public void setDimx(int dimx) {
		this.dimx = dimx;
	}

	public int getDimy() {
		return dimy;
	}

	public void setDimy(int dimy) {
		this.dimy = dimy;
	}

	public int[] getNCells() {
		return nCells;
	}


	public void setNCells(int[] cells) {
		nCells = cells;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
 
        ISpace sp = Space2D.getInstance();
        
        //instantiate simulation
        NormalModeAnalysisDisplay2D sim = new NormalModeAnalysisDisplay2D(sp);
    
        
        SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME, sim.space, sim.getController());
        simGraphic.getDisplayBox(sim.box).setPixelUnit(new Pixel(10));
        simGraphic.makeAndDisplayFrame(APP_NAME);
        
	}
	
	
	protected IntegratorHarmonic integrator;
	protected ActivityIntegrate activityIntegrate;
	protected IBox box;
	protected Boundary boundary;
	protected Primitive primitive;
	protected Basis basis;
	protected SpeciesSpheresMono species;
	protected NormalModes nm;
	protected CoordinateDefinitionLeaf coordinateDefinition;
	protected WaveVectorFactory waveVectorFactory;
	protected ISpace space;
	
	protected int dimx = 20;
	protected int dimy = 10;
	protected double temperature = 0.1;
	protected int[] nCells;



	

	
}
