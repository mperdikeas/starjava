package uk.ac.starlink.topcat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.IntList;

/**
 * Performs encoding and decoding for TopcatModels in order to 
 * perform per-table session save/restore.  This class translates 
 * between a TopcatModel and a StarTable; the StarTable can be 
 * de/serialized using one of the standard STIL I/O handlers
 * (probably a VOTable-based one since there will be significant
 * amounts of metadata).
 *
 * <p>This class is currently a singleton.
 *
 * @author   Mark Taylor
 * @since    16 Jul 2010
 */
public class TopcatCodec {

    private static final TopcatCodec instance_ = new TopcatCodec();
    private static final String TC_PREFIX = "topcat:";
    private static final ValueInfo IS_TCMODEL_INFO =
        createInfo( "isTopcatModel", Boolean.class );
    private static final ValueInfo COLS_INDEX_INFO =
        createInfo( "columnIndices", int[].class );
    private static final ValueInfo COLS_VISIBLE_INFO =
        createInfo( "columnVisibilities", boolean[].class );
    private static final ValueInfo LABEL_INFO =
        createInfo( "label", String.class );
    private static final ValueInfo SEND_ROWS_INFO =
        createInfo( "broadcastRows", Boolean.class );
    private static final ValueInfo VERSION_INFO =
        createInfo( "saveVersion", String.class );
    private static final ValueInfo SORT_COLUMN_INFO =
        createInfo( "sortColumn", Integer.class );
    private static final ValueInfo SORT_SENSE_INFO =
        createInfo( "sortSense", Boolean.class );
    private static final ValueInfo SUBSET_NAMES_INFO =
        createInfo( "rowSubsetNames", String[].class );
    private static final ValueInfo SUBSET_FLAGS_INFO =
        createInfo( "rowSubsetFlags", null );
    private static final ValueInfo CURRENT_SUBSET_INFO =
        createInfo( "currentSubset", Integer.class );
    static {
        ((DefaultValueInfo) SUBSET_FLAGS_INFO).setNullable( false );
    }
    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Private constructor prevents public instantiation.
     */
    private TopcatCodec() {
    }

    /**
     * Turns a TopcatModel into a StarTable, ready for serialization.
     *
     * @param  tcModel  model
     * @return   table
     */
    public StarTable encode( TopcatModel tcModel ) {

        /* Prepare table data and metadata for use and adjustment. */
        final StarTable dataModel = tcModel.getDataModel();
        List<DescribedValue> paramList =
            new ArrayList<DescribedValue>( dataModel.getParameters() );
        long nrow = dataModel.getRowCount();
        ColumnStarTable extraTable = ColumnStarTable.makeTableWithRows( nrow );

        /* Mark as serialized TopcatModel. */
        paramList.add( new DescribedValue( IS_TCMODEL_INFO, Boolean.TRUE ) );
        paramList.add( new DescribedValue( VERSION_INFO,
                                           TopcatUtils.getVersion() ) );

        /* Record label. */
        paramList.add( new DescribedValue( LABEL_INFO, tcModel.getLabel() ) );

        /* Record column sequences. */
        ColumnList colList = tcModel.getColumnList();
        int nCol = colList.size();
        int[] icols = new int[ nCol ];
        boolean[] activs = new boolean[ nCol ];
        for ( int jc = 0; jc < nCol; jc++ ) {
            icols[ jc ] = colList.getColumn( jc ).getModelIndex();
            activs[ jc ] = colList.isActive( jc );
        }
        paramList.add( new DescribedValue( COLS_INDEX_INFO, icols ) );
        paramList.add( new DescribedValue( COLS_VISIBLE_INFO, activs ) );

        /* Record whether to broadcast rows. */
        paramList.add( new DescribedValue( SEND_ROWS_INFO,
                                           tcModel.getRowSendModel()
                                                  .isSelected() ) );

        /* Record sort order. */
        SortOrder sortOrder = tcModel.getSelectedSort();
        TableColumn sortCol = sortOrder == null ? null : sortOrder.getColumn();
        if ( sortCol != null ) {
            int icolSort = tcModel.getColumnList().indexOf( sortCol );
            if ( icolSort >= 0 ) {
                boolean sense = tcModel.getSortSenseModel().isSelected();
                paramList.add( new DescribedValue( SORT_COLUMN_INFO,
                                                   new Integer( icolSort ) ) );
                paramList.add( new DescribedValue( SORT_SENSE_INFO,
                                                   Boolean.valueOf( sense ) ) );
            }
        }

        /* Store row subset flags in a new column. */
        List<RowSubset> subsetList =
            new ArrayList<RowSubset>( tcModel.getSubsets() );
        boolean hadAll = subsetList.remove( RowSubset.ALL );
        assert hadAll;
        RowSubset[] subsets = subsetList.toArray( new RowSubset[ 0 ] );
        if ( subsets.length > 0 ) {
            String[] subsetNames = new String[ subsets.length ];
            for ( int is = 0; is < subsets.length; is++ ) {
                subsetNames[ is ] = subsets[ is ].getName();
            }
            paramList.add( new DescribedValue( SUBSET_NAMES_INFO,
                           subsetNames ) );
            Object flagsArray = createFlagsArray( dataModel, subsets );
            if ( flagsArray != null ) {
                ColumnData flagsCol =
                    ArrayColumn
                   .makeColumn( SUBSET_FLAGS_INFO.getName(), flagsArray );
                ColumnInfo info = new ColumnInfo( SUBSET_FLAGS_INFO );
                info.setContentClass( flagsCol.getColumnInfo()
                                              .getContentClass() );
                flagsCol.setColumnInfo( info );
                extraTable.addColumn( flagsCol );
            }
        }

        /* Record current subset. */
        int iset = subsetList.indexOf( tcModel.getSelectedSubset() );
        if ( iset >= 0 ) {
            paramList.add( new DescribedValue( CURRENT_SUBSET_INFO,
                                               new Integer( iset ) ) );
        }

        /* Prepare the output table object. */
        List<StarTable> joinList = new ArrayList<StarTable>();
        joinList.add( dataModel );
        if ( extraTable.getColumnCount() > 0 ) {
            joinList.add( extraTable );
        }
        StarTable outTable =
            new JoinStarTable( joinList.toArray( new StarTable[ 0 ] ) );

        /* Set its parameters; make sure this does not overwrite the
         * parameters of the original object. */
        outTable.getParameters().clear();
        outTable.getParameters().addAll( paramList );

        /* Return the result. */
        return outTable;
    }

    /**
     * Takes a table which has been previously serialized by calling 
     * this class's {@link #encode} method, and turns it into a TopcatModel.
     * If it looks like the table is not one which was the result of an
     * earlier <code>encode</code> call, null will be returned.
     * Should be called from the AWT event dispatch thread.
     *
     * @param  table  encoded table
     * @param  location  table location string
     * @param  controlWindow  control window
     * @return   topcat model, or null
     */
    public TopcatModel decode( StarTable table, String location,
                               ControlWindow controlWindow ) {
        try {
            return doDecode( table, location, controlWindow );
        }
        catch ( RuntimeException e ) {
            logger_.warning( "Error parsing TOPCAT session file: " + e );
            return null;
        }
    }

    /**
     * Does the work for the decoding.  May throw an unchecked exception,
     * for instance a ClassCastException if certain metadata items are
     * present but have the wrong type (not likely excepting deliberate
     * sabotage, but conceivable).
     *
     * @param  table  encoded table
     * @param  location  table location string
     * @param  controlWindow  control window
     * @return   topcat model, or null
     */
    private TopcatModel doDecode( StarTable inTable, String location,
                                  ControlWindow controlWindow ) {
        CodecTable codec = new CodecTable( inTable );

        /* Determine if this is a TopcatModel encoded by this class. */
        if ( ! Boolean.TRUE.equals( codec.getCodecValue( IS_TCMODEL_INFO ) ) ) {
            return null;
        }
        TopcatModel tcModel =
            TopcatModel.createRawTopcatModel( codec.getDataTable(), location,
                                              controlWindow );

        /* Get label. */
        tcModel.setLabel( (String) codec.getCodecValue( LABEL_INFO ) );

        /* Get columns.  This is a bit involved, since a TopcatModel has
         * both a TableColumnModel and a ColumnList which must be updated
         * in a consistent way, to reflect the column order and which
         * columns are currently visible. */
        /* First get a record of the order of columns and their visibility. */
        int[] icols = (int[]) codec.getCodecValue( COLS_INDEX_INFO );
        boolean[] activs = (boolean[]) codec.getCodecValue( COLS_VISIBLE_INFO );
        TableColumnModel colModel = tcModel.getColumnModel();
        ColumnList colList = tcModel.getColumnList();
        int ncol = colModel.getColumnCount();
        assert ncol == colList.size();
        TableColumn[] tcols = new TableColumn[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            tcols[ ic ] = colList.getColumn( icols[ ic ] );
        }

        /* Reorder the columns in the TableColumnModel to match the saved
         * order.  This has the effect of updating the ColumnList as well,
         * since it is a listener. */
        for ( int ic = 0; ic < ncol; ic++ ) {
            TableColumn tcol = tcols[ ic ];
            if ( colModel.getColumn( ic ) != tcol ) {
                int kc = -1;
                for ( int jc = ic; jc < ncol && kc < 0; jc++ ) {
                    if ( colModel.getColumn( jc ) == tcol ) {
                        kc = jc;
                    }
                }
                assert kc >= 0;
                colModel.moveColumn( kc, ic );
            }
        }
        for ( int ic = 0; ic < ncol; ic++ ) {
            assert colModel.getColumn( ic ) == tcols[ ic ];
            assert colList.getColumn( ic ) == tcols[ ic ];
        }

        /* Finally flag each column as visible or not, according to the saved
         * state. */
        for ( int ic = 0; ic < ncol; ic++ ) {
            colList.setActive( ic, activs[ ic ] );
        }

        /* Get whether to broadcast rows. */
        tcModel.getRowSendModel()
               .setSelected( Boolean.TRUE
                            .equals( codec.getCodecValue( SEND_ROWS_INFO ) ) );

        /* Get current subset index. */
        Integer indexCurrentSubset =
            (Integer) codec.getCodecValue( CURRENT_SUBSET_INFO );
        int iCurrentSubset = indexCurrentSubset != null
                           ? indexCurrentSubset.intValue()
                           : -1;

        /* Get subsets. */
        String[] subsetNames =
            (String[]) codec.getCodecValue( SUBSET_NAMES_INFO );
        if ( subsetNames != null && subsetNames.length > 0 ) {
            RowSubset currentSubset = null;
            int nset = subsetNames.length;
            int icolSubsets = codec.getCodecColumnIndex( SUBSET_FLAGS_INFO );
            List<RowSubset> setList = new ArrayList<RowSubset>();
            for ( int is = 0; is < nset; is++ ) {
                RowSubset rset = createRowSubset( subsetNames[ is ], inTable,
                                                  icolSubsets, is );
                if ( rset != null ) {
                    setList.add( rset );
                }
                if ( is == iCurrentSubset ) {
                    currentSubset = rset;
                }
            }
            for ( RowSubset rset : setList ) {
                tcModel.addSubset( rset );
            }
            if ( currentSubset != null ) {
                tcModel.applySubset( currentSubset );
            }
        }

        /* Get sort order. */
        Integer icolSort = (Integer) codec.getCodecValue( SORT_COLUMN_INFO );
        if ( icolSort != null ) {
            int icSort = icolSort.intValue();
            boolean sortSense =
                Boolean.TRUE.equals( codec.getCodecValue( SORT_SENSE_INFO ) );
            TableColumn tcolSort = colList.getColumn( icSort );
            tcModel.getSortSenseModel().setSelected( sortSense );
            tcModel.sortBy( new SortOrder( tcolSort ), sortSense );
        }

        /* Return result. */
        return tcModel;
    }

    /**
     * Returns a ValueInfo which describes a particular metadata item
     * suitable for use with this codec.
     *
     * @param   unique, but not namespaced, name for the metadata item
     * @param   clazz  class of value which will be stored under this item
     * @return   new metadata description object
     */
    private static ValueInfo createInfo( String name, Class clazz ) {
        DefaultValueInfo info = clazz == null
                              ? new DefaultValueInfo( name )
                              : new DefaultValueInfo( name, clazz );
        info.setUtype( TC_PREFIX + name );
        return info;
    }

    /**
     * Generates an array of some kind of data item (probably an integer
     * type or possibly integer array) suitable for placing in a table
     * column.  This array has the same number of elements as the table
     * has rows, and encodes the content of the supplied subsets.
     *
     * @param  table  input table
     * @param  subsets  subsets applying to table
     * @return   nrow-element array
     */
    private Object createFlagsArray( StarTable table, RowSubset[] subsets ) {
        int nrow = Tables.checkedLongToInt( table.getRowCount() );
        int nset = subsets.length;
        if ( nset < 16 ) {
            short[] flags = new short[ nrow ];
            for ( int irow = 0; irow < nrow; irow++ ) {
                int flag = 0;
                for ( int iset = nset - 1; iset >= 0; iset-- ) {
                    flag <<= 1;
                    if ( subsets[ iset ].isIncluded( irow ) ) {
                        flag = flag | 1;
                    }
                }
                flags[ irow ] = (short) flag;
            }
            return flags;
        }
        else if ( nset < 32 ) {
            int[] flags = new int[ nrow ];
            for ( int irow = 0; irow < nrow; irow++ ) {
                int flag = 0;
                for ( int iset = nset - 1; iset >= 0; iset-- ) {
                    flag <<=  1;
                    if ( subsets[ iset ].isIncluded( irow ) ) {
                        flag = flag | 1;
                    }
                }
                flags[ irow ] = flag;
            }
            return flags;
        }
        else if ( nset < 64 ) {
            long[] flags = new long[ nrow ];
            for ( int irow = 0; irow < nrow; irow++ ) {
                long flag = 0L;
                for ( int iset = nset - 1; iset >= 0; iset-- ) {
                    flag <<= 1;
                    if ( subsets[ iset ].isIncluded( irow ) ) {
                        flag = flag | 1L;
                    }
                }
                flags[ irow ] = flag;
            }
            return flags;
        }
        else {
            logger_.warning( "More than 64 subsets??" ); 
            return null;
        }
    }

    /**
     * Generates a RowSubset from a column like one generated by a call
     * to {@link #createFlagsArray}.
     *
     * @param   name  subset name
     * @param   table   input table
     * @param   icol   index of column containing flag data
     * @param   iflag   index of flag within column
     * @return   iflag'th subset derived from column icol in table
     */
    private RowSubset createRowSubset( String name, final StarTable table,
                                       final int icol, int iflag ) {
        ColumnInfo info = table.getColumnInfo( icol );
        Class clazz = info.getContentClass();
        if ( clazz == Short.class ) {
            final short mask = (short) ( 1 << iflag );
            return new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return ( ((Number) table.getCell( lrow, icol ))
                                          .shortValue() & mask ) != 0;
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            };
        }
        else if ( clazz == Integer.class ) {
            final int mask = 1 << iflag;
            return new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return ( ((Number) table.getCell( lrow, icol ))
                                          .intValue() & mask ) != 0;
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            };
        }
        else if ( clazz == Long.class ) {
            final long mask = 1L << iflag;
            return new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return ( ((Number) table.getCell( lrow, icol ))
                                          .longValue() & mask ) != 0;
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            };
        }
        else {
            logger_.warning( "Can't decode subsets column" );
            return null;
        }
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return   instance
     */
    public static TopcatCodec getInstance() {
        return instance_;
    }

    /**
     * Utility class for separating codec-specific and original-data
     * data and metadata items from a saved table.
     */
    private static class CodecTable {
        private final Map<String,DescribedValue> codecParamMap_;
        private final Map<String,Integer> codecIcolMap_;
        private final StarTable dataTable_;

        /**
         * Constructor.
         *
         * @param   inTable  saved table as read
         */
        public CodecTable( StarTable inTable ) {

            /* Sort out table parameters. */
            codecParamMap_ = new HashMap<String,DescribedValue>();
            List<DescribedValue> dataParamList =
                new ArrayList<DescribedValue>();
            for ( Iterator it = inTable.getParameters().iterator();
                  it.hasNext(); ) {
                DescribedValue param = (DescribedValue) it.next();
                String utype = param.getInfo().getUtype();
                if ( utype != null && utype.startsWith( TC_PREFIX ) ) {
                    codecParamMap_.put( utype, param );
                }
                else {
                    dataParamList.add( param );
                }
            }

            /* Sort out table columns. */
            codecIcolMap_ = new HashMap<String,Integer>();
            IntList dataIcolList = new IntList();
            for ( int icol = 0; icol < inTable.getColumnCount(); icol++ ) {
                ColumnInfo info = inTable.getColumnInfo( icol );
                String utype = info.getUtype();
                if ( utype != null && utype.startsWith( TC_PREFIX ) ) {
                    codecIcolMap_.put( utype, icol );
                }
                else {
                    dataIcolList.add( icol );
                }
            }
	    int[] dataColMap = dataIcolList.toIntArray();

            /* Construct a table containing only the data items. */
            dataTable_ =
                new ColumnPermutedStarTable( inTable, dataColMap, true );
            dataTable_.getParameters().clear();
            dataTable_.getParameters().addAll( dataParamList );
        }

        /**
         * Returns a copy of the input table shorn of any codec-specific
         * data or metadata.
         *
         * @return   data-only table
         */
        public StarTable getDataTable() {
            return dataTable_;
        }

        /**
         * Returns a codec-specific parameter value from the input table.
         *
         * @param  info  metadata description
         * @return   value stored under the given info, or null if absent
         */
        public Object getCodecValue( ValueInfo info ) {
            String utype = info.getUtype();
            DescribedValue dval = codecParamMap_.get( utype );
            return dval == null ? null : dval.getValue();
        }

        /**
         * Returns the column index of a codec-specific column from the
         * input table.
         *
         * @param  info  metadata description
         * @return   column index for the given info, or -1 if absent
         */
        public int getCodecColumnIndex( ValueInfo info ) {
            String utype = info.getUtype();
            return codecIcolMap_.containsKey( utype )
                 ? codecIcolMap_.get( utype )
                 : -1;
        }
    }
}
