package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.PlotLayer;

/**
 * Control subinterface for controls that can contribute PlotLayers
 * to the plot.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public interface LayerControl extends Control {

    /**
     * Returns the layers contributed by this control.
     *
     * @return  layers
     */
    PlotLayer[] getPlotLayers();

    /**
     * Returns legend entries associated with this control.
     *
     * @return   legend entries
     */
    LegendEntry[] getLegendEntries();

    /**
     * Returns a text label associated with one of the user coords for
     * this control, typically the name of the column or expression
     * supplying the data.  Null may be returned if there's no good answer.
     *
     * @param  userCoordName  name of one of the user value infos of an
     *                        input coord for this control
     * @return   text label associated with user coordinate
     */
    String getCoordLabel( String userCoordName );
}
