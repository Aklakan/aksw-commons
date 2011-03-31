package org.aksw.commons.experiments.impl;


import java.text.DecimalFormat;

/**
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */
public class DefaultFormatter {

    boolean replaceCommaByPoints = true;

   
    public enum Display {
        AVG, HITS, TOTAL
    }

    Display display = Display.AVG;

   

//	DecimalFormat dfStdDevLatex = new DecimalFormat("##.##%");
    DecimalFormat dfLatexDefault = new DecimalFormat("####.####");
    DecimalFormat dfPercentage = new DecimalFormat("##.##%");


    

    public String getAsRows(boolean addNumbersInFront) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getAsColumn(boolean addNumbersInFront) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


}
