package uk.ac.starlink.topcat.contrib.cds;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * Vizier mode that allows the user to search for catalogues based on 
 * a provided list of known categories.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public class CategoryVizierMode extends SearchVizierMode {

    private final VizierInfo vizinfo_;
    private final JList lambdaList_;
    private final JList missionList_;
    private final JList astroList_;

    /**
     * Constructor.
     *
     * @param   vizinfo  vizier query interface
     * @param   tld  controlling load dialogue
     */
    public CategoryVizierMode( VizierInfo vizinfo,
                               VizierTableLoadDialog tld ) {
        super( "By Category", vizinfo, tld, true );
        vizinfo_ = vizinfo;
        lambdaList_ = new JList();
        missionList_ = new JList();
        astroList_ = new JList();
    }

    protected Component createSearchComponent() {
        final JComponent[] listBoxes = new JComponent[] {
            createListBox( "Wavelength", lambdaList_ ),
            createListBox( "Mission", missionList_ ),
            createListBox( "Astronomy", astroList_ ),
        };
        final JComponent kwPanel = new Box( BoxLayout.X_AXIS ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                for ( int i = 0; i < listBoxes.length; i++ ) {
                    listBoxes[ i ].setEnabled( enabled );
                }
            }
        };
        for ( int i = 0; i < listBoxes.length; i++ ) {
            kwPanel.add( listBoxes[ i ] );
        }
        kwPanel.addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {
                kwPanel.removeAncestorListener( this );
                populateLists();
            }
            public void ancestorMoved( AncestorEvent evt ) {
            }
            public void ancestorRemoved( AncestorEvent evt ) {
            }
        } );
        return kwPanel;
    }

    protected String getSearchArgs() {
        return new StringBuffer()
              .append( getKwArgs( "Wavelength", lambdaList_ ) )
              .append( getKwArgs( "Mission", missionList_ ) )
              .append( getKwArgs( "Astronomy", astroList_ ) )
              .toString();
    }

    /**
     * Fill in the contents of the lists of provided categories.
     * This method can be called from the event dispatch thread.
     */
    private void populateLists() {
        new Thread( "Vizier KW acquisition" ) {
            public void run() {
                final String[] lambdas = vizinfo_.getWavelengthKws();
                final String[] missions = vizinfo_.getMissionKws();
                final String[] astros = vizinfo_.getAstronomyKws();
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        lambdaList_.setListData( lambdas );
                        missionList_.setListData( missions );
                        astroList_.setListData( astros );
                        ((JComponent) getComponent()).revalidate();
                    }
                } );
            }
        }.start();
    }

    /**
     * Returns a VizieR server-friendly argument string which describes
     * the current selections in a JList of categories of a particular
     * kind.
     *
     * @param   name  VizieR server tag for the category type
     * @param   list  JList containing items in category <code>name</code>
     */
    private static String getKwArgs( String name, JList list ) {
        Object[] selections = list.getSelectedValues();
        String key = "-kw." + name;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < selections.length; i++ ) {
            Object sel = selections[ i ];
            assert sel instanceof String;
            if ( sel instanceof String ) {
                sbuf.append( VizierTableLoadDialog
                            .encodeArg( key, (String) sel ) );
            }
        }
        return sbuf.toString();
    }

    /**
     * Creates a component containing a JList of categories
     * suitable for display in this component.
     *
     * @param  name   human-readable name for the category type
     * @param  list   list object
     * @return   component for display
     */
    private static JComponent createListBox( String name, final JList list ) {
        final JLabel label = new JLabel( name );
        final JScrollPane scroller =
            new JScrollPane( list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        JComponent box = new JPanel( new BorderLayout() ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                label.setEnabled( enabled );
                scroller.setEnabled( enabled );
                scroller.getVerticalScrollBar().setEnabled( enabled );
                list.setEnabled( enabled );
            }
        };
        box.add( label, BorderLayout.NORTH );
        box.add( scroller, BorderLayout.CENTER );
        box.setAlignmentY( Component.TOP_ALIGNMENT );
        list.setVisibleRowCount( 6 );
        return box;
    }
}