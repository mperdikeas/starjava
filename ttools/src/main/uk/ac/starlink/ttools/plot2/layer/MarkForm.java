package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that just draws a fixed-size marker for each
 * position.  One or more positions per tuple may be marked.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public abstract class MarkForm implements ShapeForm {

    private final int npos_;
    private final String name_;
    private final Icon icon_;

    /** MarkForm instance for a single point per tuple. */
    public static final MarkForm SINGLE = createMarkForm( 1 );

    /** MarkForm instance for two points per tuple. */
    public static final MarkForm PAIR = createMarkForm( 2 );

    private static final Color DUMMY_COLOR = Color.GRAY;

    /**
     * Constructor.
     *
     * @param   npos   number of points to mark per tuple
     * @param   name   form name
     * @param   icon   form icon
     */
    protected MarkForm( int npos, String name, Icon icon ) {
        npos_ = npos;
        name_ = name;
        icon_ = icon;
    }

    public int getPositionCount() {
        return npos_;
    }

    public String getFormName() {
        return name_;
    }

    public Icon getFormIcon() {
        return icon_;
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    @Override
    public int hashCode() {
        return npos_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof MarkForm ) {
            MarkForm other = (MarkForm) o;
            return this.npos_ == other.npos_;
        }
        else {
            return false;
        }
    }

    /**
     * Factory method to create an instance of this class.
     *
     * @param   npos   number of positions per tuple
     * @return  form instance
     */
    public static MarkForm createMarkForm( final int npos ) {
        if ( npos == 1 ) {
            return new MarkForm( 1, "Mark", ResourceIcon.FORM_MARK ) {
                public ConfigKey[] getConfigKeys() {
                    return new ConfigKey[] {
                        StyleKeys.MARK_SHAPE,
                        StyleKeys.SIZE,
                    };
                }
                public Outliner createOutliner( ConfigMap config ) {
                    MarkShape shape = config.get( StyleKeys.MARK_SHAPE );
                    int size = config.get( StyleKeys.SIZE );
                    return createMarkOutliner( shape, size );
                }
            };
        }
        else {
            return new MarkForm( npos, "Marks",
                                 npos == 2 ? ResourceIcon.FORM_MARKS2
                                           : ResourceIcon.FORM_MARKS3 ) {
                public ConfigKey[] getConfigKeys() {
                    return new ConfigKey[] {
                        StyleKeys.MARK_SHAPE,
                        StyleKeys.SIZE,
                    };
                }
                public Outliner createOutliner( ConfigMap config ) {
                    MarkShape shape = config.get( StyleKeys.MARK_SHAPE );
                    int size = config.get( StyleKeys.SIZE );
                    return createMultiMarkOutliner( shape, size, npos );
                }
            };
        }
    }

    /**
     * Returns an outliner for use with single points.
     *
     * @param   shape  marker shape
     * @param   size   marker size
     * @return  single-point outliner
     */
    public static Outliner createMarkOutliner( MarkShape shape, int size ) {
        return new SingleMarkOutliner( shape, size );
    }

    /**
     * Returns an outliner for use with multiple points.
     *
     * @param  shape  marker shape
     * @param  size   marker size
     * @param  npos   number of positions per tuple
     * @return  multi-point outliner
     */
    public static Outliner createMultiMarkOutliner( MarkShape shape, int size,
                                                    int npos ) {
        return new MultiMarkOutliner( shape, size, npos );
    }

    /**
     * Creates a MarkStyle with a given size.
     *
     * @param  shape  marker shape
     * @param  size   marker size in pixels
     * @return   marker style
     */
    private static MarkStyle createMarkStyle( MarkShape shape, int size ) {
        return size == 0 ? MarkShape.POINT.getStyle( DUMMY_COLOR, 0 )
                         : shape.getStyle( DUMMY_COLOR, size );
    }

    /**
     * Creates a Glyph representing a marker.
     *
     * @param  shape  marker shape
     * @param  size  marker size
     * @return  marker glyph
     */
    public static Glyph createMarkGlyph( MarkShape shape, int size ) {
        final MarkStyle style = createMarkStyle( shape, size );
        final PixellatorPixerFactory pfact =
            new PixellatorPixerFactory( style.getPixelOffsets() );
        return new Glyph() {
            public void paintGlyph( Graphics g ) {
                style.drawShape( g );
            }
            public Pixer createPixer( Rectangle clip ) {
                return pfact.createPixer( clip );
            }
        };
    }

    /**
     * Returns a legend icon suitable for a single marker.
     *
     * @param  shape  marker shape
     * @param  size  marker size
     * @return   legend icon
     */
    public static Icon createLegendIcon( MarkShape shape, int size ) {
        final MarkStyle style = createMarkStyle( shape, size );
        final Icon baseIcon = style.getLegendIcon();
        final int width = baseIcon.getIconWidth();
        final int height = baseIcon.getIconHeight();
        return new Icon() {
            public int getIconWidth() {
                return width;
            }
            public int getIconHeight() {
                return height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                int xoff = x + width / 2;
                int yoff = y + height / 2;
                g.translate( xoff, yoff );
                style.drawLegendShape( g );
                g.translate( -xoff, -yoff );
            }
        };
    }

    /**
     * Returns a legend icon suitable for multiple markers.
     *
     * @param   shape  marker shape
     * @param   size   marker size
     * @param   npos   number of positions to show in icon
     */
    private static Icon createMultiLegendIcon( MarkShape shape, int size,
                                               final int npos ) {
        final MarkStyle style = createMarkStyle( shape, size );
        return new MultiPosIcon( npos ) {
            protected void paintPositions( Graphics g, Point[] positions ) {
                for ( int ip = 0; ip < npos; ip++ ) {
                    Point pos = positions[ ip ];
                    g.translate( pos.x, pos.y );
                    style.drawLegendShape( g );
                    g.translate( -pos.x, -pos.y );
                }
            }
        };
    }

    /**
     * Partial outliner implementation for use with MarkForm.
     * This is an abstract class with concrete implementations for
     * single and multiple position markers.
     * The implementations are not much different, and it would be
     * possible to use a single implementation with no special casing
     * for the single-position case.  However, the glyph placement done
     * by the one-point case is inside the single most time-critical
     * loop in the whole application, so we take any opportunity to
     * shave time off it.
     */
    private static abstract class MarkOutliner implements Outliner {
        final MarkStyle style_;
        final Glyph glyph_;
        final Icon icon_;

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         * @param  icon   legend icon
         */
        protected MarkOutliner( MarkShape shape, int size, Icon icon ) {
            style_ = createMarkStyle( shape, size );
            glyph_ = createMarkGlyph( shape, size );
            icon_ = icon;
        }

        /**
         * Returns a point cloud representing all the positions that
         * need to be plotted.
         *
         * @param  dataGeom  acquires data positions from tuple
         * @param  dataSpec  data spec
         * @return  point cloud
         */
        protected abstract PointCloud createPointCloud( DataGeom dataGeom,
                                                        DataSpec dataSpec );
 
        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            return new HashMap<AuxScale,AuxReader>();
        }

        public Object calculateBinPlan( Surface surface, DataGeom geom,
                                        Map<AuxScale,Range> auxRanges,
                                        DataStore dataStore, DataSpec dataSpec,
                                        Object[] knownPlans ) {
            return BinPlan
                  .calculatePointCloudPlan( createPointCloud( geom, dataSpec ),
                                            surface, dataStore, knownPlans );
        }

        public int[] getBinCounts( Object plan ) {
            BinPlan binPlan = (BinPlan) plan;
            Rectangle bigRect =
                new Rectangle( Integer.MIN_VALUE / 4, Integer.MIN_VALUE / 4,
                               Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2 );
            return convolve( binPlan.getBinner(), binPlan.getGridder(),
                             glyph_.createPixer( bigRect ) );
        }

        public long getPointCount( Object plan ) {
            return ((BinPlan) plan).getBinner().getTotal();
        }

        /**
         * Convolves a grid of bin counts with a marker shape to produce
         * a grid of values indicating how many pixels would be plotted
         * per grid position if the given shape had been stamped down once
         * for each entry in the bin count grid.
         * To put it another way, the marker shape acts as a (shaped top hat)
         * smoothing kernel.
         *
         * @param   binner  contains pixel counts per grid point
         * @param   gridder  contains grid geometry
         * @param  pixer  marker shape in terms of pixel iterator
         */
        private static int[] convolve( Binner binner, Gridder gridder,
                                       Pixer pixer ) {
            int nx = gridder.getWidth();
            int ny = gridder.getHeight();
            int[] buf = new int[ gridder.getLength() ];
            while ( pixer.next() ) {
                int px = pixer.getX();
                int py = pixer.getY();
                int ix0 = Math.max( 0, px );
                int ix1 = Math.min( nx, nx + px );
                int iy0 = Math.max( 0, py );
                int iy1 = Math.min( ny, ny + py );
                for ( int iy = iy0; iy < iy1; iy++ ) {
                    int jy = iy - py;
                    for ( int ix = ix0; ix < ix1; ix++ ) {
                        int jx = ix - px;
                        buf[ gridder.getIndex( ix, iy ) ] +=
                             binner.getCount( gridder.getIndex( jx, jy ) );
                    }
                }
            }
            return buf;
        }
    }

    /**
     * Mark outliner for a single point per tuple.
     */
    private static class SingleMarkOutliner extends MarkOutliner {

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         */
        public SingleMarkOutliner( MarkShape shape, int size ) {
            super( shape, size, createLegendIcon( shape, size ) );
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType2D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gp = new Point();
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos ) &&
                         surface.dataToGraphics( dpos, true, gp ) ) {
                        paperType.placeGlyph( paper, gp.x, gp.y,
                                              glyph_, color );
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType3D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gp = new Point();
            final double[] dz = new double[ 1 ];
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos ) &&
                         surface.dataToGraphicZ( dpos, true, gp, dz ) ) {
                        paperType.placeGlyph( paper, gp.x, gp.y, dz[ 0 ],
                                              glyph_, color );
                    }
                }
            };
        }

        protected PointCloud createPointCloud( DataGeom geom, DataSpec spec ) {
            return new PointCloud( new SubCloud( geom, spec, 0 ) );
        }


        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SingleMarkOutliner ) {
                MarkOutliner other = (SingleMarkOutliner) o;
                return this.style_.equals( other.style_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return style_.hashCode();
        }
    }

    /**
     * Mark outliner for multiple points per tuple.
     */
    private static class MultiMarkOutliner extends MarkOutliner {

        private final int npos_;

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         * @param  npos   number of points per tuple
         */
        public MultiMarkOutliner( MarkShape shape, int size, int npos ) {
            super( shape, size, createMultiLegendIcon( shape, size, npos ) );
            npos_ = npos;
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType2D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gp = new Point();
            final int npc = geom.getPosCoords().length;
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    for ( int ip = 0; ip < npos_; ip++ ) {
                        if ( geom.readDataPos( tseq, ip * npc, dpos ) &&
                             surface.dataToGraphics( dpos, true, gp ) ) {
                            paperType.placeGlyph( paper, gp.x, gp.y, glyph_,
                                                  color );
                        }
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType3D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gp = new Point();
            final double[] dz = new double[ 1 ];
            final int npc = geom.getPosCoords().length;
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    for ( int ip = 0; ip < npos_; ip++ ) {
                        if ( geom.readDataPos( tseq, ip * npc, dpos ) &&
                             surface.dataToGraphicZ( dpos, true, gp, dz ) ) {
                            paperType.placeGlyph( paper, gp.x, gp.y, dz[ 0 ],
                                                  glyph_, color );
                        }
                    }
                }
            };
        }

        protected PointCloud createPointCloud( DataGeom geom, DataSpec spec ) {
            return new PointCloud( SubCloud.createSubClouds( geom, spec,
                                                             npos_, false ) );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MultiMarkOutliner ) {
                MultiMarkOutliner other = (MultiMarkOutliner) o;
                return this.npos_ == other.npos_
                    && this.style_.equals( other.style_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 332;
            code = 23 * code + npos_;
            code = 23 * code + style_.hashCode();
            return code;
        }
    }
}
