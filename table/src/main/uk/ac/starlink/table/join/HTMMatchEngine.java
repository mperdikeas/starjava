package uk.ac.starlink.table.join;

import edu.jhu.htm.core.Domain;
import edu.jhu.htm.core.HTMException;
import edu.jhu.htm.core.HTMindexImp;
import edu.jhu.htm.core.HTMrange;
import edu.jhu.htm.core.HTMrangeIterator;
import edu.jhu.htm.geometry.Circle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implements the object matching interface for sky coordinates
 * using the HTM (Hierarchical Triangular Mesh) pixel-indexing scheme.
 * <p>
 * Note that the {@link HEALPixMatchEngine} implementation normally gives
 * much faster matching than this and should generally be used in
 * preference.
 *
 * @author   Mark Taylor (Starlink)
 * @see      <a href="http://www.sdss.jhu.edu/htm/doc/"
 *                   >http://www.sdss.jhu.edu/htm/doc</a>
 */
public class HTMMatchEngine extends SkyMatchEngine {

    private HTMindexImp htm_;

    /**
     * Scaling factor which determines the size of the mesh cells used
     * as a multiple of the size of the separation.  It can be used as
     * a tuning parameter.  It must be &gt;1.
     */
    public final static double MESH_SCALE = 1;

    /**
     * Constructs a new match engine which considers two points
     * (RA,Dec tuples) to match if they are within a given angular
     * distance on the celestial sphere.
     *
     * @param   separation   match radius in radians
     * @param   useErrors   if true, per-row errors can be specified as
     *          a third element of the tuples; otherwise only the fixed
     *          separation value counts
     */
    public HTMMatchEngine( double separation, boolean useErrors ) {
        super( separation, useErrors );
    }

    public void setSeparation( double separation ) {
        super.setSeparation( separation );

        /* Construct an HTM index with mesh elements of a size suitable
         * for the requested resolution. */
        // assert MESH_SCALE > ??; not sure what is the maximum sensible value
        try {
            htm_ = new HTMindexImp( Math.toDegrees( separation ) * MESH_SCALE );
        }
        catch ( HTMException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad separation? "
                                              + separation + " radians" )
                 .initCause( e );
        }
    }

    /**
     * Returns all the HTM cells which fall wholly or partially within
     * <tt>err</tt> radians of a given position.
     *
     * @param  ra   right ascension
     * @param  dec  declination
     * @param  err  error
     * @return  bin list
     */
    public Object[] getBins( double ra, double dec, double err ) {
        double arcminErr = Math.toDegrees( err ) * 60.0;
        Circle zone = new Circle( ra, dec, arcminErr );

        /* Get the intersection as a range of HTM pixels.
         * The more obvious 
         *      range = htm_.intersect( zone.getDomain() );
         * is flawed, since it can return pixel IDs which refer to 
         * pixels at different HTM levels (i.e. of different sizes).
         * By doing it as below (on advice from Wil O'Mullane) we
         * ensure that all the pixels are at the HTM's natural level. */
        Domain domain = zone.getDomain();
        domain.setOlevel( htm_.maxlevel_ );
        HTMrange range = new HTMrange();
        domain.intersect( htm_, range, false );

        /* Accumulate a list of the pixel IDs. */
        List binList = new ArrayList();
        try {
            for ( Iterator it = new HTMrangeIterator( range, false );
                  it.hasNext(); ) {
                binList.add( it.next() );
            }
        }
        catch ( HTMException e ) {
            throw new RuntimeException( "Uh-oh", e );
        }
        return binList.toArray();
    }
}