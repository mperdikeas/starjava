package uk.ac.starlink.hds;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.ndx.BridgeNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxBuilder;
import uk.ac.starlink.ndx.NdxImpl;

/**
 * Turns URLs which reference NDF structures into Ndx objects.
 * <p>
 * URLs are given in the format:
 * <blockquote>
 *    <i>container</i><tt>.sdf</tt>
 * </blockquote>
 * or
 * <blockquote>
 *    <i>container</i><tt>.sdf#</tt><i>path</i>
 * </blockquote>
 * where the <i>container</i><tt>.sdf</tt> part is a full absolute or
 * relative URL referring
 * to the HDS container file, and the optional fragment identifier
 * gives the HDS path within that container file in the traditional
 * dot-separated format.  If there is no fragment identifier
 * (no <tt>#</tt>), the object at the top level of the HDS container
 * file is understood.
 * <p>
 * This is a singleton class; use {@link getInstance} to get an instance.
 *
 * @author    Mark Taylor (Starlink)
 * @see  HDSReference
 */
public class NDFNdxBuilder implements NdxBuilder {

    /** Sole instance of the class. */
    private static NDFNdxBuilder instance = new NDFNdxBuilder();

    /**
     * Private sole constructor.
     */
    private NDFNdxBuilder() {}

    /**
     * Returns an NDFNdxBuilder.
     *
     * @return   the sole instance of this class
     */
    public static NDFNdxBuilder getInstance() {
        return instance;
    }

    public Ndx makeNdx( URL url, AccessMode mode ) throws IOException {
        LocalHDS lobj = LocalHDS.getReadableHDS( url );
        if ( lobj == null ) {
            return null;
        }
        HDSReference href = lobj.getHDSReference();
        final File file = href.getContainerFile();
        final boolean isTemp = lobj.isTemporary();

        try {
            /* Construct an NdxImpl which will remove any temporary file
             * when it is finalize with. */
            NdxImpl impl = new NDFNdxImpl( href, mode ) {
                public void finalize() throws Throwable {
                    try {
                        super.finalize();
                    }
                    finally {
                        if ( isTemp ) {
                            file.delete();
                        }
                    }
                }
            };

           /* Return an Ndx. */
           return new BridgeNdx( impl );
        }

        /* Tidy up the temporary file if we failed. */
        catch ( HDSException e ) {
            if ( isTemp ) {
                file.delete();
            }
            throw (IOException) new IOException().initCause( e );
        }
    }

    public boolean createNewNdx( URL url, Ndx orig ) throws IOException {
        try {
            LocalHDS lobj = LocalHDS.getNewHDS( url, "NDF" );
            if ( lobj == null ) {
                return false;
            }
            HDSReference href = lobj.getHDSReference();
            HDSObject place = href.getObject( "WRITE" );
            new NdfMaker().makeNDF( orig, place );
            return true;
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException().initCause( e );
        }
    }
}

