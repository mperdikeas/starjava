/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Science and Technology Facilities Council
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoords;
import jsky.util.SwingWorker;

import org.us_vo.www.SimpleResource;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.ToolButtonBar;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.SplatPlastic;
import uk.ac.starlink.splat.util.StarTableTransmitter;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.util.gui.ProxySetupFrame;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Display a page of controls for querying a list of  SSA servers and
 * displaying the results of those queries. The spectra returned can then be
 * selected and displayed in the main SPLAT browser.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAQueryBrowser
    extends JFrame
    implements ActionListener, MouseListener
{
    /** UI preferences. */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( SSAQueryBrowser.class );

    /** Initial window size and location */
    private static final Rectangle defaultWindowLocation =
        new Rectangle( 0, 0, 500, 550 );

    /** The object holding the list of servers that we should use for SSA
     *  queries. */
    private SSAServerList serverList = null;

    /** The instance of SPLAT we're associated with. */
    private SplatBrowser browser = null;

    /** The SpecDataFactory.*/
    private SpecDataFactory specDataFactory = SpecDataFactory.getInstance();

    /** File chooser used for saving and restoring queries. */
    protected BasicFileChooser fileChooser = null;

    /** Content pane of frame */
    protected JPanel contentPane = null;

    /** Centre panel */
    protected JPanel centrePanel = null;

    /** Object name */
    protected JTextField nameField = null;

    /** Resolve object name button */
    protected JButton nameLookup = null;

    /** Download and display selected spectra */
    protected JButton displaySelectedButton = null;

    /** Download and display all spectra */
    protected JButton displayAllButton = null;

    /** Make the query to all known servers */
    protected JButton goButton = null;

    /** Central RA */
    protected JTextField raField = null;

    /** Central Dec */
    protected JTextField decField = null;

    /** Region radius */
    protected JTextField radiusField = null;

    /** Lower limit for BAND */
    protected JTextField lowerBandField = null;

    /** Upper limits for BAND */
    protected JTextField upperBandField = null;

    /** ButtonGroup for the format selection */
    protected ButtonGroup formatGroup = null;

    /** Tabbed pane showing the query results tables */
    protected JTabbedPane resultsPane = null;

    /** The list of StarJTables in use */
    protected ArrayList starJTables = null;

    /** NED name resolver catalogue */
    protected SkycatCatalog nedCatalogue = null;

    /** SIMBAD name resolver catalogue */
    protected SkycatCatalog simbadCatalogue = null;

    /** The current name resolver */
    protected SkycatCatalog resolverCatalogue = null;

    /** The proxy server dialog */
    protected ProxySetupFrame proxyWindow = null;

    /** The SSA servers window */
    protected SSAServerFrame serverWindow = null;

    /** Make sure the proxy environment is setup */
    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAQueryBrowser( SSAServerList serverList, SplatBrowser browser )
    {
        this.serverList = serverList;
        this.browser = browser;
        initUI();
        initMenusAndToolbar();
        initFrame();
    }

    /**
     * Create and display the UI components.
     */
    private void initUI()
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );
        centrePanel = new JPanel( new BorderLayout() );
        contentPane.add( centrePanel, BorderLayout.CENTER );

        initQueryComponents();
        initResultsComponent();
        setDefaultNameServers();
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    private void initMenusAndToolbar()
    {
        //  Add the menuBar.
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  Create the toolbar.
        ToolButtonBar toolBar = new ToolButtonBar( contentPane );
        JPanel actionBarContainer = new JPanel();

        //  Get icons.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon saveImage =
            new ImageIcon( ImageHolder.class.getResource( "savefile.gif" ) );
        ImageIcon readImage =
            new ImageIcon( ImageHolder.class.getResource( "openfile.gif" ) );
        ImageIcon helpImage =
            new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );
        ImageIcon ssaImage =
            new ImageIcon( ImageHolder.class.getResource("ssapservers.gif") );

        //  Create the File menu.
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add options to save and restore the query result.
        LocalAction saveAction = new LocalAction( LocalAction.SAVE,
                                                  "Save query results",
                                                  saveImage,
                                                  "Save results of query " +
                                                  "to disk file" );

        fileMenu.add( saveAction );
        JButton saveButton = new JButton( saveAction );
        actionBarContainer.add( saveButton );

        LocalAction readAction = new LocalAction( LocalAction.READ,
                                                  "Restore query results",
                                                  readImage,
                                                  "Read results of a " +
                                                  "previous query back " +
                                                  "from disk file" );
        fileMenu.add( readAction );
        JButton readButton = new JButton( readAction );
        actionBarContainer.add( readButton );

        //  Add an action to close the window.
        LocalAction closeAction = new LocalAction( LocalAction.CLOSE,
                                                   "Close", closeImage,
                                                   "Close window",
                                                   "control W" );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBarContainer.add( closeButton );

        //  Create the options menu.
        JMenu optionsMenu = new JMenu( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        LocalAction proxyAction = new LocalAction( LocalAction.PROXY,
                                                   "Configure connection " +
                                                   "proxy..." );
        optionsMenu.add( proxyAction );

        //  Add item to control the use of SSA servers.
        LocalAction serverAction = new LocalAction(LocalAction.SERVER,
                                                   "Configure SSAP servers...",
                                                   ssaImage,
                                                   "Configure SSAP servers" );
        optionsMenu.add( serverAction );
        toolBar.add( serverAction );

        //  SSAP version 1 format control.
        initFormatOptions( optionsMenu );

        //  Create a menu containing all the name resolvers.
        JMenu resolverMenu = new JMenu( "Resolver" );
        resolverMenu.setMnemonic( KeyEvent.VK_R );
        menuBar.add( resolverMenu );

        ButtonGroup bg = new ButtonGroup();

        JRadioButtonMenuItem jrbmi = new JRadioButtonMenuItem();
        resolverMenu.add( jrbmi );
        jrbmi.setSelected( true );
        bg.add( jrbmi );
        jrbmi.setAction( new ResolverAction( "SIMBAD", simbadCatalogue ) );

        jrbmi = new JRadioButtonMenuItem();
        resolverMenu.add( jrbmi );
        bg.add( jrbmi );
        jrbmi.setAction( new ResolverAction( "NED", nedCatalogue ) );
        resolverCatalogue = simbadCatalogue;


        //  Create a menu for PLASTIC interoperations.
        JMenu interopMenu = new JMenu( "Interop" );
        interopMenu.setMnemonic( KeyEvent.VK_I );
        menuBar.add( interopMenu );

        //  Need an actions to transmit and broadcast the current
        //  results table.
        SplatPlastic plasticServer = browser.getPlasticServer();

        // Add table transmit options.
        StarTableTransmitter transmitter = 
            new StarTableTransmitter( plasticServer, this );
        interopMenu.add( transmitter.getBroadcastAction() )
            .setMnemonic( KeyEvent.VK_B );
        interopMenu.add( transmitter.createSendMenu() )
            .setMnemonic( KeyEvent.VK_T );

        //  Create the Help menu.
        HelpFrame.createButtonHelpMenu( "ssa-window", "Help on window",
                                        menuBar, toolBar );

        //  ActionBar goes at bottom.
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        setTitle( Utilities.getTitle( "Query VO for Spectra" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 550, 500 ) );
        Utilities.setFrameLocation( this, defaultWindowLocation, prefs, 
                                    "SSAQueryBrowser" );
        setVisible( true );
    }

    /**
     * Populate the north part of center window with the basic query
     * components.
     */
    private void initQueryComponents()
    {
        JPanel queryPanel = new JPanel();
        queryPanel.setBorder
            ( BorderFactory.createTitledBorder( "Search region:" ) );

        GridBagLayouter layouter =
            new GridBagLayouter( queryPanel, GridBagLayouter.SCHEME3 );
        centrePanel.add( queryPanel, BorderLayout.NORTH );

        //  Object name. Arrange for a resolver to look up the coordinates of
        //  the object name, when return or the lookup button are pressed.
        JLabel nameLabel = new JLabel( "Object:" );
        nameField = new JTextField( 15 );
        nameField.setToolTipText( "Enter the name of an object " +
                                  "and press return to get coordinates" );
        nameField.addActionListener( this );
        layouter.add( nameLabel, false );
        layouter.add( nameField, false );

        nameLookup = new JButton( "Lookup" );
        nameLookup.addActionListener( this );
        layouter.add( nameLookup, true );


        //  RA and Dec fields. We're free-formatting on these (decimal degrees
        //  not required).
        JLabel raLabel = new JLabel( "RA:" );
        raField = new JTextField( 15 );
        layouter.add( raLabel, false );
        layouter.add( raField, true );
        raField.setToolTipText( "Enter the RA of field centre, " +
                                "decimal degrees or hh:mm:ss.ss" );

        JLabel decLabel = new JLabel( "Dec:" );
        decField = new JTextField( 15 );
        layouter.add( decLabel, false );
        layouter.add( decField, true );
        decField.setToolTipText( "Enter the Dec of field centre, " +
                                 "decimal degrees or dd:mm:ss.ss" );

        //  Radius field.
        JLabel radiusLabel = new JLabel( "Radius:" );
        radiusField = new JTextField( "10.0", 15 );
        layouter.add( radiusLabel, false );
        layouter.add( radiusField, true );
        radiusField.setToolTipText( "Enter radius of field to search" +
                                    " from given centre, arcminutes" );
        radiusField.addActionListener( this );

        //  Band fields.
        JLabel bandLabel = new JLabel( "Band:" );
        lowerBandField = new JTextField( 15 );
        upperBandField = new JTextField( 15 );
        layouter.add( bandLabel, false );
        layouter.add( lowerBandField, false );
        layouter.add( upperBandField, false );
        layouter.eatLine();
        lowerBandField.setToolTipText( "Lower limit, or single include " +
                                       "value, for spectral band, in meters" );
        upperBandField.setToolTipText
            ( "Upper limit for spectral band, in meters" );

        //  Do the search.
        goButton = new JButton( "Go" );
        goButton.addActionListener( this );
        JPanel buttonPanel = new JPanel();
        buttonPanel.add( goButton );

        layouter.add( buttonPanel, true );
        layouter.eatSpare();

    }

    /**
     * Make the results component. This is mainly JTabbedPane containing a
     * JTable for each set of results (the tables are realized later) and
     * a button to display the selected spectra.
     */
    private void initResultsComponent()
    {
        JPanel resultsPanel = new JPanel( new BorderLayout() );
        resultsPanel.setBorder
            ( BorderFactory.createTitledBorder( "Query results:" ) );

        resultsPane = new JTabbedPane();
        resultsPanel.add( resultsPane, BorderLayout.CENTER );

        JPanel controlPanel = new JPanel();
        displaySelectedButton = new JButton( "Display selected" );
        controlPanel.add( displaySelectedButton );

        //  Add action to display all currently selected spectra.
        displaySelectedButton.addActionListener( this );

        displayAllButton = new JButton( "Display all" );
        controlPanel.add( displayAllButton );

        //  Add action to display all spectra.
        displayAllButton.addActionListener( this );

        resultsPanel.add( controlPanel, BorderLayout.SOUTH );
        centrePanel.add( resultsPanel, BorderLayout.CENTER );
    }

    /**
     * Initialise the SSAP version 1 data formats. Don't want 
     * one of these by default.
     */
    protected void initFormatOptions( JMenu optionsMenu )
    {
        JMenu formatMenu = new JMenu( "Query format" );
        String[] names = 
            { "None", "ALL", "COMPLIANT", "votable", "fits", "xml" };
        JRadioButtonMenuItem item;
        formatGroup = new ButtonGroup();
        for ( int i = 0; i < names.length; i++ ) {
            item = new JRadioButtonMenuItem( names[i] );
            if ( i == 0 ) {
                item.setSelected( true );
            }
            item.setActionCommand( names[i] );
            formatGroup.add( item );
            formatMenu.add( item );
        }
        optionsMenu.add( formatMenu );
    }

    /**
     * Arrange to resolve the object name into coordinates.
     */
    protected void resolveName()
    {
        String objectName = nameField.getText().trim();
        if ( objectName != null && objectName.length() > 0 ) {

            final QueryArgs queryArgs =
                new BasicQueryArgs( resolverCatalogue );

            // If objectName has spaces we should protect them.
            objectName = objectName.replaceAll( " ", "%20" );
            queryArgs.setId( objectName );

            Thread thread = new Thread( "Name server" )
            {
                public void run()
                {
                    try {
                        QueryResult r = resolverCatalogue.query( queryArgs );
                        if ( r instanceof TableQueryResult ) {
                            Coordinates coords =
                                ((TableQueryResult) r).getCoordinates( 0 );
                            if ( coords instanceof WorldCoords ) {
                                //  Enter values into RA and Dec fields.
                                String[] radec =
                                    ((WorldCoords) coords).format();
                                raField.setText( radec[0] );
                                decField.setText( radec[1] );
                            }
                        }
                    }
                    catch (Exception e) {
                        new ExceptionDialog( null, e );
                    }
                }
            };

            //  Start the nameserver.
            raField.setText( "" );
            decField.setText( "" );
            thread.start();

        }
    }

    /**
     * Setup the default name servers (SIMBAD and NED) to use to resolve
     * astronomical object names. Note these are just those used in JSky.
     * A better implementation should reuse the JSky classes.
     * <p>
     * XXX refactor these into an XML file external to the application.
     * Maybe switch to the CDS Sesame webservice.
     */
    private void setDefaultNameServers()
    {
        Properties p1 = new Properties();
        p1.setProperty( "serv_type", "namesvr" );
        p1.setProperty( "long_name", "SIMBAD Names via CADC" );
        p1.setProperty( "short_name", "simbad_ns@cadc" );
        p1.setProperty
            ( "url",
              "http://cadcwww.dao.nrc.ca/cadcbin/sim-server?&o=%id" );
        SkycatConfigEntry entry = new SkycatConfigEntry( p1 );
        simbadCatalogue = new SkycatCatalog( entry );

        Properties p2 = new Properties();
        p2.setProperty( "serv_type", "namesvr" );
        p2.setProperty( "long_name", "NED Names" );
        p2.setProperty( "short_name", "ned@eso" );
        p2.setProperty
            ( "url",
              "http://archive.eso.org/skycat/servers/ned-server?&o=%id" );
        entry = new SkycatConfigEntry( p2 );
        nedCatalogue = new SkycatCatalog( entry );
    }

    /**
     * Perform the query to all the currently selected servers.
     */
    public void doQuery()
    {
        //  Get the position.
        String ra = raField.getText();
        String dec = decField.getText();
        if ( ra == null || ra.length() == 0 ||
             dec == null || dec.length() == 0 ) {
            JOptionPane.showMessageDialog( this,
               "You have not supplied a search centre",
               "No RA or Dec", JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  And the radius.
        String radiusText = radiusField.getText();
        double radius = 10.0;
        if ( radiusText != null && radiusText.length() > 0 ) {
            try {
                radius = Double.parseDouble( radiusText );
            }
            catch (NumberFormatException e) {
                new ExceptionDialog(this, "Cannot understand radius value", e);
                return;
            }
        }

        //  Spectral bandpass. These should be in meters. XXX allow other
        //  units and do the conversion.
        String lower = lowerBandField.getText();
        if ( "".equals( lower ) ) {
            lower = null;
        }
        String upper = upperBandField.getText();
        if ( "".equals( upper ) ) {
            upper = null;
        }

        //  See if there's a data format choice.
        String format = formatGroup.getSelection().getActionCommand();
        if ( format.equals( "None" ) ) {
            format = null;
        }

        //  Create a stack of all queries to perform.
        ArrayList queryList = new ArrayList();
        Iterator i = serverList.getIterator();
        SimpleResource server = null;
        while( i.hasNext() ) {
            server = (SimpleResource) i.next();
            SSAQuery ssaQuery = new SSAQuery( server );
            ssaQuery.setPosition( ra, dec );
            ssaQuery.setRadius( radius );
            ssaQuery.setBand( lower, upper );
            ssaQuery.setFormat( format );
            queryList.add( ssaQuery );
        }

        // Now actually do the queries, these are performed in a separate
        // Thread so we avoid locking the interface.
        if ( queryList.size() > 0 ) {
            processQueryList( queryList );
        }
        else {
            JOptionPane.showMessageDialog( this,
               "There are no SSA servers currently selected",
               "No SSA servers", JOptionPane.ERROR_MESSAGE );

        }
    }

    /**
     * Process a list of URL queries to SSA servers and display the
     * results. All processing is performed in a background Thread.
     */
    protected void processQueryList( ArrayList queryList )
    {
        final ArrayList localQueryList = queryList;
        makeResultsDisplay( null );

        final ProgressPanelFrame progressFrame =
            new ProgressPanelFrame( "Querying SSAP servers" );

        Iterator i = queryList.iterator();
        while ( i.hasNext() ) {
            final SSAQuery ssaQuery = (SSAQuery) i.next();
            final ProgressPanel progressPanel =
                new ProgressPanel( "Querying: " + ssaQuery.getDescription() );
            progressFrame.addProgressPanel( progressPanel );

            final SwingWorker worker = new SwingWorker()
                {
                    boolean interrupted = false;
                    public Object construct()
                    {
                        progressPanel.start();
                        try {
                            runProcessQuery( ssaQuery, progressPanel );
                        }
                        catch (InterruptedException e) {
                            interrupted = true;
                        }
                        return null;
                    }

                    public void finished()
                    {
                        progressPanel.stop();
                        queryThread = null;

                        //  Display the results.
                        if ( ! interrupted ) {
                            addResultsDisplay( ssaQuery );
                        }
                    }
                };

            progressPanel.addActionListener( new ActionListener()
                {
                    public void actionPerformed( ActionEvent e )
                    {
                        if ( worker != null ) {
                            worker.interrupt();
                        }
                    }
                });

            worker.start();
        }
    }

    private Thread queryThread = null;

    /**
     * Do a query to an SSAP server.
     */
    private void runProcessQuery( SSAQuery ssaQuery,
                                  ProgressPanel progressPanel )
        throws InterruptedException
    {
        //  We just download VOTables, so avoid attempting to build the other
        //  formats.
        StarTableFactory factory = new StarTableFactory();
        TableBuilder[] blist = { new VOTableBuilder() };
        factory.setDefaultBuilders( blist );

        StarTable starTable = null;

        // int j = 0;

        try {
            URL url = ssaQuery.getQueryURL();
            progressPanel.logMessage( ssaQuery.getBaseURL() );
            starTable = factory.makeStarTable( url );

            //  Check parameter QUERY_STATUS, this should be set to OK
            //  when the query
            String queryOK = null;
            try {
                queryOK = starTable
                    .getParameterByName( "QUERY_STATUS" )
                    .getValueAsString( 100 );
            }
            catch (NullPointerException ne) {
                // Whoops, that's not good, but see what we can do.
            }
            if ( "OK".equalsIgnoreCase( queryOK ) ) {
                ssaQuery.setStarTable( starTable );
                progressPanel.logMessage( "Done" );
            }
            else {
                //  Some problem with the service, report that.
                progressPanel.logMessage( "Query failed: " + queryOK );
            }

            //  Dump query results as VOTables.
            //uk.ac.starlink.table.StarTableOutput sto =
            //    new uk.ac.starlink.table.StarTableOutput();
            //sto.writeStarTable( starTable,
            //                    "votable" + j + ".xml", null);
            //j++;
        }
        catch (TableFormatException te) {
            progressPanel.logMessage( te.getMessage() );
            System.out.println( te.getMessage() );
        }
        catch (IOException ie) {
            progressPanel.logMessage( ie.getMessage() );
            System.out.println( ie.getMessage() );
        }
        catch (Exception ge) {
            //  General exception.
            progressPanel.logMessage( ge.getMessage() );
            System.out.println( ge.getMessage() );
        }
        if ( Thread.interrupted() ) {
            throw new InterruptedException();
        }
        progressPanel.logMessage( "Completed download" );
    }

    /**
     * Display the results of the queries to the SSA servers. The results can
     * be either a list of SSAQuery instances, or the StarTables themselves
     * (usually these are from a disk-file restoration, not a live query).
     */
    protected void makeResultsDisplay( ArrayList tableList )
    {
        if ( starJTables == null ) {
            starJTables = new ArrayList();
        }

        //  Remove existing tables.
        resultsPane.removeAll();
        starJTables.clear();

        if ( tableList != null ) {
            Iterator i = tableList.iterator();
            while ( i.hasNext() ) {
                addResultsDisplay( i.next() );
            }
        }
    }

    protected void addResultsDisplay( Object next )
    {
        DescribedValue dValue = null;
        JScrollPane scrollPane = null;
        SSAQuery ssaQuery = null;
        StarJTable table = null;
        StarTable starTable = null;
        String shortName = null;

        if ( next instanceof SSAQuery && next != null ) {
            ssaQuery = (SSAQuery) next;
            starTable = ssaQuery.getStarTable();
            shortName = ssaQuery.getDescription();
        }
        else if ( next instanceof StarTable) {
            starTable = (StarTable) next;
            dValue = starTable.getParameterByName( "ShortName" );
            if ( dValue == null ) {
                shortName = starTable.getName();
            }
            else {
                shortName = (String)dValue.getValue();
            }
        }
        else {
            System.out.println( "Couldn't handle: " + next );
        }
        if ( starTable != null ) {
            table = new StarJTable( starTable, true );
            scrollPane = new JScrollPane( table );
            resultsPane.addTab( shortName, scrollPane );
            starJTables.add( table );

            //  Set widths of columns.
            table.configureColumnWidths( 200, 5 );

            //  Double click on row means load just that spectrum.
            table.addMouseListener( this );
        }
    }

    /**
     * Get the main SPLAT browser to download and display spectra.
     * <p>
     * Can either display all the spectra, just the selected spectra, or the
     * spectrum from a row in a given table. If table is null, then the
     * selected parameter determines the behaviour of all or just the selected
     * spectra.
     */
    protected void displaySpectra( boolean selected, StarJTable table,
                                   int row )
    {
        //  List of all spectra to be loaded and their data formats and short
        //  names etc.
        ArrayList specList = new ArrayList();

        if ( table == null ) {
            //  Visit all the tabbed StarJTables.
            Iterator i = starJTables.iterator();
            while ( i.hasNext() ) {
                extractSpectraFromTable( (StarJTable) i.next(), specList,
                                         selected, -1 );
            }
        }
        else {
            extractSpectraFromTable( table, specList, selected, row );
        }

        //  If we have no spectra complain and stop.
        if ( specList.size() == 0 ) {
            String mess;
            if ( selected ) {
                mess = "There are no spectra selected";
            }
            else {
                mess = "No spectra available";
            }
            JOptionPane.showMessageDialog( this, mess, "No spectra",
                                           JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  And load and display...
        SpectrumIO.Props[] propList = new SpectrumIO.Props[specList.size()];
        specList.toArray( propList );
        browser.threadLoadSpectra( propList );
        browser.toFront();
    }

    /**
     * Extract all the links to spectra for downloading, plus the associated
     * information available in the VOTable. Each set of spectral information
     * is used to populated a SpectrumIO.Prop object that is added to the
     * specList list.
     * <p>
     * Can return the selected spectra, if requested, otherwise all spectra
     * are returned or if a row value other than -1 is given just one row.
     */
    private void extractSpectraFromTable( StarJTable starJTable,
                                          ArrayList specList,
                                          boolean selected,
                                          int row )
    {
        int[] selection = null;

        //  Check for a selection if required, otherwise we're using the given
        //  row.
        if ( selected && row == -1 ) {
            selection = starJTable.getSelectedRows();
        }
        else if ( row != -1 ) {
            selection = new int[1];
            selection[0] = row;
        }

        // Only do this if we're processing all rows or we have a selection.
        if ( selection == null || selection.length > 0 ) {
            StarTable starTable = starJTable.getStarTable();

            //  Check for a column that contains links to the actual data
            //  (XXX these could be XML links to data within this
            //  document). The signature for this is an UCD of DATA_LINK,
            //  or a UTYPE of Access.Reference.
            int ncol = starTable.getColumnCount();
            int linkcol = -1;
            int typecol = -1;
            int namecol = -1;
            int axescol = -1;
            int specaxiscol = -1;
            int fluxaxiscol = -1;
            int unitscol = -1;
            int specunitscol = -1;
            int fluxunitscol = -1;
            int fluxerrorcol = -1;
            ColumnInfo colInfo;
            String ucd;
            String utype;
            for( int k = 0; k < ncol; k++ ) {
                colInfo = starTable.getColumnInfo( k );
                ucd = colInfo.getUCD();

                //  Old-style UCDs for backwards compatibility.
                if ( ucd != null ) {
                    ucd = ucd.toLowerCase();
                    if ( ucd.equals( "data_link" ) ) {
                        linkcol = k;
                    }
                    else if ( ucd.equals( "vox:spectrum_format" ) ) {
                        typecol = k;
                    }
                    else if ( ucd.equals( "vox:image_title" ) ) {
                        namecol = k;
                    }
                    else if ( ucd.equals( "vox:spectrum_axes" ) ) {
                        axescol = k;
                    }
                    else if ( ucd.equals( "vox:spectrum_units" ) ) {
                        unitscol = k;
                    }
                }

                //  Version 1.0 utypes. XXX not sure if axes names
                //  are in columns or are really parameters. Assume
                //  these work like the old-style scheme and appear in
                //  the columns.
                utype = (String) 
                    colInfo.getAuxDatumValueByName( "utype", String.class );
                if ( utype != null ) {
                    utype = utype.toLowerCase();
                    if ( utype.endsWith( "access.reference" ) ) {
                        linkcol = k;
                    }
                    else if ( utype.endsWith( "access.format" ) ) {
                        typecol = k;
                    }
                    else if ( utype.endsWith( "target.name" ) ) {
                        namecol = k;
                    }
                    else if ( utype.endsWith( "char.spectralaxis.name" ) ) {
                        specaxiscol = k;
                    }
                    else if ( utype.endsWith( "char.spectralaxis.unit" ) ) {
                        specunitscol = k;
                    }
                    else if ( utype.endsWith( "char.fluxaxis.name" ) ) {
                        fluxaxiscol = k;
                    }
                    else if ( utype.endsWith( "char.fluxaxis.accuracy.staterror" ) ) {
                        fluxerrorcol = k;
                    }
                    else if ( utype.endsWith( "char.fluxaxis.unit" ) ) {
                        fluxunitscol = k;
                    }
                }
            }

            //  If we have a DATA_LINK column, gather the URLs it contains
            //  that are appropriate.
            if ( linkcol != -1 ) {
                RowSequence rseq = null;
                SpectrumIO.Props props;
                String value;
                String[] axes;
                String[] units;
                try {
                    if ( ! selected && selection == null ) {
                        //  Using all rows.
                        rseq = starTable.getRowSequence();
                        while ( rseq.next() ) {
                            value = ((String)rseq.getCell( linkcol )).trim();
                            props = new SpectrumIO.Props( value );
                            if ( typecol != -1 ) {
                                value = ((String)rseq.getCell(typecol)).trim();
                                props.setType( specDataFactory
                                               .mimeToSPLATType( value ) );
                            }
                            if ( namecol != -1 ) {
                                value = ((String)rseq.getCell(namecol)).trim();
                                props.setShortName( value );
                            }

                            if ( axescol != -1 ) {

                                //  Old style column names.
                                value = ((String)rseq.getCell(axescol)).trim();
                                axes = value.split("\\s");
                                props.setCoordColumn( axes[0] );
                                props.setDataColumn( axes[1] );
                                if ( axes.length == 3 ) {
                                    props.setErrorColumn( axes[2] );
                                }
                            }
                            else {

                                //  Version 1.0 style.
                                if ( specaxiscol != -1 ) {
                                    value = (String)rseq.getCell(specaxiscol);
                                    props.setCoordColumn( value );
                                }
                                if ( fluxaxiscol != -1 ) {
                                    value = (String)rseq.getCell(fluxaxiscol);
                                    props.setDataColumn( value );
                                }
                                if ( fluxerrorcol != -1 ) {
                                    value = (String)rseq.getCell(fluxerrorcol);
                                    props.setErrorColumn( value );
                                }
                            }

                            if ( unitscol != -1 ) {

                                //  Old style column names.
                                value =
                                    ((String)rseq.getCell( unitscol )).trim();
                                units = value.split("\\s");
                                props.setCoordUnits( units[0] );
                                props.setDataUnits( units[1] );
                                //  Error must have same units as data.
                            }
                            else {
                                
                                //  Version 1.0 style.
                                if ( specunitscol != -1 ) {
                                    value = (String)rseq.getCell(specunitscol);
                                    props.setCoordUnits( value );
                                }
                                if ( fluxunitscol != -1 ) {
                                    value = (String)rseq.getCell(fluxaxiscol);
                                    props.setDataUnits( value );
                                }
                            }
                            specList.add( props );
                        }
                    }
                    else {
                        //  Just using selected rows. To do this we step
                        //  through the table and check if that row is the
                        //  next one in the selection (the selection is
                        //  sorted).
                        rseq = starTable.getRowSequence();
                        int k = 0; // Table row
                        int l = 0; // selection index
                        while ( rseq.next() ) {
                            if ( k == selection[l] ) {

                                // Store this one as matches selection.
                                value = ((String)rseq.getCell(linkcol)).trim();
                                props = new SpectrumIO.Props( value );
                                if ( typecol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(typecol)).trim();
                                    props.setType( specDataFactory
                                                   .mimeToSPLATType( value ) );
                                }
                                if ( namecol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(namecol)).trim();
                                    props.setShortName( value );
                                }
                                if ( axescol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(axescol)).trim();
                                    axes = value.split("\\s");
                                    props.setCoordColumn( axes[0] );
                                    props.setDataColumn( axes[1] );
                                }
                                if ( unitscol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(unitscol)).trim();
                                    units = value.split("\\s");
                                    props.setCoordUnits( units[0] );
                                    props.setDataUnits( units[1] );
                                }
                                specList.add( props );

                                //  Move to next selection.
                                l++;
                                if ( l >= selection.length ) {
                                    break;
                                }
                            }
                            k++;
                        }
                    }
                }
                catch (IOException ie) {
                    ie.printStackTrace();
                }
                finally {
                    try {
                        if ( rseq != null ) {
                            rseq.close();
                        }
                    }
                    catch (IOException iie) {
                        // Ignore.
                    }
                }
            }
        }
    }

    /**
     *  Restore a set of previous query results that have been written to a
     *  VOTable. The file name is obtained interactively.
     */
    public void readQueryFromFile()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                readQuery( file );
            }
            catch (SplatException e) {
                new ExceptionDialog( this, e );
            }
        }
    }

    /**
     *  Restore a set of query results from a File. The File should have the
     *  results written previously as a VOTable, with a RESOURCE containing
     *  the various query results as TABLEs.
     */
    protected void readQuery( File file )
        throws SplatException
    {
        VOElement rootElement = null;
        try {
            rootElement = new VOElementFactory().makeVOElement( file );
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open query results file", e );
        }

        //  First element should be a RESOURCE.
        VOElement[] resource = rootElement.getChildren();
        VOStarTable table = null;
        ArrayList tableList = new ArrayList();
        String tagName = null;
        for ( int i = 0; i < resource.length; i++ ) {
            tagName = resource[i].getTagName();
            if ( "RESOURCE".equals( tagName ) ) {

                //  Look for the TABLEs.
                VOElement child[] = resource[i].getChildren();
                for ( int j = 0; j < child.length; j++ ) {
                    tagName = child[j].getTagName();
                    if ( "TABLE".equals( tagName ) ) {
                        try {
                            table = new VOStarTable( (TableElement) child[j] );
                        }
                        catch (IOException e) {
                            throw new SplatException( "Failed to read query result", e );
                        }
                        tableList.add( table );
                    }
                }
            }
        }
        if ( tableList.size() > 0 ) {
            makeResultsDisplay( tableList );
        }
        else {
            throw new SplatException( "No query results found" );
        }
    }

    /**
     *  Interactively get a file name and save current query results to it as
     *  a VOTable.
     */
    public void saveQueryToFile()
    {
        if ( starJTables == null || starJTables.size() == 0 ) {
            JOptionPane.showMessageDialog( this,
                                           "There are no queries to save",
                                           "No queries", JOptionPane.ERROR_MESSAGE );
            return;
        }

        initFileChooser();
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                saveQuery( file );
            }
            catch (SplatException e) {
                new ExceptionDialog( this, e );
            }
        }
    }

    /**
     *  Save current query to a File, writing the results as a VOTable.
     */
    protected void saveQuery( File file )
        throws SplatException
    {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( file ) );
        }
        catch (IOException e) {
            throw new SplatException( e );
        }
        saveQuery( writer );
        try {
            writer.close();
        }
        catch (IOException e) {
            throw new SplatException( e );
        }
    }

    /**
     *  Save current query results to a BufferedWriter. The resulting document is a
     *  VOTable with a RESOURCE that contains a TABLE for each set of query results.
     */
    protected void saveQuery( BufferedWriter writer )
        throws SplatException
    {
        String xmlDec = VOTableWriter.DEFAULT_XML_DECLARATION;
        try {
            writer.write( xmlDec );
            writer.newLine();
            writer.write( "<VOTABLE version='1.1'>" );
            writer.newLine();
            writer.write( "<RESOURCE>" );
            writer.newLine();

            StarJTable starJTable = null;
            StarTable table = null;
            VOSerializer serializer = null;
            Iterator i = starJTables.iterator();
            while ( i.hasNext() ) {
                starJTable = (StarJTable) i.next();
                table = starJTable.getStarTable();
                serializer = VOSerializer.makeSerializer( DataFormat.TABLEDATA, table );

                //  Write <TABLE> element.
                serializer.writeInlineTableElement( writer );
            }
            writer.write( "</RESOURCE>" );
            writer.newLine();
            writer.write( "</VOTABLE>" );
            writer.newLine();
            writer.close();
        }
        catch (IOException e) {
                throw new SplatException( "Failed to save queries", e );
        }
    }

    /**
     * Return a StarTable of the currently selected tab of query results.
     */
    public StarTable getCurrentTable()
    {
        if ( starJTables != null && starJTables.size() > 0 ) {
            int index = resultsPane.getSelectedIndex();
            if ( index > -1 ) {
                StarJTable jTable = (StarJTable) starJTables.get( index );
                return jTable.getStarTable();
            }
        }
        return null;
    }

    /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser()
    {
        if ( fileChooser == null ) {
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for XML files.
            BasicFileFilter xmlFileFilter =
                new BasicFileFilter( "xml", "XML files" );
            fileChooser.addChoosableFileFilter( xmlFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
    }

    /**
     * Set the proxy server and port.
     */
    protected void showProxyDialog()
    {
        if ( proxyWindow == null ) {
            ProxySetupFrame.restore( null );
            proxyWindow = new ProxySetupFrame();
        }
        proxyWindow.setVisible( true );
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "SSAQueryBrowser" );
        this.dispose();
    }

    /**
     * Configure the SSA servers.
     */
    protected void showServerWindow()
    {
        if ( serverWindow == null ) {
            serverWindow = new SSAServerFrame( serverList );
        }
        serverWindow.setVisible( true );
    }

    public static void main( String[] args )
    {
        try {
            SSAQueryBrowser b =
                new SSAQueryBrowser( new SSAServerList(), null );
            b.pack();
            b.setVisible( true );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    // ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        if ( source.equals( nameLookup ) || source.equals( nameField ) ) {
            resolveName();
            return;
        }

        if ( source.equals( radiusField ) || source .equals( goButton ) ) {
            doQuery();
            return;
        }

        if ( source.equals( displaySelectedButton ) ) {
            displaySpectra( true, null, -1 );
            return;
        }
        if ( source.equals( displayAllButton ) ) {
            displaySpectra( false, null, -1 );
            return;
        }
    }

    //
    // MouseListener interface. Double clicks display the clicked spectrum.
    //
    public void mousePressed( MouseEvent e ) {}
    public void mouseReleased( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}
    public void mouseClicked( MouseEvent e )
    {
        if ( e.getClickCount() == 2 ) {
            StarJTable table = (StarJTable) e.getSource();
            Point p = e.getPoint();
            int row = table.rowAtPoint( p );
            displaySpectra( false, table, row );
        }
    }

    //
    //  Action for switching name resolvers.
    //
    class ResolverAction
        extends AbstractAction
    {
        SkycatCatalog resolver = null;
        public ResolverAction( String name, SkycatCatalog resolver )
        {
            super( name );
            this.resolver = resolver;
        }
        public void actionPerformed( ActionEvent e )
        {
            resolverCatalogue = resolver;
        }
    }

    //
    // LocalAction to encapsulate all trivial local Actions into one class.
    //
    class LocalAction
        extends AbstractAction
    {
        //  Types of action.
        public static final int PROXY = 0;
        public static final int SERVER = 1;
        public static final int SAVE = 2;
        public static final int READ = 3;
        public static final int CLOSE = 4;

        //  The type of this instance.
        private int actionType = PROXY;

        public LocalAction( int actionType, String name )
        {
            super( name );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, Icon icon,
                            String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, Icon icon,
                            String help, String accel )
        {
            this( actionType, name, icon, help );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( accel ) );
        }

        public void actionPerformed( ActionEvent ae )
        {
            switch ( actionType )
            {
                case PROXY: {
                    showProxyDialog();
                    break;
                }
                case SERVER: {
                    showServerWindow();
                    break;
                }
                case SAVE: {
                    saveQueryToFile();
                    break;
                }
                case READ: {
                    readQueryFromFile();
                    break;
                }
                case CLOSE: {
                    closeWindowEvent();
                    break;
                }
            }
        }
    }
}