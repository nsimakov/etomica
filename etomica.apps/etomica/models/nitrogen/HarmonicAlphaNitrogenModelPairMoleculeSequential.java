package etomica.models.nitrogen;

import java.io.FileWriter;
import java.io.IOException;

import etomica.api.IMolecule;
import etomica.api.ISpecies;
import etomica.atom.MoleculePair;
import etomica.box.Box;
import etomica.data.types.DataTensor;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicFcc;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.normalmode.BasisBigCell;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.ISpace;
import etomica.space3d.Space3D;

/**
 * This class is created to take care of Java out of memory problem when creating
 *  a 2D array that is gigantic [~10000][~10000]
 *  
 * The class is write a 5 x (nA * dof) Matrix to file while looping through the molecules
 * This class also applied the molecules pair identification algorithm to speed up the
 *  matrix construction. 
 *  
 * @author Tai Boon Tan
 *
 */
public class HarmonicAlphaNitrogenModelPairMoleculeSequential extends Simulation{

	
	public HarmonicAlphaNitrogenModelPairMoleculeSequential(ISpace space, int numMolecule, double density) {
		super(space);
		this.space = space;
		
		int nCell = (int) Math.round(Math.pow((numMolecule/4), 1.0/3.0));
		double unitCellLength = Math.pow(numMolecule/density, 1.0/3.0)/nCell;//5.661;
		System.out.println("a: " + unitCellLength);
		System.out.println("nCell: " + nCell);
		
		potentialMaster = new PotentialMaster();
				
		Basis basisFCC = new BasisCubicFcc();
		Basis basis = new BasisBigCell(space, basisFCC, new int[]{nCell, nCell, nCell});
		
		ConformationNitrogen conformation = new ConformationNitrogen(space);
		SpeciesN2 species = new SpeciesN2(space);
		species.setConformation(conformation);
		addSpecies(species);
		
		box = new Box(space);
		addBox(box);
		box.setNMolecules(species, numMolecule);		
		
		int [] nCells = new int[]{1,1,1};
		Boundary boundary = new BoundaryRectangularPeriodic(space,nCell*unitCellLength);
		Primitive primitive = new PrimitiveCubic(space, nCell*unitCellLength);
	
		coordinateDef = new CoordinateDefinitionNitrogen(this, box, primitive, basis, space);
		coordinateDef.setIsAlpha();
		coordinateDef.setOrientationVectorAlpha(space);
		coordinateDef.initializeCoordinates(nCells);
		
		box.setBoundary(boundary);
		double rCScale = 0.475;
		double rC =box.getBoundary().getBoxSize().getX(0)*rCScale;
		System.out.println("Truncation Radius (" + rCScale +" Box Length): " + rC);
		
		potential = new P2Nitrogen(space, rC);
		potential.setBox(box);

		potentialMaster.addPotential(potential, new ISpecies[]{species, species});
		
		int nSites = 2*nCell+1;
		pairMatrix = new double[nSites][nSites][nSites][4][4][5][5];
		
		cm2ndD = new CalcNumerical2ndDerivativeNitrogen(box, potential, coordinateDef);
		findPair = new FindPairMoleculeIndex(space, coordinateDef);
	}
	
	public double[][] get2ndDerivative(int molec0){
	
		DataTensor transTensor = new DataTensor(space);
		MoleculePair pair = new MoleculePair();
	
		int numMolecule = coordinateDef.getBox().getMoleculeList().getMoleculeCount();
		int dofPerMol = coordinateDef.getCoordinateDim()/numMolecule;
		double[][] array = new double[dofPerMol][coordinateDef.getCoordinateDim()];
			
		/*
		 *	Constructing the upper diagonal of the matrix
		 *	(Skipping the molec1 == molec2) 
		 */
		IMolecule molecule0 = coordinateDef.getBox().getMoleculeList().getMolecule(molec0);
		pair.atom0 = molecule0;
			
		boolean isReverseOrder = false;
		for(int molec1=0; molec1<numMolecule; molec1++){
			if(molec0 == molec1) continue;
			/*
			 * working within the 5x5 Matrix
			 */
			// Analytical calculation for 3x3 Translational second Derivative
			
			if(molec0 > molec1){
				isReverseOrder = true;
			} else {
				isReverseOrder = false;
			}
			
			pair.atom1 = coordinateDef.getBox().getMoleculeList().getMolecule(molec1);
		
			int[] index = findPair.getPairMoleculesIndex(pair.atom0, pair.atom1, isReverseOrder);
			boolean isNewPair = findPair.getIsNewPair(index);
				
			if(isNewPair){
				transTensor.E(potential.secondDerivative(pair));
				for(int i=0; i<3; i++){
					for(int j=0; j<3; j++){
						array[i][molec1*dofPerMol + j] = transTensor.x.component(i, j);
						pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j] = array[i][molec1*dofPerMol + j];
					}
				}
				// Numerical calculation for the Cross (trans and rotation) and rotation second Derivative
				for(int i=0; i<dofPerMol; i++){
					for(int j=0; j<dofPerMol; j++){
						if(i<3 && j<3) continue;
						array[i][molec1*dofPerMol + j] = cm2ndD.d2phi_du2(new int[]{molec0,molec1}, new int[]{i,j});
						pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j] = array[i][molec1*dofPerMol + j];
					}
				}
					
				findPair.updateNewMoleculePair(index);
					
			} else {
				
				if(isReverseOrder){
					for(int i=0; i<3; i++){
						for(int j=0; j<3; j++){
							array[i][molec1*dofPerMol + j] 
							                            = pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][j][i];
						}
					}
					// Numerical calculation for the Cross (trans and rotation) and rotation second Derivative
					for(int i=0; i<dofPerMol; i++){
						for(int j=0; j<dofPerMol; j++){
							if(i<3 && j<3) continue;
							array[i][molec1*dofPerMol + j] = 
								pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][j][i];
						}
					}
				} else {
					for(int i=0; i<3; i++){
						for(int j=0; j<3; j++){
							array[i][molec1*dofPerMol + j] 
							                            = pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j];
						}
					}
					// Numerical calculation for the Cross (trans and rotation) and rotation second Derivative
					for(int i=0; i<dofPerMol; i++){
						for(int j=0; j<dofPerMol; j++){
							if(i<3 && j<3) continue;
							array[i][molec1*dofPerMol + j] = 
								pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j];
						}
					}
				}
			}
		}

      	/*
      	 *  SELF-TERM
      	 *  
      	 *  The diagonal-element block are all the same
      	 *  The 3x3 translation block is found by summing all the interactions with molecules in the system
      	 *   however, the cross and rotation block have be determined numerically 
      	 */

		int[] index = findPair.getPairMoleculesIndex(molecule0, molecule0, false);
		boolean isNewPair = findPair.getIsNewPair(index);
			
		if(isNewPair){
			for(int molec1=0; molec1<numMolecule; molec1++){
    			if(molec0==molec1) continue; // we might double sum the elements in array[a][a] if we don't skip the pair
	    			
    			for(int i=0; i<3; i++){
    				for (int j=0; j<3; j++){
    					array[i][molec0*dofPerMol + j] -= array[i][molec1*dofPerMol + j] ;         				
	    				
    				}
    			}
    		}
				
			for(int i=0; i<3; i++){
   				for (int j=0; j<3; j++){
   					pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j] = array[i][molec0*dofPerMol + j];
   				}
			}
    			
		} else {
				
			for(int i=0; i<3; i++){
   				for (int j=0; j<3; j++){
   					array[i][molec0*dofPerMol + j]= pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j];
   				}
			}
			
		}
			
		// Numerical calculation for the Cross (trans and rotation) and rotation second Derivative
			
		if(isNewPair){
			for(int i=0; i<dofPerMol; i++){
				for(int j=0; j<dofPerMol; j++){
					if(i<3 && j<3) continue;
					array[i][molec0*dofPerMol + j] = cm2ndD.d2phi_du2(new int[]{molec0,molec0}, new int[]{i,j});
					pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j] = array[i][molec0*dofPerMol + j];
				}    		
	    	}
			findPair.updateNewMoleculePair(index);
				
		} else {
			for(int i=0; i<dofPerMol; i++){
				for(int j=0; j<dofPerMol; j++){
					if(i<3 && j<3) continue;
					array[i][molec0*dofPerMol + j] =  pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j];
				}    		
	   		}
			
		}
		
		return array;
		
	}
	
	public void constructHessianMatrix(String fname, int nCell){
		
		int numMolecules = nCell*nCell*nCell*4;
		int interval = nCell*4;
		double[][][] array = new double[interval][5][coordinateDef.getCoordinateDim()];
		
		try {
			FileWriter fileWriter = new FileWriter(fname);
			
			for (int iMol=0; iMol<numMolecules; iMol+=interval){
				for(int i=0; i<interval; i++){
					array[i] = get2ndDerivative(iMol+i);
				}
				for(int i=0; i<interval; i++){
					for (int iRow=0; iRow< array[0].length; iRow++){
						for (int jCol=0; jCol< array[0][0].length; jCol++){
							double value = array[i][iRow][jCol];
//							if(Math.abs(value) < 1e-6){
//								value = 0.0;
//							}
							fileWriter.write(value+ " ");
						}
						fileWriter.write("\n");
					}
				}
			}
			
			fileWriter.close();
			
		} catch (IOException e) {
			
		}
	
	}
	
	
	public static void main (String[] args){
		
		int nC=2;
		if(args.length > 0){
			nC = Integer.parseInt(args[0]);
		}
		
		int numMolecule =nC*nC*nC*4;
		double density = 0.025;
		System.out.println("numMolecules: " + numMolecule + " with density: " + density);
		HarmonicAlphaNitrogenModelPairMoleculeSequential test = new HarmonicAlphaNitrogenModelPairMoleculeSequential(Space3D.getInstance(3), numMolecule, density);

		long startTime = System.currentTimeMillis();
	
		String fname = new String ("alpha"+numMolecule+"_2ndDer_d"+density+"_new");
		test.constructHessianMatrix(fname, nC);
		
		long endTime = System.currentTimeMillis();
		System.out.println("Time taken (s): " + (endTime-startTime)/1000);
	
	}
	
	
	protected Box box;
	protected ISpace space;
	protected P2Nitrogen potential;
	protected CoordinateDefinitionNitrogen coordinateDef;
	protected PotentialMaster potentialMaster;
	protected double[][][][][][][] pairMatrix;
	protected CalcNumerical2ndDerivativeNitrogen cm2ndD;
	protected FindPairMoleculeIndex findPair;
	private static final long serialVersionUID = 1L;
}
