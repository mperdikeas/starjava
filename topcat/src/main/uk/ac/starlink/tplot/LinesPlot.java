package uk.ac.starlink.tplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.BitSet;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which paints a stack of line plots.
 *
 * @author   Mark Taylor
 * @since    3 Mar 2006
 */
public class LinesPlot extends TablePlot {

    private LinesPlotState state_;
    private Points points_;
    private PlotSurface[] surfaces_;
    private int[] sequence_;
    private Rectangle plotRegion_;
    private int[][] work_;
    private final int axPad_ = 16;

    /**
     * Constructor.
     */
    public LinesPlot() {
        surfaces_ = new PlotSurface[ 0 ];
        work_ = new int[ 2 ][ 10240 ];
    }

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     * 
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        if ( points != points_ ) {
            sequence_ = sortX( points );
        }
        points_ = points;
    }

    /**
     * Sets the plot state for this plot.  This characterises how the
     * plot will be done next time this component is painted.
     *
     * @param  state  plot state
     */
    public void setState( LinesPlotState state ) {
        state_ = state;
    }

    /**
     * Returns the most recently set state for this plot.
     *
     * @return  plot state
     */
    public LinesPlotState getState() {
        return state_;
    }

    public PlotSurface[] getSurfaces() {
        return surfaces_;
    }

    /**
     * Performs the drawing.
     *
     * @param  g  graphics conext
     * @param  c  target component
     */
    private void drawData( Graphics g, Component c ) {

        /* Acquire state. */
        Points points = points_;
        LinesPlotState state = getState();
        if ( points == null || state == null || ! state.getValid() ) {
            return;
        }
        int npoint = points.getCount();
        boolean grid = state.getGrid();
        boolean xLogFlag = state.getLogFlags()[ 0 ];
        boolean xFlipFlag = state.getFlipFlags()[ 0 ];
        boolean[] yLogFlags = state.getYLogFlags();
        final boolean[] yFlipFlags = state.getYFlipFlags();
        ValueInfo[] yInfos = state.getYAxes();
        int ngraph = yInfos.length;
        double[] xRange = state.getRanges()[ 0 ];
        double[][] yRanges = state.getYRanges();

        /* Get component dimensions. */
        FontMetrics fm = g.getFontMetrics();
        int height = getSize().height;
        int width = getSize().width;
        Insets border = getInsets();
        border.top++;
        border.right++;

        /* Work out how much space is required for the annotations at the
         * bottom of the plot (single annotated X axis). */
        int approxWidth = width - border.left - border.right;
        border.bottom +=
            new AxisLabeller( state.getAxisLabels()[ 0 ],
                              xRange[ 0 ], xRange[ 1 ], approxWidth,
                              state.getLogFlags()[ 0 ],
                              state.getFlipFlags()[ 0 ], fm, AxisLabeller.X,
                              10, axPad_, axPad_ )
           .getAnnotationHeight();

        /* Work out the available height for each plotted graph. */
        final int yInc = ( height - border.bottom - border.top ) / ngraph;

        /* Adjust border.top so it mops up any unused pixels (useful for
         * positioning calculations). */
        border.top = height - border.bottom - yInc * ngraph;

        /* Assemble an array of graphs to draw data on, and calculate 
         * the borders we need to leave for axis annotation. */
        GraphSurface[] graphs = new GraphSurface[ ngraph ];
        AxisLabeller[] yAxes = new AxisLabeller[ ngraph ];
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            double ylo = yRanges[ igraph ][ 0 ];
            double yhi = yRanges[ igraph ][ 1 ];
            graphs[ igraph ] =
                new GraphSurface( this,
                                  xLogFlag, yLogFlags[ igraph ],
                                  xFlipFlag, yFlipFlags[ igraph ] );
            graphs[ igraph ].setDataRange( xRange[ 0 ], ylo, xRange[ 1 ], yhi );
            yAxes[ igraph ] =
                new AxisLabeller( state.getYAxisLabels()[ igraph ], ylo, yhi,
                                  yInc,
                                  yLogFlags[ igraph ], yFlipFlags[ igraph ],
                                  fm, AxisLabeller.Y, 6, axPad_, axPad_ );
            int left = yAxes[ igraph ].getAnnotationHeight();
            border.left = Math.max( border.left,
                                    yAxes[ igraph ].getAnnotationHeight() );
        }

        /* Work out available width for plotted graphs and set up axis
         * labeller appropriately. */
        final int xInc = width - border.left - border.right;
        final int xPos = border.left;
        int yPos = height - border.bottom - yInc;
        AxisLabeller xAxis = new AxisLabeller( state.getAxisLabels()[ 0 ],
                                               xRange[ 0 ], xRange[ 1 ], xInc,
                                               state.getLogFlags()[ 0 ],
                                               state.getFlipFlags()[ 0 ],
                                               fm, AxisLabeller.X, 10,
                                               axPad_, axPad_ );

        /* Position each graph and draw the axes. */
        for ( int igraph = 0; igraph < ngraph; igraph++ ) {
            final GraphSurface graph = graphs[ igraph ];
            Rectangle displayBox = new Rectangle( xPos, yPos, xInc, yInc );
            graph.setBounds( displayBox );

            /* Paint graph background. */
            Color textColor = Color.BLACK;
            Color gridColor = Color.LIGHT_GRAY;
            Graphics g1 = g.create();
            graph.paintSurface( g1 );
            g1.setColor( Color.WHITE );
            g1.fillRect( xPos, yPos, xInc, yInc );

            /* Draw the X axis. */
            Graphics gx = g1.create();
            gx.translate( xPos, yPos + yInc );
            xAxis.setDrawText( igraph == 0 );
            if ( grid ) {
                gx.setColor( gridColor );
                xAxis.drawGridLines( gx, 0, -yInc );
            }
            gx.setColor( textColor );
            xAxis.annotateAxis( gx );
            gx.dispose();

            /* Draw the Y axis. */
            Graphics2D gy = (Graphics2D) g1.create();
            gy.translate( xPos, yPos + yInc );
            gy.rotate( - Math.PI * 0.5 );

            /* Draw Y grid lines if required. */
            if ( grid ) {
                gy.setColor( gridColor );
                yAxes[ igraph ].drawGridLines( gy, 0, xInc );
            }

            /* Draw a single y=0 grid line if required. */
            if ( state.getYZeroFlag() && yRanges[ igraph ][ 0 ] < 0.0
                                      && yRanges[ igraph ][ 1 ] > 0.0 ) {
                gy.setColor( gridColor );
                yAxes[ igraph ].drawGridLine( gy, 0, xInc, 0.0 );
            }

            /* Annotate the Y axis. */
            gy.setColor( textColor );
            yAxes[ igraph ].annotateAxis( gy );
            gy.dispose();

            /* Draw outline. */
            g1.setColor( textColor );
            g1.drawRect( xPos, yPos, xInc, yInc );
            g1.dispose();

            /* Move to next position. */
            yPos -= yInc;
        }

        /* Draw data points and lines. */
        RowSubset[] sets = state.getPointSelection().getSubsets();
        Style[] styles = state.getPointSelection().getStyles();
        int[] graphIndices = state.getGraphIndices();
        int nset = sets.length;
        int nerr = points.getNerror();
        int[] xoffs = new int[ nerr ];
        int[] yoffs = new int[ nerr ];
        for ( int iset = 0; iset < nset; iset++ ) {
            MarkStyle style = (MarkStyle) styles[ iset ];
            GraphSurface graph = graphs[ graphIndices[ iset ] ];
            boolean noLines = style.getLine() != MarkStyle.DOT_TO_DOT;
            boolean hasErrors = MarkStyle.hasErrors( style, points );
            Rectangle graphClip = graph.getClip().getBounds();
            if ( g.hitClip( graphClip.x, graphClip.y,
                            graphClip.width, graphClip.height ) ) {
                Graphics g1 = g.create();
                g1.clipRect( graphClip.x, graphClip.y,
                             graphClip.width, graphClip.height );
                PointPlotter plotter =
                    new PointPlotter( g1, style, state.getAntialias(),
                                      hasErrors, work_[ 0 ], work_[ 1 ] );
                RowSubset rset = sets[ iset ];
                for ( int ix = 0; ix < npoint; ix++ ) {
                    int ip = sequence_ == null ? ix : sequence_[ ix ];
                    if ( rset.isIncluded( (long) ip ) ) {
                        double[] coords = points.getPoint( ip );
                        Point point =
                            graph.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                  noLines );
                        if ( point != null ) {
                            if ( hasErrors ) {
                                double[][] errors = points.getErrors( ip );
                                if ( ScatterPlot
                                    .transformErrors( point, coords, errors,
                                                      graph, xoffs, yoffs ) ) {
                                    plotter.errors( point, xoffs, yoffs );
                                }
                            }
                            plotter.point( point );
                        }
                    }
                }
                plotter.flush();
                plotter.dispose();
                g1.dispose();
            }
        }

        /* Draw text labels. */
        if ( points.hasLabels() ) {
            for ( int iset = 0; iset < nset; iset++ ) {
                MarkStyle style = (MarkStyle) styles[ iset ]; 
                GraphSurface graph = graphs[ graphIndices[ iset ] ];
                Rectangle graphClip = graph.getClip().getBounds();
                if ( g.hitClip( graphClip.x, graphClip.y,
                                graphClip.width, graphClip.height ) ) {
                    RowSubset rset = sets[ iset ];
                    for ( int ix = 0; ix < npoint; ix++ ) {
                        int ip = sequence_ == null ? ix : sequence_[ ix ];
                        String label = points.getLabel( ip );
                        if ( label != null && label.trim().length() > 0 &&
                             rset.isIncluded( (long) ip ) ) {
                            double[] coords = points.getPoint( ip );
                            Point point =
                                graph.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                      true );
                            if ( point != null ) {
                                style.drawLabel( g, point.x, point.y, label );
                            }
                        }
                    }
                }
            }
        }

        /* Mark active points. */
        MarkStyle target = MarkStyle.targetStyle();
        Graphics targetGraphics = g.create();
        targetGraphics.setColor( Color.BLACK );
        int[] activePoints = state.getActivePoints();
        for ( int i = 0; i < activePoints.length; i++ ) {
            int ip = activePoints[ i ];
            PlotSurface surface = null;
            for ( int iset = 0; iset < nset && surface == null; iset++ ) {
                if ( sets[ iset ].isIncluded( ip ) ) {
                    surface = graphs[ graphIndices[ iset ] ];
                }
            }
            if ( surface != null && points.getCount() > 0 ) {
                double[] coords = points.getPoint( ip );
                Point point =
                    surface.dataToGraphics( coords[ 0 ], coords[ 1 ], true );
                if ( point != null ) {
                    target.drawMarker( targetGraphics, point.x, point.y );
                }
            }
        }

        /* Store graph set. */
        surfaces_ = graphs;
        plotRegion_ = graphs.length > 0 ? graphs[ 0 ].getClip().getBounds()
                                        : new Rectangle();
        for ( int i = 0; i < graphs.length; i++ ) {
            plotRegion_.add( graphs[ i ].getClip().getBounds() );
        }

        /* Notify listeners that the plot has changed.  We haven't counted
         * statistics, so use dummy values. */
        firePlotChangedLater( new PlotEvent( this, state, -1, -1, -1 ) );
    }

    /**
     * Returns a bit vector indicating which points are in the X range
     * currently visible within this plot.
     *
     * @return   bit vector with true marking point indices in visible X range
     */
    public BitSet getPointsInRange() {
        double xlo = state_.getRanges()[ 0 ][ 0 ];
        double xhi = state_.getRanges()[ 0 ][ 1 ];
        Points points = points_;
        int npoint = points.getCount();
        RowSubset[] sets = state_.getPointSelection().getSubsets();
        int nset = sets.length;
        BitSet inRange = new BitSet();
        for ( int ip = 0; ip < npoint; ip++ ) {
            long lp = (long) ip;
            boolean use = false;
            for ( int is = 0; is < nset && ! use; is++ ) {
                use = use || sets[ is ].isIncluded( lp );
            }
            if ( use ) {
                double[] coords = points.getPoint( ip );
                double x = coords[ 0 ];
                if ( x >= xlo && x <= xhi ) {
                    inRange.set( ip );
                }
            }
        }
        return inRange;
    }

    /**
     * Returns an iterator over the points which are visible in this plot.
     *
     * @return  point iterator
     */
    public PointIterator getPlottedPointIterator() {
        if ( ! state_.getValid() ) {
            return null;
        }
        final Points points = points_;
        final int npoint = points.getCount();
        final RowSubset[] sets = state_.getPointSelection().getSubsets();
        final int nset = sets.length;
        final PlotSurface[] setSurfaces = new PlotSurface[ nset ];
        int[] graphIndices = state_.getGraphIndices();
        for ( int iset = 0; iset < nset; iset++ ) {
            setSurfaces[ iset ] = surfaces_[ graphIndices[ iset ] ];
        }
        return new PointIterator() {
            int ip = -1;
            int[] point = new int[ 3 ];
            protected int[] nextPoint() {
                while ( ++ip < npoint ) {
                    long lp = (long) ip;
                    PlotSurface surface = null;
                    for ( int is = 0; is < nset && surface == null; is++ ) {
                        if ( sets[ is ].isIncluded( lp ) ) {
                            surface = setSurfaces[ is ];
                        }
                    }
                    if ( surface != null ) {
                        double[] coords = points.getPoint( ip );
                        double x = coords[ 0 ];
                        double y = coords[ 1 ];
                        Point p = surface.dataToGraphics( x, y, true );
                        if ( p != null ) {
                            point[ 0 ] = ip;
                            point[ 1 ] = p.x;
                            point[ 2 ] = p.y;
                            return point;
                        }
                    }
                }
                return null;
            }
        };
    }

    /**
     * Returns the rectangle which is the union of all the graph regions,
     * that is the region between the axes, for the last time a plot was
     * drawn.
     *
     * @return   plot region rectangle
     */
    public Rectangle getPlotRegion() {
        return plotRegion_;
    }

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        if ( isOpaque() ) {
            Color color = g.getColor();
            g.setColor( getBackground() );
            g.fillRect( 0, 0, getWidth(), getHeight() );
            g.setColor( color );
        }
        drawData( g, this );
    }

    /**
     * Returns an array giving the order in which the elements of the
     * Points structure should be processed.  This is numerically
     * increasing order of the X coordinate.
     * A null return indicates that the natural ordering may be used.
     *
     * @param   points   points data
     * @return  array of indices of ordered elements of <code>points</code>
     */
    private int[] sortX( Points points ) {
        int npoint = points.getCount();

        /* As an optimisation, check if the points are sorted by X already.
         * This will often be the case and we can save ourselves construction
         * and disposal of a potentially large array in this case. */
        boolean sorted = true;
        double last = Double.NaN;
        for ( int i = 0; i < npoint && sorted; i++ ) {
            double[] coords = points.getPoint( i );
            if ( coords[ 0 ] < last ) {
                sorted = false;
            }
            last = coords[ 0 ];
        }
        if ( sorted ) {
            return null;
        }

        /* Set up a sortable item class. */
        class Item implements Comparable {
            final int index_;
            final double x_;
            Item( int index, double x ) {
                index_ = index;
                x_ = x;
            }
            public int compareTo( Object o ) {
                double x1 = this.x_;
                double x2 = ((Item) o).x_;
                if ( x1 > x2 ) {
                    return +1;
                }
                else if ( x1 < x2 ) {
                    return -1;
                }
                else if ( x1 == x2 ) {
                    return 0;
                }
                else if ( Double.isNaN( x1 ) && Double.isNaN( x2 ) ) {
                    return 0;
                }
                else if ( Double.isNaN( x1 ) ) {
                    return +1;
                }
                else if ( Double.isNaN( x2 ) ) {
                    return -1;
                }
                else {
                    throw new AssertionError( x1 + " cmp " + x2 + "??" );
                }
            }
        }

        /* Construct and populate an array representing the points data. */
        Item[] items = new Item[ npoint ];
        for ( int i = 0; i < npoint; i++ ) {
            double[] coords = points.getPoint( i );
            items[ i ] = new Item( i, coords[ 0 ] );
        }

        /* Sort the array. */
        Arrays.sort( items );

        /* Extract the sorted indices into a usable form. */
        int[] indices = new int[ npoint ];
        for ( int i = 0; i < npoint; i++ ) {
            indices[ i ] = items[ i ].index_;
        }

        /* Return the result. */
        return indices;
    }

    /**
     * Object which can accept points to plot and draw them onto the
     * graphics context in a suitable way.
     */
    private static class PointPlotter {
        private final MarkStyle style_;
        private final Graphics pointGraphics_;
        private final Graphics lineGraphics_;
        private final Rectangle pointBounds_;
        private final int huge_;
        private final boolean hasLines_;
        private final boolean hasMarks_;
        private final boolean hasErrors_;
        private int xLo_;
        private int xHi_;
        private Point lastPoint_;
        private boolean lastInclude_;
        private int[] xWork_;
        private int[] yWork_;
        private int nWork_;
        private int iLine_;

        /**
         * Constructor.
         *
         * @param  g  base graphics context - the clip is used to optimise
         *            what gets plotted
         * @param  style  the mark/line plotting style for points
         * @param  antialiasLines  true iff you want lines antialiased
         * @param  hasErrors   true if you might want errors drawn
         * @param  work1  workspace array
         * @param  work2  workspace array
         */
        PointPlotter( Graphics g, MarkStyle style, boolean antialiasLines,
                      boolean hasErrors, int[] work1, int[] work2 ) {
            style_ = style;
            xWork_ = work1;
            yWork_ = work2;
            nWork_ = Math.min( xWork_.length, yWork_.length );

            /* Work out which components we will need to be drawing. */
            hasLines_ = style.getLine() == MarkStyle.DOT_TO_DOT;
            hasMarks_ = ! style.getHidePoints();
            hasErrors_ = hasErrors;

            /* Set up a graphics context for plotting points.
             * Null if no points are to be plotted. */
            pointGraphics_ = hasMarks_ || hasErrors_ ? g.create() : null;

            /* Set up a graphics context for drawing lines.
             * Null if no lines are to be drawn. */
            if ( hasLines_ ) {
                lineGraphics_ = g.create();
                lineGraphics_.setColor( style.getColor() );
                if ( lineGraphics_ instanceof Graphics2D ) {
                    Graphics2D lg2 = (Graphics2D) lineGraphics_;
                    lg2.setStroke( style.getStroke( BasicStroke.CAP_BUTT,
                                                    BasicStroke.JOIN_ROUND ) );
                    lg2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        antialiasLines ? RenderingHints.VALUE_ANTIALIAS_ON
                                       : RenderingHints.VALUE_ANTIALIAS_OFF );
                }
            }
            else {
                lineGraphics_ = null;
            }

            /* Prepare clipping information. */
            Rectangle gClip = g.getClipBounds();
            int maxr = style.getMaximumRadius();
            pointBounds_ = new Rectangle( gClip.x - maxr, gClip.y - maxr,
                                          gClip.width + maxr * 2,
                                          gClip.height + maxr * 2 );
            xLo_ = gClip.x;
            xHi_ = gClip.x + gClip.width;
            huge_ = Math.max( gClip.height, gClip.width ) * 100;
        }

        /**
         * Adds a point to the sequence to be plotted.
         *
         * @param  point  point to plot
         */
        void point( Point point ) {
            if ( ! point.equals( lastPoint_ ) ) {
                if ( hasMarks_ && pointBounds_.contains( point ) ) {
                    style_.drawMarker( pointGraphics_, point.x, point.y );
                }
                if ( hasLines_ ) {
                    boolean include = point.x >= xLo_ && point.x <= xHi_;
                    if ( include && ! lastInclude_ && lastPoint_ != null ) {
                        addLineVertex( lastPoint_.x, lastPoint_.y );
                    }
                    if ( include || lastInclude_ ) {
                        addLineVertex( point.x, point.y );
                    }
                    lastInclude_ = include;
                }
                assert lastPoint_ != point
                    : "PlotSurface keeps returning same mutable Point object?";
                lastPoint_ = point;
            }
        }

        /**
         * Adds a set of errors to the sequence to be plotted.
         *
         * @param  point  central point
         * @param  xoffs  error point offsets in X dimension
         * @param  yoffs  error point offsets in Y dimension
         */
        void errors( Point point, int[] xoffs, int[] yoffs ) {
            if ( hasErrors_ ) {
                style_.drawErrors( pointGraphics_, point.x, point.y,
                                   xoffs, yoffs );
            }
        }

        /**
         * Adds a vertex to the list which will have lines drawn between
         * them.
         *
         * @param   x  X graphics coordinate
         * @param   y  Y graphics coordinate
         */
        private void addLineVertex( int x, int y ) {

            /* If we've filled up the points buffer, flush it. */
            if ( iLine_ >= nWork_ ) {
                flush();
            }

            /* If an attempt is made to draw to a line which is monstrously
             * far off the graphics clip, it can lead to problems (e.g. 
             * the display system attempting to locate so much memory that
             * the kernel kills the JVM).  In this case, approximate the
             * point to somewhere far away in roughly the right direction.
             * This isn't likely to happen very often in any case. */
            x = Math.max( -huge_, Math.min( huge_, x ) );
            y = Math.max( -huge_, Math.min( huge_, y ) );

            /* Store the point for later plotting. */
            xWork_[ iLine_ ] = x;
            yWork_[ iLine_ ] = y;
            iLine_++;
        }

        /**
         * Ensures that all points have been drawn.  
         * Since points are plotted in bursts for reasons of aesthetics and
         * efficiency, this must be called after all {@link #point} calls
         * to ensure that the drawing has actuall been done.
         */
        void flush() {
            if ( iLine_ > 1 ) {
                lineGraphics_.drawPolyline( xWork_, yWork_, iLine_ );
                xWork_[ 0 ] = xWork_[ iLine_ - 1 ];
                yWork_[ 0 ] = yWork_[ iLine_ - 1 ];
                iLine_ = 1;
            }
        }

        /**
         * Should be called to release resources when this plotter is no
         * longer required.  Calls {@link #flush}.
         */
        void dispose() {
            flush();
            if ( pointGraphics_ != null ) {
                pointGraphics_.dispose();
            }
            if ( lineGraphics_ != null ) {
                lineGraphics_.dispose();
            }
        }
    }
}