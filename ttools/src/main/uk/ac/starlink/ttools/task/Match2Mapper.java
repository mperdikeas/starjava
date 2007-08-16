package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.table.join.TextProgressIndicator;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.JELTable;

/**
 * TableMapper which does the work for pair matching (tmatch2).
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class Match2Mapper implements TableMapper {

    private final MatchEngineParameter matcherParam_;
    private final WordsParameter[] tupleParams_;
    private final JoinTypeParameter joinParam_;
    private final ChoiceParameter modeParam_;
    private final Parameter[] duptagParams_;

    private final static Logger logger =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    public Match2Mapper() {

        matcherParam_ = new MatchEngineParameter( "matcher" );

        tupleParams_ = new WordsParameter[ 2 ];
        duptagParams_ = new Parameter[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            tupleParams_[ i ] = new WordsParameter( "values" + i1 );
            tupleParams_[ i ].setUsage( "<expr-list>" );
            tupleParams_[ i ].setPrompt( "Expressions for match values " +
                                         "from table " + i1 );
            tupleParams_[ i ].setDescription( new String[] {
                "<p>Defines the values from table " + i1,
                "which are used to determine whether a match has occurred.",
                "These will typically be coordinate values such as RA and Dec",
                "and perhaps some per-row error values as well, though exactly",
                "what values are required is determined by the kind of match",
                "as determined by",
                "<code>" + matcherParam_.getName() + "</code>.",
                "Depending on the kind of match, the number and type of",
                "the values required will be different.",
                "Multiple values should be separated by whitespace;",
                "if whitespace occurs within a single value it must be",
                "'quoted' or \"quoted\".",
                "Elements of the expression list are commonly just column",
                "names, but may be algebraic expressions calculated from",
                "zero or more columns as explained in <ref id='jel'/>.",
                "</p>",
            } );

            duptagParams_[ i ] = new Parameter( "duptag" + i1 );
            duptagParams_[ i ].setUsage( "<trail-string>" );
            duptagParams_[ i ].setPrompt( "Column deduplication string " +
                                          "for table " + i1 );
            duptagParams_[ i ].setDefault( "_" + i1 );
            duptagParams_[ i ].setNullPermitted( true );
            duptagParams_[ i ].setDescription( new String[] {
                "<p>If the same column name appears in both of the input",
                "tables, those columns are renamed in the output table",
                "to avoid ambiguity.",
                "The output column name of such a duplicated column",
                "is formed by appending the value of this parameter",
                "to its name in the input table.",
                "</p>",
            } );
        }

        modeParam_ = new ChoiceParameter( "find", new String[] {
            "best", "all",
        } );
        modeParam_.setDefault( "best" );
        modeParam_.setPrompt( "Type of match to perform" );
        modeParam_.setDescription( new String[] {
            "<p>Determines which matches are retained.",
            "If <code>best</code> is selected, then only the best match",
            "between the two tables will be retained; in this case",
            "the data from a row of either input table will appear in",
            "at most one row of the output table.",
            "If <code>all</code> is selected, then all pairs of rows",
            "from the two input tables which match the input criteria",
            "will be represented in the output table.",
            "</p>",
        } );

        joinParam_ = new JoinTypeParameter( "join" );
        joinParam_.setDefault( "1and2" );
        joinParam_.setPrompt( "Selection criteria for output rows" );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            matcherParam_,
            tupleParams_[ 0 ],
            tupleParams_[ 1 ],
            matcherParam_.getMatchParametersParameter(),
            joinParam_,
            modeParam_,
            duptagParams_[ 0 ],
            duptagParams_[ 1 ],
        };
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {

        /* Get the matcher. */
        MatchEngine matcher = matcherParam_.matchEngineValue( env );

        /* Find the number and characteristics of the columns required
         * for this matcher. */
        ValueInfo[] tinfos = matcher.getTupleInfos();
        int ncol = tinfos.length;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < ncol; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ' ' );
            }
            sbuf.append( tinfos[ i ].getName() );
        }
        String colNames = sbuf.toString();

        /* Assemble the arrays of supplied expressions which will supply
         * the values to the matcher for each table. */
        String[][] tupleExprs = new String[ 2 ][];
        for ( int i = 0; i < 2; i++ ) {
            int i1 = i + 1;
            tupleParams_[ i ].setRequiredWordCount( ncol );
            tupleParams_[ i ].setPrompt( "Table " + i1 + " match columns ("
                                       + colNames + ")" );
            tupleExprs[ i ] = tupleParams_[ i ].wordsValue( env );
        }

        /* Get other parameter values. */
        JoinType join = joinParam_.joinTypeValue( env );
        String mode = modeParam_.stringValue( env );
        boolean bestOnly;
        if ( mode.toLowerCase().equals( "best" ) ) {
            bestOnly = true;
        }
        else if ( mode.toLowerCase().equals( "all" ) ) {
            bestOnly = false;
        }
        else {
            throw new UsageException( "Unknown value of " +
                                      modeParam_.getName() + "??" );
        }
        JoinFixAction[] fixacts = new JoinFixAction[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            String duptag = duptagParams_[ i ].stringValue( env );
            fixacts[ i ] = ( duptag == null || duptag.trim().length() == 0 )
                ? JoinFixAction.NO_ACTION
                : JoinFixAction.makeRenameDuplicatesAction( duptag,
                                                            false, true );
        }
        PrintStream logStrm = env.getErrorStream();

        /* Construct and return a mapping based on this lot. */
        return new Match2Mapping( matcher, tupleExprs[ 0 ], tupleExprs[ 1 ],
                                  join, bestOnly,
                                  fixacts[ 0 ], fixacts[ 1 ], logStrm );
    }

    /**
     * TableMapping implementation which does the work of matching two tables.
     */
    private static class Match2Mapping implements TableMapping {

        final String[][] exprTuples_;
        final JoinFixAction[] fixacts_;
        final MatchEngine matchEngine_;
        final boolean bestOnly_;
        final JoinType join_;
        final PrintStream logStrm_;

        /**
         * Constructor.
         *
         * @param   matchEngine  engine defining the match characteristics
         * @param   exprTuple1  array of strings defining the values from
         *          the first input table which are used for input into the
         *          match; each element is a JEL expression evaluated in
         *          the context of the first table
         * @param   exprTuple2  array of strings defining the values from
         *          the second input table which are used for input into the
         *          match; each element is a JEL expression evaluated in
         *          the context of the second table
         * @param   join  output row selection type
         * @param   bestOnly   whether only the best match is to be retained
         * @param   fixact1    deduplication fix action for first input table
         * @param   fixact2    deduplication fix action for second input table
         * @param   logStrm    print stream for logging output
         */
        Match2Mapping( MatchEngine matchEngine, String[] exprTuple1,
                       String[] exprTuple2, JoinType join,
                       boolean bestOnly, JoinFixAction fixact1, 
                       JoinFixAction fixact2, PrintStream logStrm ) {
            matchEngine_ = matchEngine;
            exprTuples_ = new String[][] { exprTuple1, exprTuple2 };
            join_ = join;
            bestOnly_ = bestOnly;
            fixacts_ = new JoinFixAction[] { fixact1, fixact2, };
            logStrm_ = logStrm;
        }

        public StarTable mapTables( InputTableSpec[] inSpecs )
                throws IOException, TaskException {
            StarTable[] inTables = { inSpecs[ 0 ].getWrappedTable(), 
                                     inSpecs[ 1 ].getWrappedTable(), };

            /* Attempt to create the tables containing the tuples with which
             * the match will be done.  This is a dry run, intended to
             * catch any exceptions before the possibly expensive work
             * of randomising the input tables is performed. */
            makeSubTables( inTables );

            /* Now randomise the tables (currently required for the rest
             * of the matching) and create the subtables for real. */
            for ( int i = 0; i < 2; i++ ) {
                inTables[ i ] = Tables.randomTable( inTables[ i ] );
            }
            StarTable[] subTables = makeSubTables( inTables );

            /* Do the match. */
            RowMatcher matcher = new RowMatcher( matchEngine_, subTables );
            matcher.setIndicator( new TextProgressIndicator( logStrm_ ) );
            LinkSet matches;
            try {
                matches = matcher.findPairMatches( bestOnly_ );
                if ( ! matches.sort() ) {
                    logger.warning( "Implementation can't sort rows - "
                                  + "matched table rows may not be ordered" );
                }
            }
            catch ( InterruptedException e ) {
                throw new ExecutionException( e.getMessage(), e );  
            }

             /* Create a new table from the result and return. */
            ValueInfo scoreInfo = matchEngine_.getMatchScoreInfo();
            return MatchStarTables.makeJoinTable( inTables[ 0 ], inTables[ 1 ],
                                                  matches, join_, ! bestOnly_,
                                                  fixacts_, scoreInfo );
        }

        /**
         * Creates the tables containing the values which are required by the
         * matcher.  These typically consist of a few of the columns from
         * the input tables, but in general may come from any JEL expression
         * based on them.  Because JEL compilation is performed here, an
         * exception (rethrown as an ExecutionException) may occur.
         *
         * @param  inTables  input tables
         * @return   match value tables based on <code>inTables</code>
         *           using this mapping's known algebraic expressions
         * @throws  ExecutionException  if a compilation error occurs
         */
        private StarTable[] makeSubTables( StarTable[] inTables )
                throws ExecutionException {

            /* Work out the type of columns we will require from each table. */
            ValueInfo[] tupleInfos = matchEngine_.getTupleInfos();
            int nCoord = tupleInfos.length;

            /* For each table, create a new table containing only columns
             * defined by the supplied expressions. */
            StarTable[] subTables = new StarTable[ inTables.length ];
            for ( int i = 0; i < subTables.length; i++ ) {
                ColumnInfo[] colInfos = new ColumnInfo[ nCoord ];
                for ( int j = 0; j < nCoord; j++ ) {
                    colInfos[ j ] = new ColumnInfo( tupleInfos[ j ] );
                }
                try {
                    subTables[ i ] = new JELTable( inTables[ i ], colInfos,
                                                   exprTuples_[ i ] );
                }
                catch ( CompilationException e ) {
                    throw new ExecutionException( e.getMessage(), e );
                }
            }
            return subTables;
        }
    }
}