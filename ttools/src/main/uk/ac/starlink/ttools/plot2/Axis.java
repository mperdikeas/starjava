package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Does geometry and drawing for a straight line axis.
 * Linear and logarithmic scales are supported; obtain one using
 * the {@link #createAxis} factory method.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public abstract class Axis {

    private final int glo_;
    private final int ghi_;
    private final double dlo_;
    private final double dhi_;

    /**
     * Constructor.
     *
     * @param   glo  minimum graphics coordinate
     * @param   ghi  maximum graphics coordinate
     * @param   dlo  minimum data coordinate
     * @param   dhi  maximum data coordinate
     */
    protected Axis( int glo, int ghi, double dlo, double dhi ) {
        glo_ = glo;
        ghi_ = ghi;
        dlo_ = dlo;
        dhi_ = dhi;
    }

    /**
     * Converts a data coordinate to the graphics position on this axis.
     *
     * @param   d  data coordinate
     * @return  graphics coordinate
     */
    public abstract int dataToGraphics( double d );

    /**
     * Converts a graphics position on this axis to a data coordinate.
     *
     * @param   g  graphics coordinate
     * @return   data coordinate
     */
    public abstract double graphicsToData( int g );

    /**
     * Returns the data bounds that result from performing an axis zoom
     * about a given data position.
     *
     * @param   d0   data reference position for zoom
     * @param  factor  amount to zoom
     * @return   2-element array giving new new data min/max coordinates
     */
    public abstract double[] dataZoom( double d0, double factor );

    /**
     * Returns the data bounds that result from performing an axis pan
     * between two given data positions.
     *
     * @param  d0  source data position
     * @param  d1  destination data position
     * @return   2-element array giving new new data min/max coordinates
     */
    public abstract double[] dataPan( double d0, double d1 );

    /**
     * Draws an axis title and supplied tickmarks.
     *
     * @param  ticks  tickmark array
     * @param  title  axis label text, may be null
     * @param  captioner  text positioning object
     * @param  orient  axis orientation code
     * @param  invert  whether to reverse sense of axis 
     * @param  g   graphics context
     */
    public void drawLabels( Tick[] ticks, String title, Captioner captioner,
                            Orientation orient, boolean invert, Graphics g ) {
        calculateLabels( ticks, title, captioner, orient, invert, g );
    }

    /**
     * Determines the bounds for axis and tickmark annotations.
     * The returned value is a bounding box for everything that would be
     * drawn by a corresponding call to {@link #drawLabels}.
     *
     * @param  ticks  tickmark array
     * @param  title  axis label text, may be null
     * @param  captioner  text positioning object
     * @param  orient  axis orientation code
     * @param  invert  whether to reverse sense of axis 
     * @return   bounding box for all annotations
     */
    public Rectangle getLabelBounds( Tick[] ticks, String title,
                                     Captioner captioner, Orientation orient,
                                     boolean invert ) {
        return calculateLabels( ticks, title, captioner, orient, invert, null );
    }

    /**
     * Does the work for painting and calculating bounds for axis annotations.
     *
     * @param  ticks  tickmark array
     * @param  title  axis label text, may be null
     * @param  captioner  text positioning object
     * @param  orient  axis orientation code
     * @param  invert  whether to reverse sense of axis 
     * @param  graphics context, or null if no painting is required
     * @return   bounding box for all annotations
     */
    private Rectangle calculateLabels( Tick[] ticks, String title,
                                       Captioner captioner, Orientation orient,
                                       boolean invert, Graphics g ) {
        boolean hasGraphics = g != null;
        Graphics2D g2 = hasGraphics ? (Graphics2D) g : null;
        AffineTransform trans0 = hasGraphics ? g2.getTransform() : null;
        double det0 = trans0 == null ? 0 : trans0.getDeterminant();

        /* Without this check, you can get JVM crashes! */
        if ( Double.isNaN( det0 ) || det0 == 0 ) {
            hasGraphics = false;
            trans0 = null;
        }
        int cpad = captioner.getPad();
        Rectangle textBounds = null;
        Rectangle tickBounds = new Rectangle();

        /* Place ticks. */
        int tickUnit = orient.isDown() ? -2 : +2;
        for ( int it = 0; it < ticks.length; it++ ) {
            Tick tick = ticks[ it ];
            String label = tick.getLabel();
            double gx = dataToGraphics( tick.getValue() );
            double tx = invert ? ghi_ - gx : gx - glo_;
            AffineTransform tTrans =
                AffineTransform.getTranslateInstance( tx, 0 );
            Rectangle cbounds = label == null
                              ? null
                              : captioner.getCaptionBounds( label );
            AffineTransform oTrans =
                label == null ? null
                              : orient.captionTransform( cbounds, cpad );

            /* Update bounding box for tick labels. */
            if ( label != null ) {
                Rectangle box = combineTrans( tTrans, oTrans )
                               .createTransformedShape( cbounds ).getBounds();
                tickBounds.add( box );
            }

            /* If we are drawing, draw now. */
            if ( hasGraphics ) {
                g2.setTransform( combineTrans( trans0, tTrans ) );
                if ( label != null ) {
                    g2.drawLine( 0, -tickUnit, 0, tickUnit );
                    g2.setTransform( combineTrans( trans0, tTrans, oTrans ) );
                    captioner.drawCaption( label, g2 );
                }
                else {
                    g2.drawLine( 0, 0, 0, tickUnit );
                }
                g2.setTransform( trans0 );
            }
        }
        if ( hasGraphics ) {
            g2.setTransform( trans0 );
        }
        textBounds = combineRect( textBounds, tickBounds );
 
        /* Place title. */
        if ( title != null ) {
            Rectangle cbounds = captioner.getCaptionBounds( title );
            int tx = ( ghi_ - glo_ ) / 2 - cbounds.width / 2;
            int ty = orient.isDown()
                   ? + tickBounds.height + cpad - cbounds.y
                   : - tickBounds.height - cpad - cbounds.height - cbounds.y;
            AffineTransform tTrans =
                AffineTransform.getTranslateInstance( tx, ty );
            if ( hasGraphics ) {
                g2.setTransform( combineTrans( trans0, tTrans ) );
                captioner.drawCaption( title, g2 );
                g2.setTransform( trans0 );
            }
            Rectangle titleBounds =
                combineTrans( tTrans )
               .createTransformedShape( cbounds ).getBounds();
            textBounds = combineRect( textBounds, titleBounds );
        }
        return textBounds;
    }

    /**
     * Factory method to create a linear or logarithmic axis.
     *
     * @param   glo   minimum graphics coordinate
     * @param   ghi   maximum graphics coordinate
     * @param   dlo   minimum data coordinate
     * @param   dhi   maximum data coordinate
     * @param   log   true for logarithmic scaling, false for linear
     * @param   flip  true if the data coordinates should run
     *                in the opposite sense to the graphics coordinates
     */
    public static Axis createAxis( int glo, int ghi, double dlo, double dhi,
                                   boolean log, boolean flip ) {
        return log ? new LogAxis( glo, ghi, dlo, dhi, flip )
                   : new LinearAxis( glo, ghi, dlo, dhi, flip );
    }

    /**
     * Convenience method to combine multiple AffineTransforms and produce
     * the result (product) of them all. 
     *
     * @param   transforms   list of one or more transforms
     * @return  concatenation of input transforms
     */
    private static AffineTransform
            combineTrans( AffineTransform... transforms ) {
        AffineTransform trans = new AffineTransform();
        for ( int i = 0; i < transforms.length; i++ ) {
            trans.concatenate( transforms[ i ] );
        }
        return trans;
    }

    /**
     * Combines two rectangles, coping with the case where one or both
     * are null.
     *
     * @param   r1  first rectangle
     * @param   r2  second rectangle
     * @return   union of r1 and r2
     */
    private static Rectangle combineRect( Rectangle r1, Rectangle r2 ) {
        if ( r1 == null ) {
            return r2;
        }
        else {
            Rectangle rect = new Rectangle( r1 );
            if ( r2 != null ) {
                rect.add( r2 );
            }
            return rect;
        }
    }

    /**
     * Axis implementation with linear scaling.
     */
    private static class LinearAxis extends Axis {
        private final double a_;
        private final double a1_;
        private final double b_;
        private final double dlo_;
        private final double dhi_;

        /**
         * Constructor.
         *
         * @param   glo   minimum graphics coordinate
         * @param   ghi   maximum graphics coordinate
         * @param   dlo   minimum data coordinate
         * @param   dhi   maximum data coordinate
         * @param   flip  true if the data coordinates should run
         *                in the opposite sense to the graphics coordinates
         */
        public LinearAxis( int glo, int ghi, double dlo, double dhi,
                           boolean flip ) {
            super( glo, ghi, dlo, dhi );
            dlo_ = dlo;
            dhi_ = dhi;
            a_ = ( flip ? -1.0 : +1.0 ) * ( ghi - glo ) / ( dhi - dlo );
            a1_ = 1.0 / a_;
            b_ = ( flip ? ghi : glo ) - a_ * dlo;
        }

        public int dataToGraphics( double d ) {
            return (int) ( b_ + a_ * d );
        }

        public double graphicsToData( int g ) {
            return ( g - b_ ) * a1_;
        }

        public double[] dataZoom( double d0, double factor ) {
            return zoom( dlo_, dhi_, d0, factor, false );
        }

        public double[] dataPan( double d0, double d1 ) {
            return pan( dlo_, dhi_, d0, d1, false );
        }
    }

    /**
     * Axis implementation with logarithmic scaling.
     */
    private static class LogAxis extends Axis {
        private final double a_;
        private final double a1_;
        private final double b_;
        private final double dlo_;
        private final double dhi_;

        /**
         * Constructor.
         *
         * @param   glo   minimum graphics coordinate
         * @param   ghi   maximum graphics coordinate
         * @param   dlo   minimum data coordinate
         * @param   dhi   maximum data coordinate
         * @param   flip  true if the data coordinates should run
         *                in the opposite sense to the graphics coordinates
         */
        public LogAxis( int glo, int ghi, double dlo, double dhi,
                        boolean flip ) {
            super( glo, ghi, dlo, dhi );
            dlo_ = dlo;
            dhi_ = dhi;
            a_ = ( flip ? -1.0 : +1.0 )
               * ( ghi - glo ) / ( Math.log( dhi ) - Math.log( dlo ) );
            b_ = ( flip ? ghi : glo ) - a_ * Math.log( dlo );
            a1_ = 1.0 / a_;
        }

        public int dataToGraphics( double d ) {
            return (int) ( b_ + a_ * Math.log( d ) );
        }

        public double graphicsToData( int g ) {
            return Math.exp( ( g - b_ ) * a1_ );
        }

        public double[] dataZoom( double d0, double factor ) {
            return zoom( dlo_, dhi_, d0, factor, true );
        }

        public double[] dataPan( double d0, double d1 ) {
            return pan( dlo_, dhi_, d0, d1, true );
        }
    }

    /**
     * Utility method for axis panning.
     *
     * @param   dlo  initial axis lower bound
     * @param   dhi  initial axis upper bound
     * @param   d0   pan gesture start position
     * @param   d1   pan gesture end position
     * @param   isLog  false for linear axis, true for logarithmic
     * @return  2-element array giving final (panned) axis {lower,upper} bounds
     */
    public static double[] pan( double dlo, double dhi,
                                double d0, double d1, boolean isLog ) {
        if ( isLog ) {
            double d10 = d0 / d1;
            return new double[] { dlo * d10, dhi * d10 };
        }
        else {
            double d10 = d1 - d0;
            return new double[] { dlo - d10, dhi - d10 };
        }
    }

    /**
     * Utility method for axis zooming.
     *
     * @param   dlo  initial axis lower bound
     * @param   dhi  initial axis upper bound
     * @param   d0   zoom gesture reference position
     * @param   factor  zoom factor
     * @param   isLog   false for linear axis, true for logarithmic
     * @return  2-element array giving final (zoomed) axis {lower,upper} bounds
     */
    public static double[] zoom( double dlo, double dhi,
                                 double d0, double factor, boolean isLog ) {
        if ( isLog ) {
            double f1 = 1. / factor;
            return new double[] { d0 * Math.pow( dlo / d0, f1 ),
                                  d0 * Math.pow( dhi / d0, f1 ) };
        }
        else {
            double f1 = 1. / factor;
            return new double[] { d0 + ( dlo - d0 ) * f1,
                                  d0 + ( dhi - d0 ) * f1 };
        }
    }
}
