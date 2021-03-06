/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.metastable;

import etomica.data.*;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataFunction;
import etomica.units.dimensions.Null;

public class DataProcessorXY extends DataProcessor implements DataSourceIndependent {

    protected final DataDump dumpX;
    protected DataFunction data;
    
    
    public DataProcessorXY(DataDump dumpX) {
        this.dumpX = dumpX;
    }

    protected IData processData(IData inputData) {
        double[] y = data.getData();
        for (int i=0; i<inputData.getLength(); i++) {
            y[i] = inputData.getValue(i);
        }
        return data;
    }

    protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
        dataInfo = new DataFunction.DataInfoFunction("XY", Null.DIMENSION, this);
        data = new DataFunction(new int[]{dumpX.getDataInfo().getLength()});
        return dataInfo;
    }

    public DataDoubleArray getIndependentData(int i) {
        return (DataDoubleArray)dumpX.getData();
    }

    public DataInfoDoubleArray getIndependentDataInfo(int i) {
        return (DataInfoDoubleArray)dumpX.getDataInfo();
    }

    public int getIndependentArrayDimension() {
        return 1;
    }

    public DataTag getIndependentTag() {
        return dumpX.getTag();
    }

}
