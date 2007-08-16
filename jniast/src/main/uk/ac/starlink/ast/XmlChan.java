/* ********************************************************
 * This file automatically generated by XmlChan.pl.
 *                   Do not edit.                         *
 **********************************************************/

package uk.ac.starlink.ast;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Java interface to the AST XmlChan class
 *  - I/O Channel using XML to represent Objects. 
 * A XmlChan is a specialised form of Channel which supports XML I/O
 * operations. Writing an Object to an XmlChan (using
 * astWrite) will, if the Object is suitable, generate an
 * XML description of that Object, and reading from an XmlChan will 
 * create a new Object from its XML description.
 * <p>
 * Normally, when you use an XmlChan, you should provide "source"
 * and "sink" functions which connect it to an external data store
 * by reading and writing the resulting XML text. These functions
 * should perform any conversions needed between external character 
 * encodings and the internal ASCII encoding. If no such functions 
 * are supplied, a Channel will read from standard input and write 
 * to standard output.
 * 
 * 
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_XmlChan'>AST XmlChan</a>  
 */
public class XmlChan extends Channel {

    /** XML namespace for elements in AstObject serialization. */
    public static final String AST__XMLNS = getAstConstantC( "AST__XMLNS" );

    /**
     * Creates a channel which reads from the given <code>InputStream</code>
     * and writes to the given <code>OutputStream</code>.
     *
     * @param   in   a stream to read AST objects from.  If <code>null</code>,
     *               then <code>System.in</code> is used.
     * @param   out  a stream to write AST objects to.  If <code>null</code>,
     *               then <code>System.out</code> is used.
     */
    public XmlChan( InputStream in, OutputStream out ) {
        super( in, out );
    }

    /**
     * This constructor does not do all the required construction to
     * create a valid XmlChan object, but is required for inheritance
     * by user subclasses of XmlChan.
     */
    protected XmlChan() {
        super();
    }

    /**
     * Get 
     * system for formatting Objects as XML.  
     * This attribute specifies the formatting system to use when AST
     * Objects are written out as XML through an XmlChan. It
     * affects the behaviour of the astWrite function when
     * they are used to transfer any AST Object to or from an external
     * XML representation.
     * <p>
     * The XmlChan class allows AST objects to be represented in the form
     * of XML in several ways (conventions) and the XmlFormat attribute is 
     * used to specify which of these should be used. The formatting options 
     * available are outlined in the "Formats Available" section below.
     * <p>
     * By default, an XmlChan will attempt to determine which format system
     * is already in use, and will set the default XmlFormat value
     * accordingly (so that subsequent I/O operations adopt the same
     * conventions). It does this by looking for certain critical items
     * which only occur in particular formats. For details of how this
     * works, see the "Choice of Default Format" section below. If you wish
     * to ensure that a particular format system is used, independently of
     * any XML already read, you should set an explicit XmlFormat value
     * yourself.
     * <h4>Formats Available</h4>
     * The XmlFormat attribute can take any of the following (case
     * insensitive) string values to select the corresponding formatting
     * system:
     * <p>
     * <br> - "NATIVE": This is a direct conversion to XML of the heirarchical
     * format used by a standard XML channel (and also by the NATIVE
     * encoding of a FitsChan).
     * <p>
     * <br> - "QUOTED": This is the same as NATIVE format except that extra
     * information is included which allows client code to convert the
     * XML into a form which can be read by a standard AST Channel. This
     * extra information indicates which AST attribute values should be
     * enclosed in quotes before being passed to a Channel. 
     * <p>
     * <br> - "IVOA": This is an experimental format which uses XML schemas
     * being developed by the International Virtual Observatory Alliance
     * (IVOA - see "http://www.ivoa.net/") to describe coordinate systems, 
     * regions, mappings, etc. Current support is limited to the draft STC
     * schema described at "http://hea-www.harvard.edu/~arots/nvometa/STC.html".
     * Note, this format is under active development and is consequently
     * liable to change. The current implementation reflects version 1.2
     * of the draft STC document.
     * <h4>The IVOA Format</h4>
     * The IVOA format should be considered experimental. It currently
     * caters only for certain parts of V1.20 of the the draft Space-Time 
     * Coordinate (STC) schema (see http://hea-www.harvard.edu/~arots/nvometa/STC.ht$
     * The following points should be noted when using an XmlChan to read
     * or write STC information (note, this list is currently incomplete):
     * <p>
     * <br> - Objects can currently only be read using this format, not written.
     * <br> - The AST object generated by reading an <STCMetadata> element will 
     * be an instance of one of the AST "Stc" classes: StcResourceProfile, 
     * StcSearchLocation, StcCatalogEntryLocation, StcObsDataLocation.
     * <br> - When reading an <STCMetadata> element, the axes in the returned 
     * AST Object will be in the order space, time, spectral, redshift,
     * irrespective of the order in which the axes occur in the <STCMetadata>
     * element. If the supplied <STCMetadata> element does not contain all of 
     * these axes, the returned AST Object will also omit them, but the 
     * ordering of those axes which are present will be as stated above. If 
     * the spatial frame represents a celestial coordinate system the 
     * spatial axes will be in the order (longitude, latitude). 
     * <br> - Until such time as the AST TimeFrame is complete, a simple
     * 1-dimensional Frame (with Domain set to TIME) will be used to 
     * represent the STC <TimeFrame> element. Consequently, most of the
     * information within a <TimeFrame> element is currently ignored.
     * <br> - <SpaceFrame> elements can only be read if they describe a celestial 
     * longitude and latitude axes supported by the AST SkyFrame class. The 
     * space axes will be returned in the order (longitude, latitude).
     * <br> - Velocities associated with SpaceFrames cannot be read.
     * <br> - Any <GenericCoordFrame> elements within an <AstroCoordSystem> element
     * are currently ignored.
     * <br> - Any second or subsequent <AstroCoordSystem> found within an
     * STCMetaData element is ignored.
     * <br> - Any second or subsequent <AstroCoordArea> found within an
     * STCMetaData element is ignored.
     * <br> - Any <OffsetCenter> found within a <SpaceFrame> is ignored.
     * <br> - Any CoordFlavor element found within a <SpaceFrame> is ignored.
     * <br> - <SpaceFrame> elements can only be read if they refer to 
     * one of the following space reference frames: ICRS, GALACTIC_II,
     * SUPER_GALACTIC, HEE, FK4, FK5, ECLIPTIC.
     * <br> - <SpaceFrame> elements can only be read if the reference 
     * position is TOPOCENTER. Also, any planetary ephemeris is ignored.
     * <br> - Regions: there is currently no support for STC regions of type
     * Sector, ConvexHull or SkyIndex.
     * <br> - The AST Region read from a CoordInterval element is considered to
     * be open if either the lo_include or the hi_include attribute is
     * set to false.
     * <br> - <RegionFile> elements are not supported.
     * <br> - Vertices within <Polygon> elements are always considered to be
     * joined using great circles (that is, <SmallCircle> elements are
     * ignored).
     * 
     *
     * @return  this object's XmlFormat attribute
     */
    public String getXmlFormat() {
        return getC( "XmlFormat" );
    }

    /**
     * Set 
     * system for formatting Objects as XML.  
     * This attribute specifies the formatting system to use when AST
     * Objects are written out as XML through an XmlChan. It
     * affects the behaviour of the astWrite function when
     * they are used to transfer any AST Object to or from an external
     * XML representation.
     * <p>
     * The XmlChan class allows AST objects to be represented in the form
     * of XML in several ways (conventions) and the XmlFormat attribute is 
     * used to specify which of these should be used. The formatting options 
     * available are outlined in the "Formats Available" section below.
     * <p>
     * By default, an XmlChan will attempt to determine which format system
     * is already in use, and will set the default XmlFormat value
     * accordingly (so that subsequent I/O operations adopt the same
     * conventions). It does this by looking for certain critical items
     * which only occur in particular formats. For details of how this
     * works, see the "Choice of Default Format" section below. If you wish
     * to ensure that a particular format system is used, independently of
     * any XML already read, you should set an explicit XmlFormat value
     * yourself.
     * <h4>Formats Available</h4>
     * The XmlFormat attribute can take any of the following (case
     * insensitive) string values to select the corresponding formatting
     * system:
     * <p>
     * <br> - "NATIVE": This is a direct conversion to XML of the heirarchical
     * format used by a standard XML channel (and also by the NATIVE
     * encoding of a FitsChan).
     * <p>
     * <br> - "QUOTED": This is the same as NATIVE format except that extra
     * information is included which allows client code to convert the
     * XML into a form which can be read by a standard AST Channel. This
     * extra information indicates which AST attribute values should be
     * enclosed in quotes before being passed to a Channel. 
     * <p>
     * <br> - "IVOA": This is an experimental format which uses XML schemas
     * being developed by the International Virtual Observatory Alliance
     * (IVOA - see "http://www.ivoa.net/") to describe coordinate systems, 
     * regions, mappings, etc. Current support is limited to the draft STC
     * schema described at "http://hea-www.harvard.edu/~arots/nvometa/STC.html".
     * Note, this format is under active development and is consequently
     * liable to change. The current implementation reflects version 1.2
     * of the draft STC document.
     * <h4>The IVOA Format</h4>
     * The IVOA format should be considered experimental. It currently
     * caters only for certain parts of V1.20 of the the draft Space-Time 
     * Coordinate (STC) schema (see http://hea-www.harvard.edu/~arots/nvometa/STC.ht$
     * The following points should be noted when using an XmlChan to read
     * or write STC information (note, this list is currently incomplete):
     * <p>
     * <br> - Objects can currently only be read using this format, not written.
     * <br> - The AST object generated by reading an <STCMetadata> element will 
     * be an instance of one of the AST "Stc" classes: StcResourceProfile, 
     * StcSearchLocation, StcCatalogEntryLocation, StcObsDataLocation.
     * <br> - When reading an <STCMetadata> element, the axes in the returned 
     * AST Object will be in the order space, time, spectral, redshift,
     * irrespective of the order in which the axes occur in the <STCMetadata>
     * element. If the supplied <STCMetadata> element does not contain all of 
     * these axes, the returned AST Object will also omit them, but the 
     * ordering of those axes which are present will be as stated above. If 
     * the spatial frame represents a celestial coordinate system the 
     * spatial axes will be in the order (longitude, latitude). 
     * <br> - Until such time as the AST TimeFrame is complete, a simple
     * 1-dimensional Frame (with Domain set to TIME) will be used to 
     * represent the STC <TimeFrame> element. Consequently, most of the
     * information within a <TimeFrame> element is currently ignored.
     * <br> - <SpaceFrame> elements can only be read if they describe a celestial 
     * longitude and latitude axes supported by the AST SkyFrame class. The 
     * space axes will be returned in the order (longitude, latitude).
     * <br> - Velocities associated with SpaceFrames cannot be read.
     * <br> - Any <GenericCoordFrame> elements within an <AstroCoordSystem> element
     * are currently ignored.
     * <br> - Any second or subsequent <AstroCoordSystem> found within an
     * STCMetaData element is ignored.
     * <br> - Any second or subsequent <AstroCoordArea> found within an
     * STCMetaData element is ignored.
     * <br> - Any <OffsetCenter> found within a <SpaceFrame> is ignored.
     * <br> - Any CoordFlavor element found within a <SpaceFrame> is ignored.
     * <br> - <SpaceFrame> elements can only be read if they refer to 
     * one of the following space reference frames: ICRS, GALACTIC_II,
     * SUPER_GALACTIC, HEE, FK4, FK5, ECLIPTIC.
     * <br> - <SpaceFrame> elements can only be read if the reference 
     * position is TOPOCENTER. Also, any planetary ephemeris is ignored.
     * <br> - Regions: there is currently no support for STC regions of type
     * Sector, ConvexHull or SkyIndex.
     * <br> - The AST Region read from a CoordInterval element is considered to
     * be open if either the lo_include or the hi_include attribute is
     * set to false.
     * <br> - <RegionFile> elements are not supported.
     * <br> - Vertices within <Polygon> elements are always considered to be
     * joined using great circles (that is, <SmallCircle> elements are
     * ignored).
     * 
     *
     * @param  xmlFormat   the XmlFormat attribute of this object
     */
    public void setXmlFormat( String xmlFormat ) {
       setC( "XmlFormat", xmlFormat );
    }

    /**
     * Get 
     * controls output of indentation and line feeds.  
     * This attribute controls the appearance of the XML produced when an
     * AST object is written to an XmlChan. If it is non-zero, then extra
     * linefeed characters will be inserted as necessary to ensure that each 
     * XML tag starts on a new line, and each tag will be indented to show 
     * its depth in the containment hierarchy. If XmlIndent is zero (the
     * default), then no linefeeds or indentation strings will be added to
     * output text.
     * 
     *
     * @return  this object's XmlIndent attribute
     */
    public boolean getXmlIndent() {
        return getB( "XmlIndent" );
    }

    /**
     * Set 
     * controls output of indentation and line feeds.  
     * This attribute controls the appearance of the XML produced when an
     * AST object is written to an XmlChan. If it is non-zero, then extra
     * linefeed characters will be inserted as necessary to ensure that each 
     * XML tag starts on a new line, and each tag will be indented to show 
     * its depth in the containment hierarchy. If XmlIndent is zero (the
     * default), then no linefeeds or indentation strings will be added to
     * output text.
     * 
     *
     * @param  xmlIndent   the XmlIndent attribute of this object
     */
    public void setXmlIndent( boolean xmlIndent ) {
       setB( "XmlIndent", xmlIndent );
    }

    /**
     * Get 
     * controls output buffer length.  
     * This attribute specifies the maximum length to use when writing out 
     * text through the sink function supplied when the XmlChan was created.
     * <p>
     * The number of characters in each string written out through the sink 
     * function will not be greater than the value of this attribute (but
     * may be less). A value of zero (the default) means there is no limit - 
     * each string can be of any length.
     * <p>
     * 
     *
     * @return  this object's XmlLength attribute
     */
    public int getXmlLength() {
        return getI( "XmlLength" );
    }

    /**
     * Set 
     * controls output buffer length.  
     * This attribute specifies the maximum length to use when writing out 
     * text through the sink function supplied when the XmlChan was created.
     * <p>
     * The number of characters in each string written out through the sink 
     * function will not be greater than the value of this attribute (but
     * may be less). A value of zero (the default) means there is no limit - 
     * each string can be of any length.
     * <p>
     * 
     *
     * @param  xmlLength   the XmlLength attribute of this object
     */
    public void setXmlLength( int xmlLength ) {
       setI( "XmlLength", xmlLength );
    }

    /**
     * Get 
     * the namespace prefix to use when writing.  
     * This attribute is a string which is to be used as the namespace
     * prefix for all XML elements created as a result of writing an AST
     * Object out through an XmlChan. The URI associated with the namespace
     * prefix is given by the symbolic constant AST__XMLNS defined in 
     * ast.h.
     * A definition of the namespace prefix is included in each top-level
     * element produced by the XmlChan.
     * <p>
     * The default value is a blank string which causes no prefix to be
     * used. In this case each top-level element will set the default 
     * namespace to be the value of AST__XMLNS.
     * 
     *
     * @return  this object's XmlPrefix attribute
     */
    public String getXmlPrefix() {
        return getC( "XmlPrefix" );
    }

    /**
     * Set 
     * the namespace prefix to use when writing.  
     * This attribute is a string which is to be used as the namespace
     * prefix for all XML elements created as a result of writing an AST
     * Object out through an XmlChan. The URI associated with the namespace
     * prefix is given by the symbolic constant AST__XMLNS defined in 
     * ast.h.
     * A definition of the namespace prefix is included in each top-level
     * element produced by the XmlChan.
     * <p>
     * The default value is a blank string which causes no prefix to be
     * used. In this case each top-level element will set the default 
     * namespace to be the value of AST__XMLNS.
     * 
     *
     * @param  xmlPrefix   the XmlPrefix attribute of this object
     */
    public void setXmlPrefix( String xmlPrefix ) {
       setC( "XmlPrefix", xmlPrefix );
    }

}