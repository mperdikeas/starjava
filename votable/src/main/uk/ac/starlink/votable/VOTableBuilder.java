package uk.ac.starlink.votable;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.SourceReader;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which 
 * gets <tt>StarTable</tt>s from VOTable documents.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableBuilder implements TableBuilder {

    private static Pattern htmlPattern = 
        Pattern.compile( "<x?html", Pattern.CASE_INSENSITIVE );

    /**
     * Makes a StarTable out of a DataSource which points to a VOTable.
     * If the source has a position attribute, it is currently 
     * interpreted as an index into a breadth-first list of the TABLE
     * elements in the document pointed to by <tt>datsrc</tt>,
     * thus it must be a non-negative integer less than the number of
     * TABLE elements.  If it has no position attribute, the first
     * TABLE element is used.  The interpretation of the position
     * should probably change or be extended in the future to 
     * allow XPath expressions.
     *
     * @param  datsrc  the location of the VOTable document to use
     */
    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom )
            throws IOException {

        /* Check if the source looks like HTML.  If it does it is almost
         * certainly not going to represent a valid VOTable, and trying
         * to process it will be slow, since the parser may take some
         * time to work out that it's not XML we are seeing.  
         * In this case bail out. */
        String sintro = new String( datsrc.getIntro() );
        if ( htmlPattern.matcher( sintro ).lookingAt() ) {
            return null;
        }

        /* Try to get a VOTable object from this source. */
        VOElement votable;
        try {
            votable = VOElementFactory.makeVOElement( datsrc );
        }

        /* If we have got an Exception it's probably because 
         * it wasn't XML.  Return null to indicate it wasn't our kind
         * of input. */
        catch ( SAXException e ) {
            return null;
        }
        catch ( IOException e ) {
            return null;
        }

        /* If we have got an IllegalArgumentException it probably wasn't
         * a VOTable. */
        catch ( IllegalArgumentException e ) {
            return null;
        }

        /* If the datasource has a position, try to use this to identify
         * the actual table we're after in the document. 
         * For now, this is just an integer indicating which table in 
         * breadth-order we want.  In due course it ought to be an XPath
         * probably. */
        int itab = 0;
        String pos = datsrc.getPosition();
        if ( pos != null ) {
            try {
                itab = Integer.parseInt( pos );
            }
            catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( 
                    "Expecting integer for position in " +
                    datsrc + "(" +  e + ")" );
            }
            if ( itab < 0 ) {
                throw new IllegalArgumentException(
                    "Expecting integer >= 0 for position in " + 
                    datsrc + "(got " + itab + ")" );
            }
        }

        /* Find the first TABLE element within the VOTable.  This is a
         * short-term measure - in due course there should be some way
         * (XPath) of indicating which TABLE we want to look at. */
        TableElement tableEl = findTableElement( votable, itab );

        /* If it's null, then we haven't found one. */
        if ( tableEl == null ) {
            if ( itab == 0 ) {
                throw new IOException( 
                    "VOTable document contained no TABLE elements" );
            }
            else {
                throw new IOException(
                    "VOTable document contained less than " + ( itab + 1 ) +
                    " tables" );
            }
        }

        /* Adapt the TABLE element to a StarTable. */
        return new VOStarTable( tableEl );
    }

    /**
     * Returns <tt>true</tt> for flavors which have MIME types starting
     * <ul>
     * <li>text/xml
     * <li>application/xml
     * <li>application/x-votable+xml
     * </ul>
     */
    public boolean canImport( DataFlavor flavor ) {
        String pri = flavor.getPrimaryType();
        String sub = flavor.getSubType();
        if ( pri.equals( "text" ) && sub.equals( "xml" ) ||
             pri.equals( "application" ) && sub.equals( "xml" ) ||
             pri.equals( "application" ) && sub.equals( "x-votable+xml" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Acquires the data from a single TABLE element in a VOTable document,
     * writing the result to a sink.  This can be used if only one-shot
     * access to the data is required.  
     * Invocation of this method should be cheap on memory even
     * for large XML documents and/or tables.
     *
     * @param  istrm  stream from which the VOTable document will be supplied
     * @param  sink   callback interface into which the table metadata and
     *                data will be dumped
     * @param  index  if present, a string representation of the index of
     *                the table in the document to be read - "0" means the
     *                first one encountered, "1" means the second, etc
     */
    public void copyStarTable( InputStream istrm, TableSink sink, 
                               String index ) throws IOException {
        int itable = index.matches( "[0-9]+" ) ? Integer.parseInt( index ) : 0;
        try {
            TableCopier.copyStarTable( new InputSource( istrm ), sink, itable );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Performs a (breadth-first) search to locate any descendents of the
     * given VOElement which are Table elements.
     *
     * @param  voEl  a starting element
     * @param  index  the index of the required table (0 is first)
     * @return  a Table element which is the <tt>index</tt>'th TABLE 
     *          descendent of <tt>vosrc</tt>,
     *          or <tt>null</tt> if there isn't one
     */
    private static TableElement findTableElement( VOElement voEl, int index ) {
        VOElement[] tables = voEl.getDescendantsByName( "TABLE" );
        return index < tables.length ? (TableElement) tables[ index ] : null;
    }
}
