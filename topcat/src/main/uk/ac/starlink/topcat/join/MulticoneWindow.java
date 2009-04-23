package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.vo.ConeSearch;
import uk.ac.starlink.vo.KeywordServiceQueryFactory;
import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegistryPanel;

/**
 * Window for executing a multiple cone search type match between an
 * input table and a cone search service.
 *
 * @author   Mark Taylor
 * @since    21 Apr 2009
 */
public class MulticoneWindow extends AuxWindow {

    /**
     * Constructor.
     *
     * @param   parent  parent component
     */
    public MulticoneWindow( Component parent ) {
        super( "Multiple Cone Search", parent );

        JProgressBar progBar = placeProgressBar();
        final MulticonePanel mcPanel = new MulticonePanel( progBar );
        final RegistryPanel regPanel =
            new RegistryPanel( new KeywordServiceQueryFactory(
                                       RegCapabilityInterface.CONE_STDID ),
                               true );
        ListSelectionListener coneSelListener = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                RegResource[] resources =
                    regPanel.getSelectedResources();
                RegCapabilityInterface[] capabilities =
                    regPanel.getSelectedCapabilities();
                if ( resources.length == 1 && capabilities.length == 1 ) {
                    ConeSearch cs =
                        new ConeSearch( resources[ 0 ], capabilities[ 0 ] );
                    mcPanel.setServiceUrl( cs.getServiceURL().toString() );
                }
            }
        };
        regPanel.getResourceSelectionModel()
                .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel.getCapabilitySelectionModel()
                .setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        regPanel.getResourceSelectionModel()
                .addListSelectionListener( coneSelListener );
        regPanel.getCapabilitySelectionModel()
                .addListSelectionListener( coneSelListener );

        regPanel.setBorder(
            BorderFactory.createCompoundBorder(
                makeTitledBorder( "Available Cone Search Services" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        mcPanel.setBorder(
            BorderFactory.createCompoundBorder(
                makeTitledBorder( "Multicone Parameters" ),
                BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        regPanel.setPreferredSize( new Dimension( 600, 350 ) );

        JComponent main = getMainArea();
        main.add( regPanel, BorderLayout.CENTER );
        main.add( mcPanel, BorderLayout.SOUTH );

        JComponent controls = getControlPanel();
        controls.add( new JButton( mcPanel.getStartAction() ) );
        controls.add( new JButton( mcPanel.getStopAction() ) );

        getJMenuBar().add( regPanel.makeColumnVisibilityMenu( "Columns" ) );

        addHelp( "MulticoneWindow" );
    }
}