package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.Icon;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.layer.FunctionPlotter;
import uk.ac.starlink.ttools.plot2.layer.SpectrogramPlotter;

/**
 * Action for adding a single-plotter layer control to the plot stack.
 *
 * @author   Mark Taylor
 * @since    25 Jul 2013
 */
public abstract class LayerControlAction extends BasicAction {

    private final ControlStack stack_;

    /**
     * Constructs a LayerControlAction from name, icon and description.
     *
     * @param   name   action name
     * @param   layerIcon   icon representing the type of layer the action
     *                      will add; it may get doctored to generate the
     *                      icon for this action
     * @param   descrip  action description
     * @param   stack   plot stack
     */
    public LayerControlAction( String name, Icon layerIcon, String descrip,
                               ControlStack stack ) {
        super( name, ResourceIcon.toAddIcon( layerIcon ), descrip );
        stack_ = stack;
    }

    /**
     * Constructs a LayerControlAction from a Plotter.
     *
     * @param  plotter   plotter which will be added to the stack
     * @param  stack    plot stack
     */
    public LayerControlAction( Plotter plotter, ControlStack stack ) {
        this( "Add " + plotter.getPlotterName() + " Control",
              plotter.getPlotterIcon(),
              "Add a new " + plotter.getPlotterName().toLowerCase()
                           + " layer control to the stack",
              stack );
    }

    /**
     * Creates a layer control for this action's plotter.
     *
     * @return   new layer control
     */
    public abstract LayerControl createLayerControl();

    public void actionPerformed( ActionEvent evt ) {
        stack_.addControl( createLayerControl() );
    }

    /**
     * Attempts to return an instance of this class corresponding to
     * a given plotter.  If no suitable implementation is available,
     * null is returned.
     *
     * @param  plotter   plotter to provide an action for
     * @param  stack    stack to which controls are to be added
     * @param  nextSupplier  manages global dispensing for some style options
     * @param  tcListener  listener for TopcatEvents
     * @param  baseConfigger  configuration source for some global config
     *                        options
     * @return  new action to add plotter control to stack, or null
     */
    public static LayerControlAction
            createPlotterAction( final Plotter plotter, ControlStack stack,
                                 final NextSupplier nextSupplier,
                                 final TopcatListener tcListener,
                                 final Configger baseConfigger ) {
        final CoordGroup cgrp = plotter.getCoordGroup();

        /* This disjunction is currently a rather messy and ad hoc list of
         * tests.  Each one is added to cater for specific plotters,
         * though the behaviours invoked here might be appropriate in at
         * least some cases for more generic plotter types.  However,
         * the Plotter interface currently does not in these cases
         * provide enough information to identify programmatically
         * what type of GUI is required.
         * Possibly improve in future versions when it's clear which
         * kinds of plotter are sufficiently common to warrant their own
         * generic GUI controls. */

        if ( plotter instanceof FunctionPlotter ) {
            final FunctionPlotter fPlotter = (FunctionPlotter) plotter;
            return new LayerControlAction( plotter, stack ) {
                public LayerControl createLayerControl() {
                    return new FunctionLayerControl( fPlotter );
                }
            };
        }
        else if ( plotter instanceof SpectrogramPlotter ) {
            final SpectrogramPlotter sPlotter = (SpectrogramPlotter) plotter;
            return new LayerControlAction( plotter, stack ) {
                public LayerControl createLayerControl() {
                    return new SpectrogramLayerControl( sPlotter );
                }
            };
        }
        else if ( cgrp.isSinglePartialPosition() ) {
            return new LayerControlAction( plotter, stack ) {
                public LayerControl createLayerControl() {
                    PositionCoordPanel posCoordPanel =
                        new SimplePositionCoordPanel( cgrp.getExtraCoords(),
                                                      null );
                    return new SingleFormLayerControl( posCoordPanel, true,
                                                       nextSupplier,
                                                       tcListener,
                                                       plotter.getPlotterIcon(),
                                                       plotter,
                                                       baseConfigger );
                }
            };
        }

        /* Not great - no options for miscellaneous plotters with both
         * positional and non-positional coordinates.  Could be done if
         * necessary. */
        else if ( cgrp.getPositionCount() == 0 ) {
            return new LayerControlAction( plotter, stack ) {
                public LayerControl createLayerControl() {
                    PositionCoordPanel coordPanel =
                        new SimplePositionCoordPanel( cgrp.getExtraCoords(),
                                                      null );
                    return new BasicCoordLayerControl( plotter, coordPanel );
                }
            };
        }
        else {
            return null;
        }
    }
}
