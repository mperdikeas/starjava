/* ********************************************************
 * This file automatically generated by /d2/starjava/java/source/jniast/src/perl/WinMap.pl
 * Do not edit
 **********************************************************/

package uk.ac.starlink.ast;


/**
 * Java interface to the AST WinMap class
 *  - map one window on to another by scaling and shifting each axis. 
 * A Winmap is a linear Mapping which transforms a rectangular
 * window in one coordinate system into a similar window in another
 * coordinate system by scaling and shifting each axis (the window
 * edges being parallel to the coordinate axes).
 * <p>
 * A WinMap is specified by giving the coordinates of two opposite
 * corners (A and B) of the window in both the input and output
 * coordinate systems.
 * 
 * 
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_WinMap'>AST WinMap</a> 
 * @author   Mark Taylor (Starlink) 
 * @version  $Id$
 */
public class WinMap extends Mapping {
    /** 
     * Creates a WinMap.   
     * @param  ncoord  The number of coordinate values for each point to be
     * transformed (i.e. the number of dimensions of the space in
     * which the points will reside). The same number is applicable
     * to both input and output points.
     * 
     * @param  ina  An array containing the "ncoord"
     * coordinates of corner A of the window in the input coordinate 
     * system.
     * 
     * @param  inb  An array containing the "ncoord"
     * coordinates of corner B of the window in the input coordinate 
     * system.
     * 
     * @param  outa  An array containing the "ncoord"
     * coordinates of corner A of the window in the output coordinate 
     * system.
     * 
     * @param  outb  An array containing the "ncoord"
     * coordinates of corner B of the window in the output coordinate 
     * system.
     * 
     * @throws  AstException  if an error occurred in the AST library
    */
    public WinMap( int ncoord, double[] ina, double[] inb, double[] outa, double[] outb ) {
        construct( ncoord, ina, inb, outa, outb );
    }
    private native void construct( int ncoord, double[] ina, double[] inb, double[] outa, double[] outb );

}
