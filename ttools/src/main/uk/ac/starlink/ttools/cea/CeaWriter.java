package uk.ac.starlink.ttools.cea;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.ObjectFactory;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.util.XmlWriter;

/**
 * Writes an application description file suitable for use with
 * the AstroGrid Common Execution Architecture.
 *
 * <p>This makes a best guess at providing a CEA-friendly interface to
 * the capabilities of STILTS.  Call the {@link #main} method with 
 * the <code>-help</code> flag for a usage message.
 *
 * @author   Mark Taylor
 * @since    17 March 2006
 */
public class CeaWriter extends XmlWriter {

    private final CeaTask[] tasks_;
    private final String appPath_;
    private final String ceaAppName_;
    private final Formatter formatter_;
    private String docUrl_ = "http://www.starlink.ac.uk/stilts/sun256/";

    /**
     * Constructs a CeaWriter.
     *
     * @param   out  destination stream for XML
     * @param   tasks   array of CeaTask objects whose usage will be 
     *          provided in the output application definition file
     * @param   appPath  path at which the stilts script can be found
     * @param   ceaAppName  name by which the described application
     *          will be known to the CEA
     */
    CeaWriter( PrintStream out, CeaTask[] tasks, String appPath,
               String ceaAppName ) {
        super( out );
        tasks_ = tasks;
        appPath_ = appPath;
        ceaAppName_ = ceaAppName;
        formatter_ = new Formatter();
    }

    /**
     * Writes the application description to the output stream.
     *
     * @param   cmdline  invocation string for command
     */
    void writeConfig( String cmdline ) throws SAXException {

        /* Intro. */
        writeDeclaration();
        println( "<!-- Automatically generated by " 
             + "\n !      " + cmdline
             + "\n !   STILTS version " + Stilts.getVersion()
             + "\n !   " + new Date()
             + "\n !-->" );
        int startLevel = getLevel();

        startElement( "CommandLineExecutionControllerConfig",
            formatAttribute( "xmlns", 
                             "http://www.astrogrid.org/schema/" +
                             "CEAImplementation/v1" )
          + formatAttribute( "xmlns:ceab",
                             "http://www.astrogrid.org/schema/" +
                             "CommonExecutionArchitectureBase/v1" )
          + formatAttribute( "xmlns:agpd",
                             "http://www.astrogrid.org/schema/" +
                             "AGParameterDefinition/v1" )
          + formatAttribute( "xmlns:xsi",
                             "http://www.w3.org/2001/XMLSchema-instance" )
          + formatAttribute( "xsi:schemaLocation",
                             "http://www.astrogrid.org/schema/" +
                             "CEAImplementation/v1 " +
                             "./src/workflow-objects/schema/" +
                             "CEAImplementation.xsd" ) );
        println( getIndent( getLevel() ) 
               + "<!-- Application name set from " + getClass().getName()
               + " command-line flag. -->" );
        startElement( "Application",
                      formatAttribute( "name", ceaAppName_ ) 
                    + formatAttribute( "version", Stilts.getVersion() ) );

        /* Write all the parameters for all tasks.  They are given 
         * reference names qualified by their task names so that they can
         * be referenced separately in their respective interfaces later. */
        startElement( "ceab:Parameters" );
        for ( int i = 0; i < tasks_.length; i++ ) {
            writeParameters( tasks_[ i ] );
        }
        endElement( "ceab:Parameters" );

        /* Write a separate interface for each task. */
        startElement( "Interfaces",
                      formatAttribute( "xmlns",
                                       "http://www.astrogrid.org/schema/"
                                     + "CommonExecutionArchitectureBase/v1" ) );
        for ( int i = 0; i < tasks_.length; i++ ) {
            writeInterface( tasks_[ i ] );
        }
        endElement( "Interfaces" );

        /* Write the location of the executable. */
        println( getIndent( getLevel() )
               + "<!-- ExecutionPath set from " + getClass().getName() 
               + " command-line flag. -->" );
        addElement( "ExecutionPath", "", appPath_ );

        /* Write description matter.  This includes short text and a link
         * for each of the separate tasks (CEA interfaces). */
        addElement( "LongName", "",
                    "STILTS - "
                  + "Starlink Tables Infrastructure Library Tool Set" );
        addElement( "Version", "", Stilts.getVersion() );
        startElement( "Description" );
        println( "STILTS is a package which provides a number of table " +
                 "manipulation functions." );
        println( "The following tasks (profiles) are provided: " );
        for ( int i = 0; i < tasks_.length; i++ ) {
            CeaTask task = tasks_[ i ];
            println( "   " + task.getName() + ":" );
            println( "      " + task.getPurpose() );
        }
        endElement( "Description" );
        addElement( "ReferenceURL", "", docUrl_ );

        /* Outro. */
        endElement( "Application" );
        endElement( "CommandLineExecutionControllerConfig" );
        assert getLevel() == startLevel : "Mismatched levels";
    }

    /**
     * Writes parameter description elements for each parameter belonging
     * to a given task.
     *
     * @param   task   task
     */
    private void writeParameters( CeaTask task ) throws SAXException {
        CeaParameter[] params = task.getParameters();

        /* Write a CEA parameter definition representing the task itself.
         * This is identified only by its position on the command line,
         * and is only allowed to assume a single, fixed value. */
        startElement( "CmdLineParameterDefn",
                      formatAttribute( "name", getParamRef( task, null ) )
                    + formatAttribute( "commandPosition", "1" )
                    + formatAttribute( "fileRef", "false" )
                    + formatAttribute( "type", "text" ) );
        addElement( "agpd:UI_Name", "", "task" );
        addElement( "agpd:UI_Description", "", "Task name - fixed" );
        addElement( "agpd:DefaultValue", "", task.getName() );
        startElement( "agpd:OptionList" );
        addElement( "agpd:OptionVal", "", task.getName() );
        endElement( "agpd:OptionList" );
        endElement( "CmdLineParameterDefn" );

        /* Write CEA parameter definitions for each of the task's parameters. */
        for ( int iParam = 0; iParam < params.length; iParam++ ) {
            CeaParameter param = params[ iParam ];
            startElement( "CmdLineParameterDefn",
                          formatAttribute( "name", getParamRef( task, param ) )
                        + formatAttribute( "commandSwitch", param.getName() )
                        + formatAttribute( "type", param.getType() )
                        + formatAttribute( "fileRef", param.isRef() ? "true"
                                                                    : "false" )
                        + formatAttribute( "switchType", "keyword" ) );
            addElement( "agpd:UI_Name", "", param.getName() );

            /* Note that although the CEA v1 schema documentation gives 
             * UI_Description content as type "agpd:xhtmlDocumentation",
             * at time of writing, it only treats it as plain text, 
             * meaning that tags come out as angle brackets etc.
             * So translate to plain text here. */
            addElement( "agpd:UI_Description", "",
                        xmlToCdata( param.getDescription() ) );
            String dflt = param.getDefault();
            if ( dflt != null && dflt.trim().length() > 0 ) {
                addElement( "agpd:DefaultValue", "", dflt );
            }
            Object[] options = param.getOptions();
            if ( options != null && options.length > 0 ) {
                startElement( "agpd:OptionList" );
                for ( int iOpt = 0; iOpt < options.length; iOpt++ ) {
                    Object opt = options[ iOpt ];
                    addElement( "agpd:OptionVal", "",
                                opt == null ? null : opt.toString() );
                }
                endElement( "agpd:OptionList" );
            }
            endElement( "CmdLineParameterDefn" );
        }
    }

    /**
     * Writes the Interface element used for invoking a given task.
     *
     * @param   task  task
     */
    private void writeInterface( CeaTask task ) {

        /* Intro for an interface representing a STILTS task. */
        CeaParameter[] params = task.getParameters();
        startElement( "Interface", formatAttribute( "name", task.getName() ) );
        startElement( "input" );

        /* Write a CEA parameter reference for the parameter representing
         * the task itself. */
        addElement( "pref",
                    formatAttribute( "ref", getParamRef( task, null ) ),
                    "" );

        /* Write CEA parameter references for each of the non-output 
         * parameters used by this task. */
        for ( int iParam = 0; iParam < params.length; iParam++ ) {
            CeaParameter param = params[ iParam ];
            if ( ! param.isOutput() ) {
                String atts = formatAttribute( "ref",
                                               getParamRef( task, param ) );
                if ( param.isMulti() ) {
                    atts = atts + formatAttribute( "minoccurs", "0" )
                                + formatAttribute( "maxoccurs", "0" );
                }
                else if ( param.isNullPermitted() ) {
                    atts = atts + formatAttribute( "minoccurs", "0" );
                }
                addElement( "pref", atts, "" );
            }
        }
        endElement( "input" );

        /* Write CEA parameter references for each of the output parameters
         * used by this task. */
        startElement( "output" );
        for ( int iParam = 0; iParam < params.length; iParam++ ) {
            CeaParameter param = params[ iParam ];
            if ( param.isOutput() ) {
                String atts = formatAttribute( "ref",
                                               getParamRef( task, param ) );
                addElement( "pref", atts, "" );
            }
        }
        endElement( "output" );

        /* Outro. */
        endElement( "Interface" );
    }

    /**
     * Takes XML text on input (simple HTML-like constructions are understood)
     * and outputs line-formatted text suitable for writing as content of
     * a CDATA declared element.
     *
     * @param   xmlText  input, containing XML markup
     * @return  output, maybe containing XML entity references, but no tags
     */
    private String xmlToCdata( String xmlText ) throws SAXException {
        return formatText( formatter_.formatXML( xmlText, 0 ) );
    }

    /**
     * Returns a unique key relating to a given task and one of its parameters.
     * The result is not primarily designed to be human-readable.
     *
     * @param   task   task
     * @param   param   parameter - if null, the returned reference describes
     *          the task itself
     */
    private static String getParamRef( CeaTask task, CeaParameter param ) {
        return param == null 
             ? "TASK-" + task.getName()
             : task.getName() + "_" + param.getName();
    }

    /**
     * Returns an array of the tasks which should form part of the CEA
     * STILTS interface.  The list of tasks and their parameters etc
     * may (should) be doctored for use which may be different from their
     * use on the command line as appropriate.
     *
     * @return   task array
     */
    static CeaTask[] getTasks() throws LoadException {

        /* Get a map containing each of the known STILTS tasks keyed by
         * name. */
        ObjectFactory taskFactory = Stilts.getTaskFactory();
        String[] taskNames = taskFactory.getNickNames();
        Map appMap = new HashMap();
        for ( int i = 0; i < taskNames.length; i++ ) {
            String name = taskNames[ i ];
            Task task = (Task) taskFactory.createObject( name );
            appMap.put( name, new CeaTask( task, name ) );
        }

        /* Doctor some of the specific tasks as required; small changes
         * to parameters etc are required for sensible use in a CEA
         * environment. */
        CeaTask tpipe = (CeaTask) appMap.get( "tpipe" );
        tpipe.removeParameter( "script" );
        tpipe.removeParameter( "istream" );

        CeaTask votcopy = (CeaTask) appMap.get( "votcopy" );
        votcopy.getParameter( "out" ).setOutput( true );

        CeaTask votlint = (CeaTask) appMap.get( "votlint" );
        votlint.getParameter( "out" ).setOutput( true );

        /* Prepare and return an array of CeaTask objects sorted by name. */
        String[] appNames = (String[]) new ArrayList( appMap.keySet() )
                                      .toArray( new String[ 0 ] );
        Arrays.sort( appNames );
        CeaTask[] tasks = new CeaTask[ appNames.length ];
        for ( int i = 0; i < appNames.length; i++ ) {
            tasks[ i ] = (CeaTask) appMap.get( appNames[ i ] );
        }
        return tasks;
    }

    /**
     * Main method.  Invoked to write STILTS application description to 
     * standard output.
     *
     * @example <code>CeaWriter -path /starjava/bin/stilts 
     *                          -auth org.astrogrid.test1/stilts</code>
     *
     * @param   args  arguments - invoke with "-help" for a usage messege
     */
    public static void main( String[] args )
            throws LoadException, SAXException {
        String usage = "\n   Usage: " + CeaWriter.class.getName()
                     + " [-help]"
                     + " -path stilts-path"
                     + " -auth cea-auth"
                     + " [-docurl docurl]"
                     + "\n";

        /* Process arguments. */
        List argList = new ArrayList( Arrays.asList( args ) );
        String appPath = null;
        String ceaAuth = "astrogrid.cam";
        String docUrl = null;
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-path" ) && it.hasNext() ) {
                it.remove();
                appPath = (String) it.next();
                it.remove();
            }
            else if ( arg.equals( "-auth" ) && it.hasNext() ) {
                it.remove();
                ceaAuth = (String) it.next();
                it.remove();
            }
            else if ( arg.equals( "-docurl" ) && it.hasNext() ) {
                it.remove();
                docUrl = (String) it.next();
                it.remove();
            }
            else if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return;
            }
        }

        /* Validate arguments. */
        if ( ! argList.isEmpty() || appPath == null || ceaAuth == null ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Prepare writer object. */
        CeaWriter ceaWriter = new CeaWriter( System.out, getTasks(), appPath,
                                             ceaAuth + "/stilts" );
        if ( docUrl != null ) {
            ceaWriter.docUrl_ = docUrl;
        }

        /* Prepare command line. */
        StringBuffer cbuf = new StringBuffer( ceaWriter.getClass().getName() );
        for ( int i = 0; i < args.length; i++ ) {
            cbuf.append( ' ' ).append( args[ i ] );
        }

        /* Do work. */
        ceaWriter.writeConfig( cbuf.toString() );
    }
}
