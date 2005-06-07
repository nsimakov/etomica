package etomica.virial;
import etomica.Simulation;
import etomica.potential.Potential2;
import etomica.virial.cluster.ReeHoover;
import etomica.virial.cluster.Standard;
/** * @author jksingh * Meter to compute the 5th virial coefficient. */
public class MeterVirialB5P extends MeterVirial {	public MeterVirialB5P(Simulation sim, double refSigma, P0Cluster simulationPotential, Potential2 targetPotential) {		super(sim, 				1.0, new etomica.virial.cluster.E5(new MayerHardSphere(refSigma)), 				D5HS(refSigma),				B5Clusters(targetPotential),				simulationPotential);	}	public static double D5HS(double sigma) {		double b0 = 2.*Math.PI/3 * sigma*sigma*sigma;		return (45.70/30)*b0 * b0 * b0 * b0;		//return 0.711*b0*b0*b0*b0;	}	public static Cluster[] B5Clusters(Potential2 potential) {		MayerFunctionSpherical f = new MayerGeneralSpherical(potential);//		int[][] ERH1 = new int[][] {{1,2},{3,4}};//		int[][] ERH2 = new int[][] {{0,1},{2,3},{4,0}};//		int[][] ERH3 = new int[][] {{0,1},{1,4},{2,3},{4,0}};//		int[][] ERH4 = new int[][] {{0,1},{1,2},{2,3},{3,4},{4,0}};//				int[][] ERH1 = new int[][] {{0,1},{0,2},{0,3},{0,4},{1,3},{1,4},{2,3},{2,4}};		int[][] ERH2 = new int[][] {{0,2},{0,3},{1,2},{1,3},{1,4},{2,4},{3,4}};		int[][] ERH3 = new int[][] {{0,2},{0,3},{1,2},{1,3},{2,4},{3,4}};		int[][] ERH4 = new int[][] {{0,2},{0,3},{1,3},{1,4},{2,4}};				Cluster c1 = new Cluster(5, 6.0/30.0, new Cluster.BondGroup(f, Standard.full(5)));		Cluster c2 = new ReeHoover(5, -45./30.0, new Cluster.BondGroup(f, ERH1));		Cluster c3 = new ReeHoover(5, 60./30.0, new Cluster.BondGroup(f, ERH2));		Cluster c4 = new ReeHoover(5, -10./30.0, new Cluster.BondGroup(f, ERH3));		Cluster c5 = new ReeHoover(5, -12./30.0, new Cluster.BondGroup(f, ERH4));				//return new Cluster[] {c2};		return new Cluster[] {c1, c2,c3,c4,c5};	}}