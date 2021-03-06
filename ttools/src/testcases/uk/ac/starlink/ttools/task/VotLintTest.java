package uk.ac.starlink.ttools.task;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.ttools.TableTestCase;

public class VotLintTest extends TableTestCase {

    public VotLintTest( String name ) {
        super( name );
    }

    private String[] getOutputLines( Map map ) throws Exception {
       MapEnvironment env = new MapEnvironment( map );
       new VotLint().createExecutable( env ).execute();
       String[] lines = env.getOutputLines();

       /* Correct for the differences between the XML parsers used by
        * different JVMs. */
       for ( int i = 0; i < lines.length; i++ ) {
           lines[ i ] = lines[ i ].replaceFirst( ", c.[0-9]+\\)", ")" );
       }
       return lines;
    }

    public void testSilent() throws Exception {
        Map map = new HashMap();
        map.put( "votable",
                 getClass().getResource( "no-errors.vot.gz" ).toString() );
        assertEquals( 0, getOutputLines( map ).length );
    }

    public void testErrors() throws Exception {

        String[] errors = new String[] {
            "INFO (l.4): No arraysize for character, "
                + "FIELD implies single character",
            "WARNING (l.11): Characters after first in char scalar ignored "
                + "(missing arraysize?)",
            "WARNING (l.15): Wrong number of TDs in row (expecting 3 found 4)",
            "ERROR (l.18): Row count (1) not equal to nrows attribute (2)",
        };

        Map map = new HashMap();
        map.put( "votable",
                 getClass().getResource( "with-errors.vot" ).toString() );

        /* When validating, the details of the error messages will depend
         * on the XML parser being used - which changes according to what
         * JVM you're using (e.g. J2SE1.4 or 1.5).  So just check we've got
         * the right number of errors here. */
        map.put( "validate", "true" );
        assertEquals( 5, getOutputLines( map ).length );

        /* In other cases we can check the content of the error messages
         * themselves, since they are output by votlint itself. */
        map.put( "validate", "false" );
        assertEquals( 4, getOutputLines( map ).length );
        assertArrayEquals( errors, getOutputLines( map ) );

        map.put( "version", "1.1" );
        assertArrayEquals( errors, getOutputLines( map ) );

        map.put( "version", "1.0" );
        assertEquals( "WARNING (l.1): Declared version (1.1) " +
                      "differs from version specified to linter (1.0)",
                      getOutputLines( map )[ 0 ] );

        map.put( "version", "9.9" );
        try {
            getOutputLines( map );
            fail();
        }
        catch ( ParameterValueException e ) {
            // OK
        }
    }
}
