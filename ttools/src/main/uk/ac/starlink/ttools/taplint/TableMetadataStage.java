package uk.ac.starlink.ttools.taplint;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.util.CountMap;
import uk.ac.starlink.vo.ColumnMeta;
import uk.ac.starlink.vo.ForeignMeta;
import uk.ac.starlink.vo.TableMeta;

/**
 * Validation stage for checking the content of parsed Table metadata.
 * Concrete subclasses must provide a method to acquire the metadata
 * as an array of TableMeta objects.
 *
 * @author   Mark Taylor
 * @since    3 Jun 2011
 */
public abstract class TableMetadataStage implements Stage {

    private final String srcDescription_;
    private final String[] knownColFlags_;
    private final boolean reportOtherFlags_;
    private TableMeta[] tmetas_;
    private static final String[] KNOWN_COL_FLAGS =
        new String[] { "indexed", "primary", "nullable" };

    /**
     * Constructor.
     *
     * @param   srcDescription  short text description of table metadata source
     * @param   knownColFlags  standard column flag values to report counts for
     * @param   reportOtherFlags  whether to report counts for non-standard
     *                            column flag values
     */
    public TableMetadataStage( String srcDescription, String[] knownColFlags,
                               boolean reportOtherFlags ) {
        srcDescription_ = srcDescription;
        knownColFlags_ = knownColFlags;
        reportOtherFlags_ = reportOtherFlags;
    }

    public String getDescription() {
        return "Check content of tables metadata from " + srcDescription_;
    }

    /**
     * Returns a short text description of table metadata source.
     * 
     * @return  metadata source description
     */
    public String getSourceDescription() {
        return srcDescription_;
    }

    /**
     * Returns the table metadata obtained by the last run of this stage.
     *
     * @return  table metadata array
     */
    public TableMeta[] getTableMetadata() {
        return tmetas_;
    }
   
    /**
     * Returns an array providing table metadata to check.
     *
     * @param  serviceUrl  TAP service URL
     * @param  reporter   destination for validation messages
     */
    protected abstract TableMeta[] readTableMetadata( URL serviceUrl,
                                                      Reporter reporter );

    public void run( URL serviceUrl, Reporter reporter ) {
        TableMeta[] tmetas = readTableMetadata( serviceUrl, reporter );
        checkTables( reporter, tmetas );
        tmetas_ = tmetas;
    }

    /**
     * Performs the checking and reporting for a given table metadata set.
     *
     * @param  reporter  destination for validation messages
     * @param  tmetas   table metadata to check
     */
    private void checkTables( Reporter reporter, TableMeta[] tmetas ) { 
        if ( tmetas == null ) {
            reporter.report( Reporter.Type.WARNING, "GONE",
                             "Table metadata absent" );
            return;
        }
        int nTable = tmetas.length;
        int nCol = 0;
        Map<String,TableMeta> tableMap = 
            createNameMap( "table", 'T', tmetas, reporter );
        Map<String,Map<String,ColumnMeta>> colsMap =
            new HashMap<String,Map<String,ColumnMeta>>();
        for ( String tname : tableMap.keySet() ) {
            TableMeta tmeta = tableMap.get( tname );
            ColumnMeta[] cols = tmeta.getColumns();
            nCol += cols.length; 
            Map<String,ColumnMeta> cmap =
                createNameMap( "column", 'C', cols, reporter );
            colsMap.put( tname, cmap );
        }
        int nForeign = 0;
        int nIndex = 0;
        CountMap<String> flagMap = new CountMap<String>();
        for ( String tname : tableMap.keySet() ) {
            TableMeta tmeta = tableMap.get( tname );
            Map<String,ColumnMeta> cmap = colsMap.get( tname );
            for ( String cname : cmap.keySet() ) {
                ColumnMeta col = cmap.get( cname );
                if ( col.isIndexed() ) {
                    nIndex++;
                }
                String[] flags = col.getFlags();
                for ( int ig = 0; ig < flags.length; ig++ ) {
                    flagMap.addItem( flags[ ig ] );
                }
            }
            ForeignMeta[] fkeys = tmeta.getForeignKeys();
            nForeign += fkeys.length;
            for ( int ik = 0; ik < fkeys.length; ik++ ) {
                ForeignMeta fkey = fkeys[ ik ];
                String targetTableName = fkey.getTargetTable();
                TableMeta targetTable = tableMap.get( targetTableName );
                if ( targetTable == null ) {
                    String msg = new StringBuffer()
                       .append( "Non-existent target table " )
                       .append( fkey.getTargetTable() )
                       .append( " for foreign key in table " )
                       .append( tmeta )
                       .toString();
                    reporter.report( Reporter.Type.ERROR, "FKNT", msg );
                }
                else {
                    Map<String,ColumnMeta> targetCmap =
                        colsMap.get( targetTableName );
                    ForeignMeta.Link[] links = fkey.getLinks();
                    for ( int il = 0; il < links.length; il++ ) {
                        ForeignMeta.Link link = links[ il ];
                        ColumnMeta fromCol = cmap.get( link.getFrom() );
                        ColumnMeta toCol = targetCmap.get( link.getTarget() );
                        if ( fromCol == null || toCol == null ) {
                            reporter.report( Reporter.Type.ERROR, "FKLK",
                                             "Broken link " + link
                                           + " in foreign key " + fkey );
                        }
                        String fromType = fromCol.getDataType();
                        String toType = toCol.getDataType();
                        if ( fromType == null || ! fromType.equals( toType ) ) {
                            String msg = new StringBuffer()
                                .append( "Type mismatch for link " )
                                .append( link )
                                .append( " in foreign key " )
                                .append( fkey )
                                .append( "; " )
                                .append( fromType )
                                .append( " vs. " )
                                .append( toType )
                                .toString();
                            reporter.report( Reporter.Type.WARNING,
                                             "FTYP", msg );
                        }
                    }
                }
            }
        }
        Collection<String> otherFlagList =
            new HashSet<String>( flagMap.keySet() );
        otherFlagList.removeAll( Arrays.asList( knownColFlags_ ) );
        String[] otherFlags = otherFlagList.toArray( new String[ 0 ] );
        reporter.report( Reporter.Type.SUMMARY, "SUMM",
                         "Tables: " + nTable + ", "
                       + "Columns: " + nCol + ", "
                       + "Foreign Keys: " + nForeign );
        reporter.report( Reporter.Type.SUMMARY, "FLGS",
                         "Standard column flags: "
                       + summariseCounts( flagMap, knownColFlags_ ) );
        if ( reportOtherFlags_ ) {
            reporter.report( Reporter.Type.SUMMARY, "FLGO",
                             "Other column flags: "
                           + summariseCounts( flagMap, otherFlags ) );
        }
    }

    /**
     * Summarises how many entries in a given map there are for each of
     * a given set of keys.
     *
     * @param  countMap   map containing counts
     * @param  keys   keys to be summarised
     * @return   summary string
     */
    private String summariseCounts( CountMap<String> countMap, String[] keys ) {
        if ( keys.length == 0 ) {
            return "none";
        }
        StringBuffer sbuf = new StringBuffer();
        for ( int ik = 0; ik < keys.length; ik++ ) {
            String key = keys[ ik ];
            if ( ik > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( key )
                .append( ": " )
                .append( countMap.getCount( key ) );
        }
        return sbuf.toString();
    }

    /**
     * Creates a map from an array of objects.
     * The map keys are the results of calling <code>toString</code> on the
     * objects.  Any duplicates will be reported through the supplied
     * reporter.
     *
     * @param   dName  descriptive name of type of thing in map
     * @param   dChr   single character labelling type of thing
     * @param   values  objects to populate map
     * @param   reporter   destination for validation messages
     * @return   name -> value map
     */
    private <V> Map<String,V> createNameMap( String dName, char dChr,
                                             V[] values, Reporter reporter ) {
        Map<String,V> map = new LinkedHashMap<String,V>();
        for ( int iv = 0; iv < values.length; iv++ ) {
            V value = values[ iv ];
            String name = value.toString();
            if ( name == null || name.trim().length() == 0 ) {
                reporter.report( Reporter.Type.ERROR, "" + dChr + "BLA",
                                 "Blank name for " + dName + " #" + iv );
            }
            if ( ! map.containsKey( name ) ) {
                map.put( name, value );
            }
            else {
                reporter.report( Reporter.Type.WARNING, "" + dChr + "DUP",
                                 "Duplicate " + dName + " \"" + name + "\"" );
            }
        }
        return map;
    }
}