/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.normalmode;

import etomica.data.AccumulatorAverageBlockless;
import etomica.data.DataDistributer;
import etomica.data.DataSourceScalar;
import etomica.integrator.mcmove.MCMoveOverlapListener;
import etomica.units.dimensions.Null;
import etomica.units.dimensions.Pressure;

/**
 * Datasource that returns an estimate of mu based on thermodynamic
 * self-consistency of pressure measurements and defect free energies.
 * Solution is found using an expression for (mu-muLat) in terms of
 * (P-Plat).  P is computed via one approach, while the expression for
 * (mu-muLat) is derived using the other approach.
 * 
 * @author Andrew Schultz
 */
public class DataSourceMuRoot extends DataSourceScalar {

    protected final MCMoveOverlapListener mcMoveOverlapMeter;
    protected final DataDistributer pSplitter;
    protected double bmu;
    protected double bmuLat, volume, latticeDensity;
    protected double lastP, lastVacancyConcentration, lastDP, lastLnTot, lastDMu;
    
    public DataSourceMuRoot(MCMoveOverlapListener mcMoveOverlapMeter, double bmu, DataDistributer pSplitter, double bmuLat, double latticeDensity, double volume) {
        super("mu", Null.DIMENSION);
        this.mcMoveOverlapMeter = mcMoveOverlapMeter;
        this.pSplitter = pSplitter;
        this.bmu = bmu;
        this.latticeDensity = latticeDensity;
        this.bmuLat = bmuLat;
        this.volume = volume;
    }
    
    public synchronized double getDataAsScalar() {
        double[] ratios = mcMoveOverlapMeter.getRatios();
        if (ratios == null || ratios.length == 0 || pSplitter.getNumDataSinks() < 2) return Double.NaN;
        double myMu = bmu;
        double lastMu = bmu;
        double maxMu = Double.POSITIVE_INFINITY;
        double minMu = Double.NEGATIVE_INFINITY;
        while (true) {

            double p = 1;
            double l = Math.exp(myMu);
            double vAvg = 0;
            double totMinus1 = 0;
            for (int i=ratios.length-1; i>=0; i--) {
                if (Double.isNaN(ratios[i])) {
                    if (i==ratios.length-1) return Double.NaN;
                    break;
                }
                p /= l*ratios[i];
                totMinus1 += p;
            }
            double tot = totMinus1 + 1;
            if (totMinus1 > 0.5) {
                // probably only happens for large systems where multiple vacanacies are happy
                lastLnTot = Math.log(tot);
            }
            else {
                // ln(1+x) = x - x^2/2 + x^3/3 + ...
                lastLnTot = 0;
                double xn = totMinus1;
                for (int i=1; i<100; i++) {
                    if (Math.abs(xn/i) < lastLnTot*1e-15) break;
                    lastLnTot += xn/i;
                    xn *= -totMinus1;
                }
            }
            p = 1;
            double pressure1 = 0;
            double oneMinusP0 = 0;
            for (int i=0; i<=ratios.length; i++) {
                if (i>0) oneMinusP0 += p/tot;
                if (ratios.length-1-i >= 0) {
                    if (Double.isNaN(ratios[ratios.length-1-i])) {
                        break;
                    }
                    p /= l*ratios[ratios.length-1-i];
                }
            }
            p = 1;
            double pLat = Double.NaN;
            for (int i=0; i<pSplitter.getNumDataSinks() && i<=ratios.length; i++) {
                double pi = p/tot;
                AccumulatorAverageBlockless acc = (AccumulatorAverageBlockless)pSplitter.getDataSink(i);
                if (acc == null || acc.getSampleCount() == 0) {
                    if (i<2) return Double.NaN;
                    break;
                }
                double accValue = acc.getData().getValue(acc.AVERAGE.index);
                pressure1 += pi*accValue;
                if (i==0) {
                    pLat = accValue;
                    lastDP = -oneMinusP0*accValue;
                }
                else {
                    lastDP += pi*accValue;
                }
                vAvg += pi*i;
                if (ratios.length-1-i >= 0) {
                    if (Double.isNaN(ratios[ratios.length-1-i])) {
                        break;
                    }
                    p /= l*ratios[ratios.length-1-i];
                }
            }
            lastVacancyConcentration = vAvg / (volume*latticeDensity);

            if (Double.isNaN(pLat)) return Double.NaN;
            lastP = pressure1;
            double dmu = lastDP/latticeDensity - lastLnTot/volume;
            lastDMu = dmu;
            double newMu = bmuLat + dmu;
            if (newMu == lastMu) return newMu;
            if (newMu == maxMu || newMu == minMu) return (minMu+maxMu)/2;
            if (newMu > maxMu) {
                return maxMu;
            }
            if (newMu < minMu) {
                return minMu;
            }
            if (newMu > lastMu) {
                minMu = lastMu;
            }
            else {
                maxMu = lastMu;
            }
            lastMu = myMu;
            myMu = newMu;
        }
    }
    
    public double getLastPressure() {
        return lastP;
    }
    
    public double getLastDMu() {
        return lastDMu;
    }

    public double getLastDPressure() {
        return lastDP;
    }

    public double getLastVacancyConcentration() {
        return lastVacancyConcentration;
    }
    
    public double getLastLnTot() {
        return lastLnTot;
    }

    public class DataSourceMuRootPressure extends DataSourceScalar {
        public DataSourceMuRootPressure() {
            super("P", Pressure.DIMENSION);
        }
        
        public double getDataAsScalar() {
            DataSourceMuRoot.this.getDataAsScalar();
            return DataSourceMuRoot.this.getLastPressure();
        }
    }

    public class DataSourceMuRootVacancyConcentration extends DataSourceScalar {
        public DataSourceMuRootVacancyConcentration() {
            super("vc", Null.DIMENSION);
        }
        
        public double getDataAsScalar() {
            DataSourceMuRoot.this.getDataAsScalar();
            return DataSourceMuRoot.this.getLastVacancyConcentration();
        }
    }
}
