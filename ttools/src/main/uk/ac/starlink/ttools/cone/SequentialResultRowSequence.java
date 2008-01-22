package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Straightforward implementation of ConeResultRowSequence based on a
 * ConeQueryRowSequence.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2008
 */
public class SequentialResultRowSequence implements ConeResultRowSequence {

    private final ConeQueryRowSequence querySeq_;
    private final ConeSearcher coneSearcher_;
    private final boolean bestOnly_;

    /**
     * Constructor.
     *
     * @param  querySeq  sequence providing cone search query parameters
     * @param  coneSearcher  cone search implementation
     * @param  bestOnly  whether all results or just best are required
     */
    public SequentialResultRowSequence( ConeQueryRowSequence querySeq,
                                        ConeSearcher coneSearcher,
                                        boolean bestOnly ) {
        querySeq_ = querySeq;
        coneSearcher_ = coneSearcher;
        bestOnly_ = bestOnly;
    }

    public StarTable getConeResult() throws IOException {
        return SkyConeMatch2Producer.getConeResult( coneSearcher_, bestOnly_,
                                                    querySeq_.getRa(),
                                                    querySeq_.getDec(),
                                                    querySeq_.getRadius() );
    }

    public boolean next() throws IOException {
        return querySeq_.next();
    }

    public Object getCell( int icol ) throws IOException {
        return querySeq_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return querySeq_.getRow();
    }

    public void close() throws IOException {
        querySeq_.close();
    }

    public double getDec() throws IOException {
        return querySeq_.getDec();
    }

    public double getRa() throws IOException {
        return querySeq_.getRa();
    }

    public double getRadius() throws IOException {
        return querySeq_.getRadius();
    }
}