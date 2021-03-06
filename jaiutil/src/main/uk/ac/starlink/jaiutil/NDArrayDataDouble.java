/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.jaiutil;

import java.awt.image.Raster;
import java.io.IOException;
import uk.ac.starlink.array.NDArray;

/**
 * Used for double precision NDArray data.
 *
 * @version $Id$
 * @author Allan Brighton
 * @author Peter W. Draper
 */
public class NDArrayDataDouble extends NDArrayData
{
    /** 
     * Constructor 
     */
    public NDArrayDataDouble( NDArray nda ) 
        throws IOException
    {
	super( nda );
    }

    /** 
     * Constructor 
     */
    public NDArrayDataDouble ( NDArray nda, int[] axes ) 
        throws IOException
    {
        super( nda, axes );
    }

    /** 
     * Fill in the given tile with the appropriate image data.
     */
    public Raster getTile( Raster tile, int subsample, 
                           int scaledWidth, int scaledHeight ) 
        throws IOException
    {
	double[] destArray = getRasterArray( tile );

	int tw = tile.getWidth(), 
	    th = tile.getHeight(),
	    x0 = tile.getMinX(), 
	    y0 = tile.getMinY();

        if ( subsample == 1 ) { 
            fillTile( destArray, x0, y0, tw, th );
        }
        else { 
            //  zoomed out: skip subsample pixels
            //  Use random access to speed up.
	    int x1 = Math.min( x0 + tw - 1, scaledWidth - 1 );
            int y1 = Math.min( y0 + th - 1, scaledHeight - 1 );
            for ( int j = y0; j <= y1; j++ ) {
                int dst = ( j - y0 ) * tw;
                long src = ( j * width + x0 ) * subsample;
                for ( int i = x0; i <= x1; i++ ) {
                    tiler.setOffset( src );
                    tiler.read( destArray, dst++, 1 );
                    src += subsample;
                }
            }
        }
	return tile;
    }

    /** 
     * Return a prescaled preview image at "1/factor" of the normal
     * size in the given raster tile.
     */
    public Raster getPreviewImage( Raster tile, int factor ) 
        throws IOException
    {
        double[] destArray = getRasterArray( tile );
        double[] line = new double[width];

        int tw = tile.getWidth(),
            th = tile.getHeight(),
            w = tw*factor,
            h = th*factor,
            n = 0, 
            m = 0;
        try {
            for ( int j = 0; j < h; j += factor ) {
                n =  m++ * tw;
                fillTile( line, 0, j, width, 1 );
                for ( int i = 0; i < w; i += factor ) {
                    destArray[n++] = line[i];
                }
            }
            return tile;
        }
        catch (Exception e) {
            throw new RuntimeException( e.getMessage() );
        }
    }

    /**
     * Retrieves the data array from a Raster as a <code>double[]</code>.
     */
    private static double[] getRasterArray( Raster tile ) {
        Object dataBuffer = tile.getDataBuffer();
        if ( dataBuffer instanceof javax.media.jai.DataBufferDouble ) {
            // JAI 1.1.1
            return ((javax.media.jai.DataBufferDouble) dataBuffer).getData();
        }
        else {
            // JAI 1.1.2
            return ((java.awt.image.DataBufferDouble) dataBuffer).getData();
        }
    }
}
