/* ********************************************************
 * This file automatically generated by /d2/starjava/java/source/jniast/src/perl/SlaMap.pl
 * Do not edit
 **********************************************************/

package uk.ac.starlink.ast;


/**
 * Java interface to the AST SlaMap class
 *  - sequence of celestial coordinate conversions. 
 * An SlaMap is a specialised form of Mapping which can be used to
 * represent a sequence of conversions between standard celestial
 * (longitude, latitude) coordinate systems.
 * <p>
 * When an SlaMap is first created, it simply performs a unit
 * (null) Mapping on a pair of coordinates. Using the astSlaAdd
 * function, a series of coordinate conversion steps may then be
 * added, selected from those provided by the SLALIB Positional
 * Astronomy Library (Starlink User Note SUN/67). This allows
 * multi-step conversions between a variety of celestial coordinate
 * systems to be assembled out of the building blocks provided by
 * SLALIB.
 * <p>
 * For details of the individual coordinate conversions available,
 * see the description of the astSlaAdd function.
 * 
 * @see <a href="http://star-www.rl.ac.uk/star/docs/sun67.htx/sun67.html">SUN/67 - SLALIB</a>
 * 
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_SlaMap'>AST SlaMap</a> 
 * @author   Mark Taylor (Starlink) 
 * @version  $Id$
 */
public class SlaMap extends Mapping {

   /**
    * Creates a default SlaMap.
    */
   public SlaMap() {
      construct( 0 );
   }

    /** 
     * Creates a SlaMap.   
     * @param  flags  This parameter is reserved for future use and should currently
     * always be set to zero.
     * 
     * @throws  AstException  if an error occurred in the AST library
    */
    public SlaMap( int flags ) {
        construct( flags );
    }
    private native void construct( int flags );

    /** 
     * Add a celestial coordinate conversion to an SlaMap.   
     * This function adds one of the standard celestial coordinate
     * system conversions provided by the SLALIB Positional Astronomy
     * Library (Starlink User Note SUN/67) to an existing SlaMap.
     * <p>
     * When an SlaMap is first created (using astSlaMap), it simply
     * performs a unit (null) Mapping. By using astSlaAdd (repeatedly
     * if necessary), one or more coordinate conversion steps may then
     * be added, which the SlaMap will perform in sequence. This allows
     * multi-step conversions between a variety of celestial coordinate
     * systems to be assembled out of the building blocks provided by
     * SLALIB.
     * <p>
     * Normally, if an SlaMap's Invert attribute is zero (the default),
     * then its forward transformation is performed by carrying out
     * each of the individual coordinate conversions specified by
     * astSlaAdd in the order given (i.e. with the most recently added
     * conversion applied last).
     * <p>
     * This order is reversed if the SlaMap's Invert attribute is
     * non-zero (or if the inverse transformation is requested by any
     * other means) and each individual coordinate conversion is also
     * replaced by its own inverse. This process inverts the overall
     * effect of the SlaMap. In this case, the first conversion to be
     * applied would be the inverse of the one most recently added.
     * <h4>Notes</h4>
     * <br> - All coordinate values processed by an SlaMap are in
     * radians. The first coordinate is the celestial longitude and the
     * second coordinate is the celestial latitude.
     * <br> - When assembling a multi-stage conversion, it can sometimes be
     * difficult to determine the most economical conversion path. For
     * example, converting to the standard FK5 coordinate system as an
     * intermediate stage is often sensible in formulating the problem,
     * but may introduce unnecessary extra conversion steps. A solution
     * to this is to include all the steps which are (logically)
     * necessary, but then to use astSimplify to simplify the resulting
     * SlaMap. The simplification process will eliminate any steps
     * which turn out not to be needed.
     * <br> - This function does not check to ensure that the sequence of
     * coordinate conversions added to an SlaMap is physically
     * meaningful.
     * <h4>SLALIB Conversions</h4>
     * The following strings (which are case-insensitive) may be supplied
     * via the "cvt" parameter to indicate which celestial coordinate
     * conversion is to be added to the SlaMap. Each string is derived
     * from the name of the SLALIB routine that performs the
     * conversion and the relevant documentation (SUN/67) should be
     * consulted for details.  Where arguments are needed by
     * the conversion, they are listed in parentheses. Values for
     * these arguments should be given, via the "args" array, in the
     * order indicated. The argument names match the corresponding
     * SLALIB routine arguments and their values should be given using
     * exactly the same units, time scale, calendar, etc. as described
     * in SUN/67:
     * <p>
     * <br> - "ADDET" (EQ): Add E-terms of aberration.
     * <br> - "SUBET" (EQ): Subtract E-terms of aberration.
     * <br> - "PREBN" (BEP0,BEP1): Apply Bessel-Newcomb pre-IAU 1976 (FK4)
     * precession model.
     * <br> - "PREC" (EP0,EP1): Apply IAU 1975 (FK5) precession model.
     * <br> - "FK45Z" (BEPOCH): Convert FK4 to FK5 (no proper motion or parallax).
     * <br> - "FK54Z" (BEPOCH): Convert FK5 to FK4 (no proper motion or parallax).
     * <br> - "AMP" (DATE,EQ): Convert geocentric apparent to mean place.
     * <br> - "MAP" (EQ,DATE): Convert mean place to geocentric apparent.
     * <br> - "ECLEQ" (DATE): Convert ecliptic coordinates to J2000.0 equatorial.
     * <br> - "EQECL" (DATE): Convert equatorial J2000.0 to ecliptic coordinates.
     * <br> - "GALEQ": Convert galactic coordinates to J2000.0 equatorial.
     * <br> - "EQGAL": Convert J2000.0 equatorial to galactic coordinates.
     * <br> - "GALSUP": Convert galactic to supergalactic coordinates.
     * <br> - "SUPGAL": Convert supergalactic coordinates to galactic.
     * <p>
     * For example, to use the "ADDET" conversion, which takes a single
     * argument EQ, you should consult the documentation for the SLALIB
     * routine SLA_ADDET. This describes the conversion in detail and
     * shows that EQ is the Besselian epoch of the mean equator and
     * equinox.
     * This value should then be supplied to astSlaAdd in args[0].
     * <p>
     * In addition the following strings may be supplied for more complex
     * conversions which do not correspond to any one single SLALIB routine
     * (DATE is the Modified Julian Date of the observation, and
     * (OBSX,OBSY,OBZ) are the Heliocentric-Aries-Ecliptic cartesian
     * coordinates, in metres, of the observer):
     * <p>
     * <br> - "HPCEQ" (DATE,OBSX,OBSY,OBSZ): Convert Helioprojective-Cartesian coordinates to J2000.0 equatorial.
     * <br> - "EQHPC" (DATE,OBSX,OBSY,OBSZ): Convert J2000.0 equatorial coordinates to Helioprojective-Cartesian.
     * <br> - "HPREQ" (DATE,OBSX,OBSY,OBSZ): Convert Helioprojective-Radial coordinates to J2000.0 equatorial.
     * <br> - "EQHPR" (DATE,OBSX,OBSY,OBSZ): Convert J2000.0 equatorial coordinates to Helioprojective-Radial.
     * <p>
     * 
     * @see <a href="http://star-www.rl.ac.uk/star/docs/sun67.htx/sun67.html">SUN/67 - SLALIB</a>
     * @param   cvt
     * Pointer to a null-terminated string which identifies the
     * celestial coordinate conversion to be added to the
     * SlaMap. See the "SLALIB Conversions" section for details of
     * those available.
     * 
     * @param   args
     * An array containing argument values for the celestial
     * coordinate conversion. The number of arguments required, and
     * hence the number of array elements used, depends on the
     * conversion specified (see the "SLALIB Conversions"
     * section). This array is ignored
     * and a NULL pointer may be supplied
     * if no arguments are needed.
     * 
     * @throws  AstException  if an error occurred in the AST library
     */
    public native void add( String cvt, double[] args );

}
