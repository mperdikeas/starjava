/* ********************************************************
 * This file automatically generated by AstObject.pl.
 *                   Do not edit.                         *
 **********************************************************/

package uk.ac.starlink.ast;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.util.Loader;


/**
 * Java interface to the AST Object class
 *  - base class for all AST Objects. 
 * This class is the base class from which all other classes in the
 * AST library are derived. It provides all the basic Object
 * behaviour and Object manipulation facilities required throughout
 * the library. There is no Object constructor, however, as Objects
 * on their own are not useful.
 * 
 * 
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_Object'>AST Object</a>  
 */
public class AstObject {

    /** Holds the C pointer to the AST object.  Used by native code. */
    protected long pointer = 0;

    /** Library version numbers. */
    static int[] AST_VERSION;
    static int[] JNIAST_NATIVE_VERSION;
    static int[] JNIAST_JAVA_VERSION;

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.ast" );

    /* This call performs the native static initialisation required before
     * any of the AST native methods can work.  Since all the AST objects
     * are subclassed from this one, putting it in the static initialisation
     * of this class guarantees it will be done before any other native
     * methods are called. */
    static {
        Loader.loadLibrary( "jniast" );
        nativeInitialize();

        /* Set and check consistency of component versions. */
        AST_VERSION = new int[] { 
            getAstConstantI( "AST_MAJOR_VERS" ),
            getAstConstantI( "AST_MINOR_VERS" ),
            getAstConstantI( "AST_RELEASE" ),
        };
        JNIAST_NATIVE_VERSION = new int[] { 
            getAstConstantI( "JNIAST_MAJOR_VERS" ),
            getAstConstantI( "JNIAST_MINOR_VERS" ),
            getAstConstantI( "JNIAST_RELEASE" ),
        };
        JNIAST_JAVA_VERSION = new int[] {
            4,
            0,
            1,
        };

        /* Check that the versions look consistent. */
        if ( ! versionGreaterEqual( AST_VERSION,
                                    JNIAST_NATIVE_VERSION ) ||
             ! versionGreaterEqual( JNIAST_NATIVE_VERSION, 
                                    JNIAST_JAVA_VERSION ) ) {
            logger.warning( "Inconsistent component versions: "
                          + reportVersions() );
        }
    }

    /** Bad coordinate value. */
    public static final double AST__BAD = getAstConstantD( "AST__BAD" );

    /**
     * Dummy constructor.  This constructor does not create a valid
     * AstObject object, but is required for inheritance by AstObject's
     * subclasses.
     */
    protected AstObject() {
    }

    /**
     * Finalize the object; this annuls the AST object to free resources.
     * Subclasses which override this method should call it in their
     * finalizers.
     */
    protected void finalize() throws Throwable {
        if ( pointer != 0 ) {
            annul();
        }
        pointer = 0;
        super.finalize();
    }

    /* Initializer for shared library. */
    private native static void nativeInitialize();

    /**
     * Gets the value of a named integer constant from the underlying
     * AST library.
     *
     * @param  constname  the name of the constant ("AST__<i>something</i>")
     * @return the value of <code>constname</code>
     * @throws IllegalArgumentException  if no constant by that name exists
     */
    public static native int getAstConstantI( String constname );

    /**
     * Gets the value of a named double precision constant from the underlying
     * AST library.
     *
     * @param  constname  the name of the constant ("AST__<i>something</i>")
     * @return the value of <code>constname</code>
     * @throws IllegalArgumentException  if no constant by that name exists
     */
    static native double getAstConstantD( String constname );

    /**
     * Gets the value of a named character constant from the underlying
     * AST library.
     *
     * @param  constname  the name of the constant ("AST__<i>something</i>")
     * @return  the value of <code>constname</code>
     * @throws  IllegalArgumentException  if no constant by that name exists
     */
    static native String getAstConstantC( String constname );

    /**
     * Annul this object.  Associated resources in the underlying library
     * are reclaimed.  Following this call the object cannot be used.
     * User code should not normally call this, but should dereference
     * the object instead (the <code>finalize</code> method calls
     * <code>annul</code>.
     */
    public native void annul();

    /**
     * Delete this object.  Associated resources in the underlying library
     * are reclaimed, and <i>any</i> remaining references to the
     * underlying object are rendered invalid.  
     * <p>
     * Note that deletion is unconditional, regardless of other references
     * to this java object or to the underlying AST object.  This method
     * should be used with caution.
     */
    public native void delete();

    /**
     * Clone an object.
     * This method creates a new Object which is a reference to the 
     * same underlying AST object.  This method calls the astClone
     * function of the underlying AST library and makes a new AstObject
     * referencing the result.
     *
     * @return  a new <code>AstObject</code> containing a reference to the 
     *          underlying AST object referenced by this <code>AstObject</code>
     * @see  AstObject#sameObject(Object) sameObject
     */
    public native Object clone();

    /**
     * Determine whether two AstObjects are similar in all respects.
     * This method is implemented by writing both objects to a 
     * {@link Channel} and comparing the resulting textual representations.
     * It may therefore be relatively expensive.
     *
     * @param   obj  object to be compared with this one
     * @return       true if <code>obj</code> resembles this object in all
     *               respects, false otherwise
     * @see  AstObject#sameObject(Object) sameObject
     */
    public boolean equals( Object obj ) {
        boolean eq = false;
        if ( obj.getClass().equals( getClass() ) ) {
            AstStringer stringer = new AstStringer();
            String s1 = stringer.representation( this );
            String s2 = stringer.representation( (AstObject) obj );
            eq = ( s1 != null ) && ( s2 != null ) && s1.equals( s2 );
        } 
        return eq;
    }

    /**
     * Return a hash code for this AstObject.  The notion of equality
     * in this method must match the one in the equals method.
     * This method is implemented by writing the object to a 
     * {@link Channel} and calculating the hash code of the resulting
     * String.  It may therefore be relatively expensive.
     *
     * @return  the hash code of this AstObject
     */
    public int hashCode() {
        String s = new AstStringer().representation( this );
        if ( s != null ) {
            return s.hashCode();
        }

        /* There shouldn't be an error, but if there is just use the 
         * same hashcode implementation used by Object. */
        else {
            return System.identityHashCode( this );
        }
    }

    /**
     * Determine whether two AstObjects are references to the same 
     * underlying object.  Since AstObjects are merely containers for
     * references to objects in the underlying AST library, 
     * two AstObjects may effectively be references to the same object
     * (so that, for instance, changing an attribute of one will change
     * it in the other) but the <code>==</code> operator applied between
     * them will return false.  This method can tell you whether two
     * AstObjects refer to the same thing.
     *
     * @param  obj   an object to be compared to this one.
     * @return       true if <code>obj</code> references the same AST object
     *               as this does.
     * @see          AstObject#equals(Object) equals
     */
    public boolean sameObject( Object obj ) {
        boolean same = false;
        if ( obj instanceof AstObject ) {
            AstObject aobj = (AstObject) obj;
            String oldID = getID();
            if ( oldID.equals( aobj.getID() ) ) {
                String newID = oldID + "x";
                setID( newID );
                if ( newID.equals( aobj.getID() ) ) {
                    same = true;
                }
                if ( oldID.length() == 0 ) {
                    clear( "ID" );
                }
                else {
                    setID( oldID );
                }
            }
        }
        return same;
    }

    /* Local class used by the equals and hashCode methods. */
    private class AstStringer extends Channel {
        StringBuffer buf = new StringBuffer();
        {
            setFull( -1 );
            setComment( false );
        }
        protected void sink( String line ) {
            buf.append( line + "\n" );
        }
        synchronized String representation( AstObject ao ) {
            try {
                write( ao );
                String result = buf.toString();
                buf.setLength( 0 );
                return result;
            }
            catch ( IOException e ) {
                return null;
            }
        }
    }

    /**
     * Returns a string giving the versions of AST and of JNIAST.
     *
     * @return  versions of the components of this package
     */
    public static String reportVersions() {
        return new StringBuffer()
            .append( "AST " )
            .append( reportVersion( AST_VERSION ) )
            .append( "; " )
            .append( "JNIAST native " )
            .append( reportVersion( JNIAST_NATIVE_VERSION ) )
            .append( "; " )
            .append( "JNIAST java " )
            .append( reportVersion( JNIAST_JAVA_VERSION ) )
            .toString();
    }

    /**
     * Turns a 3-element version identifier into a human-readable string.
     *
     * @return  version string of the form  Vmajor.minor-relase
     */
    private static String reportVersion( int[] vers ) {
        return new StringBuffer()
            .append( 'V' )
            .append( vers[ 0 ] )
            .append( '.' )
            .append( vers[ 1 ] )
            .append( '-' )
            .append( vers[ 2 ] )
            .toString();
    }

    /**
     * Compares two versions indicators.
     *
     * @param  vers1 first version indicator as (major,minor,release)
     * @param  vers2 second version indicator as (major,minor,release)
     * @return <tt>true</tt> iff <tt>vers1</tt> represents a later 
     *         or equal version than <tt>vers2</tt>
     */
    private static boolean versionGreaterEqual( int[] vers1, int[] vers2 ) {
        if ( vers1[ 0 ] == vers2[ 0 ] ) {
            if ( vers1[ 1 ] == vers2[ 1 ] ) {
                if ( vers1[ 2 ] == vers2[ 2 ] ) {
                    return true;
                }
                else {
                    return vers1[ 2 ] > vers2[ 2 ];
                }
            }
            else {
                return vers1[ 1 ] > vers2[ 1 ];
            }
        }
        else {
            return vers1[ 0 ] > vers2[ 0 ];
        }
    }

    /** 
     * Clear attribute values for an Object.   
     * This function clears the values of a specified set of attributes
     * for an Object. Clearing an attribute cancels any value that has
     * previously been explicitly set for it, so that the standard
     * default attribute value will subsequently be used instead. This
     * also causes the astTest function to return the value zero for
     * the attribute, indicating that no value has been set.
     * <h4>Notes</h4>
     * <br> - Attribute names are not case sensitive and may be surrounded
     * by white space.
     * <br> - It does no harm to clear an attribute whose value has not been
     * set.
     * <br> - An error will result if an attempt is made to clear the value
     * of a read-only attribute.
     * @param   attrib
     * Pointer to a null-terminated character string containing a
     * comma-separated list of the names of the attributes to be cleared.
     * 
     * @throws  AstException  if an error occurred in the AST library
     */
    public native void clear( String attrib );

    /** 
     * Copy an Object.   
     * This function creates a copy of an Object and returns a pointer
     * to the resulting new Object. It makes a "deep" copy, which
     * contains no references to any other Object (i.e. if the original
     * Object contains references to other Objects, then the actual
     * data are copied, not simply the references). This means that
     * modifications may safely be made to the copy without indirectly
     * affecting any other Object.
     * <h4>Notes</h4>
     * <br> - A null Object pointer (AST__NULL) will be returned if this
     * function is invoked with the AST error status set, or if it
     * should fail for any reason.
     * @return  Pointer to the new Object.
     * 
     * @throws  AstException  if an error occurred in the AST library
     */
    public native AstObject copy(  );

    /**
     * Get a character attribute value by name.
     *
     * @param  attrib  the name of the character attribute to retrieve
     * @return the named attribute as a <code>String</code>
     * @throws AstException  if the AST routine generates an error, in 
     *                       particular if no character 
     *                       attribute called <code>attrib</code> exists
     */
    public native String getC( String attrib );

    /**
     * Set a character attribute value by name.
     *
     * @param  attrib the name of the character attribute to set
     * @param  value  the new value of the attribute
     * @throws AstException  if the AST routine generates an error, 
     *                       in particular if no writable character
     *                       attribute called <code>attrib</code> exists
     */
    public native void setC( String attrib, String value );

    /**
     * Get a double precision attribute value by name.
     *
     * @param  attrib  the name of the double precision attribute to retrieve
     * @return the named attribute as a <code>double</code>
     * @throws AstException  if the AST routine generates an error, in 
     *                       particular if no double precision 
     *                       attribute called <code>attrib</code> exists
     */
    public native double getD( String attrib );

    /**
     * Set a double precision attribute value by name.
     *
     * @param  attrib the name of the double precision attribute to set
     * @param  value  the new value of the attribute
     * @throws AstException  if the AST routine generates an error, 
     *                       in particular if no writable double precision
     *                       attribute called <code>attrib</code> exists
     */
    public native void setD( String attrib, double value );

    /**
     * Get a floating point attribute value by name.
     *
     * @param  attrib  the name of the floating point attribute to retrieve
     * @return the named attribute as a <code>float</code>
     * @throws AstException  if the AST routine generates an error, in 
     *                       particular if no floating point 
     *                       attribute called <code>attrib</code> exists
     */
    public native float getF( String attrib );

    /**
     * Set a floating point attribute value by name.
     *
     * @param  attrib the name of the floating point attribute to set
     * @param  value  the new value of the attribute
     * @throws AstException  if the AST routine generates an error, 
     *                       in particular if no writable floating point
     *                       attribute called <code>attrib</code> exists
     */
    public native void setF( String attrib, float value );

    /**
     * Get a long integer attribute value by name.
     *
     * @param  attrib  the name of the long integer attribute to retrieve
     * @return the named attribute as a <code>long</code>
     * @throws AstException  if the AST routine generates an error, in 
     *                       particular if no long integer 
     *                       attribute called <code>attrib</code> exists
     */
    public native long getL( String attrib );

    /**
     * Set a long integer attribute value by name.
     *
     * @param  attrib the name of the long integer attribute to set
     * @param  value  the new value of the attribute
     * @throws AstException  if the AST routine generates an error, 
     *                       in particular if no writable long integer
     *                       attribute called <code>attrib</code> exists
     */
    public native void setL( String attrib, long value );

    /**
     * Get a integer attribute value by name.
     *
     * @param  attrib  the name of the integer attribute to retrieve
     * @return the named attribute as a <code>int</code>
     * @throws AstException  if the AST routine generates an error, in 
     *                       particular if no integer 
     *                       attribute called <code>attrib</code> exists
     */
    public native int getI( String attrib );

    /**
     * Set a integer attribute value by name.
     *
     * @param  attrib the name of the integer attribute to set
     * @param  value  the new value of the attribute
     * @throws AstException  if the AST routine generates an error, 
     *                       in particular if no writable integer
     *                       attribute called <code>attrib</code> exists
     */
    public native void setI( String attrib, int value );

    /**
     * Get a boolean attribute value by name.  This is a convenience
     * method which calls <code>getI</code> but maps integers to 
     * booleans.
     *
     * @param   attrib     the name of the boolean attribute to retrieve
     * @return  the named attribute as a <code>boolean</code>
     * @throws  AstException  if the AST routine generates an error,
     *                        in particular if no integer attribute by
     *                        this name exists
     */
    public boolean getB( String attrib ) {
        return ( getI( attrib ) != 0 );
    }

    /**
     * Set a boolean attribute value by name.  This is a convenience
     * method which calls <code>setI</code> but maps <code>boolean</code>
     * values to integers.
     *
     * @param   attrib         the name of the boolean attribute to set
     * @param   value          the new value of <code>attrib</code>
     * @throws  AstException   if the AST routine generates an error,
     *                         in particular if no writable integer
     *                         attribute by that name exists
     */
    public void setB( String attrib, boolean value ) {
        setI( attrib, ( value ? 1 : 0 ) );
    }

    /** 
     * Set attribute values for an Object.   
     * This function assigns a set of attribute values to an Object,
     * over-riding any previous values. The attributes and their new
     * values are specified via a character string, which should
     * contain a comma-separated list of the form:
     * <p>
     *    "attribute_1 = value_1, attribute_2 = value_2, ... "
     * <p>
     * where "attribute_n" specifies an attribute name, and the value
     * to the right of each "=" sign should be a suitable textual
     * representation of the value to be assigned. This value will be
     * interpreted according to the attribute's data type.
     * <p>
     * The string supplied may also contain "printf"-style format
     * specifiers, identified by "%" signs in the usual way. If
     * present, these will be substituted by values supplied as
     * additional optional arguments (using the normal "printf" rules)
     * before the string is used.
     * <h4>Notes</h4>
     * <br> - Attribute names are not case sensitive and may be surrounded
     * by white space.
     * <br> - White space may also surround attribute values, where it will
     * generally be ignored (except for string-valued attributes where
     * it is significant and forms part of the value to be assigned).
     * <br> - It is not possible to include a comma directly in the value
     * assigned to an attribute via the "settings" string. To achieve
     * this, you should use "%s" format and supply the value as a
     * separate additional argument to astSet (or use the astSetC
     * function instead).
     * <br> - The same procedure may be adopted if "%" signs are to be included
     * and are not to be interpreted as format specifiers (alternatively,
     * the "printf" convention of writing "%%" may be used).
     * <br> - An error will result if an attempt is made to set a value for
     * a read-only attribute.
     * @param   settings
     * Pointer to a null-terminated character string containing a
     * comma-separated list of attribute settings in the form described
     * above.
     * 
     * @throws  AstException  if an error occurred in the AST library
     */
    public native void set( String settings );

    /** 
     * Display a textual representation of an Object on standard output.   
     * This function displays a textual description of any AST Object
     * on standard output. It is provided primarily as an aid to
     * debugging.
     * 
     * @throws  AstException  if an error occurred in the AST library
     */
    public native void show(  );

    /** 
     * Test if an Object attribute value is set.   
     * This function returns a boolean result (0 or 1) to indicate
     * whether a value has been explicitly set for one of an Object's
     * attributes.
     * <h4>Notes</h4>
     * <br> - Attribute names are not case sensitive and may be surrounded
     * by white space.
     * <br> - A value of zero will be returned if this function is invoked
     * with the AST error status set, or if it should fail for any reason.
     * <br> - A value of zero will also be returned if this function is used
     * to test a read-only attribute, although no error will result.
     * @param   attrib
     * Pointer to a null-terminated character string containing
     * the name of the attribute to be tested.
     * 
     * @throws  AstException  if an error occurred in the AST library
     */
    public native boolean test( String attrib );

    /**
     * Get 
     * object identification string.  
     * This attribute contains a string which may be used to identify
     * the Object to which it is attached. There is no restriction on
     * the contents of this string, which is not used internally by the
     * AST library, and is simply returned without change when
     * required. The default value is an empty string.
     * <p>
     * An identification string can be valuable when, for example,
     * several Objects have been stored in a file (using astWrite) and
     * are later retrieved (using astRead). Consistent use of the ID
     * attribute allows the retrieved Objects to be identified without
     * depending simply on the order in which they were stored.
     * <p>
     * This attribute may also be useful during debugging, to
     * distinguish similar Objects when using astShow to display them.
     * <h4>Notes</h4>
     * <br> - Unlike most other attributes, the value of the ID attribute is
     * not transferred when an Object is copied. Instead, its value is
     * undefined (and therefore defaults to an empty string) in any
     * copy. However, it is retained in any external representation of
     * an Object produced by the astWrite function.
     *
     * @return  this object's ID attribute
     */
    public String getID() {
        return getC( "ID" );
    }

    /**
     * Set 
     * object identification string.  
     * This attribute contains a string which may be used to identify
     * the Object to which it is attached. There is no restriction on
     * the contents of this string, which is not used internally by the
     * AST library, and is simply returned without change when
     * required. The default value is an empty string.
     * <p>
     * An identification string can be valuable when, for example,
     * several Objects have been stored in a file (using astWrite) and
     * are later retrieved (using astRead). Consistent use of the ID
     * attribute allows the retrieved Objects to be identified without
     * depending simply on the order in which they were stored.
     * <p>
     * This attribute may also be useful during debugging, to
     * distinguish similar Objects when using astShow to display them.
     * <h4>Notes</h4>
     * <br> - Unlike most other attributes, the value of the ID attribute is
     * not transferred when an Object is copied. Instead, its value is
     * undefined (and therefore defaults to an empty string) in any
     * copy. However, it is retained in any external representation of
     * an Object produced by the astWrite function.
     *
     * @param  ID   the ID attribute of this object
     */
    public void setID( String ID ) {
       setC( "ID", ID );
    }

    /**
     * Get 
     * permanent Object identification string.  
     * This attribute is like the ID attribute, in that it contains a 
     * string which may be used to identify the Object to which it is 
     * attached. The only difference between ID and Ident is that Ident 
     * is transferred when an Object is copied, but ID is not.
     * 
     *
     * @return  this object's Ident attribute
     */
    public String getIdent() {
        return getC( "Ident" );
    }

    /**
     * Set 
     * permanent Object identification string.  
     * This attribute is like the ID attribute, in that it contains a 
     * string which may be used to identify the Object to which it is 
     * attached. The only difference between ID and Ident is that Ident 
     * is transferred when an Object is copied, but ID is not.
     * 
     *
     * @param  Ident   the Ident attribute of this object
     */
    public void setIdent( String Ident ) {
       setC( "Ident", Ident );
    }

    /**
     * Get 
     * number of Objects in class.  
     * Number of Objects in class.
     * 
     *
     * @return  this object's Nobject attribute
     */
    public int getNobject() {
        return getI( "Nobject" );
    }

    /**
     * Get 
     * count of active Object pointers.  
     * This attribute gives the number of active pointers associated
     * with an Object. It is modified whenever pointers are created or
     * annulled (by astClone, astAnnul or astEnd for example). The count
     * includes the initial pointer issued when the Object was created.
     * <p>
     * If the reference count for an Object falls to zero as the result
     * of annulling a pointer to it, then the Object will be deleted.
     * 
     *
     * @return  this object's RefCount attribute
     */
    public int getRefCount() {
        return getI( "RefCount" );
    }

}