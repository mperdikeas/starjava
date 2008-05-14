package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import uk.ac.starlink.table.StarTable;

/**
 * ConeResultRowSequence implementation which uses a fixed number of
 * worker threads to dispatch cone queries in parallel.
 * The result table has the same ordering as if they had been done in 
 * sequence.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2008
 */
public class ParallelResultRowSequence implements ConeResultRowSequence {

    private final ConeQueryRowSequence querySeq_;
    private final ConeSearcher coneSearcher_;
    private final boolean bestOnly_;
    private final String distanceCol_;
    private final int poolMax_;
    private final SortedSet resultPool_;
    private final Worker[] workers_;
    private long submitIndex_;
    private long nextIndex_;
    private Result currentResult_;
    private IOException error_;

    /**
     * Constructor.
     *
     * @param  querySeq  sequence providing cone search query parameters
     * @param  coneSearcher  cone search implementation
     * @param  bestOnly  whether all results or just best are required
     * @param  distanceCol  name of column to hold distance information
     *                      in output table, or null
     * @param  parallelism  number of concurrent querying threads
     */
    public ParallelResultRowSequence( ConeQueryRowSequence querySeq,
                                      ConeSearcher coneSearcher,
                                      boolean bestOnly, String distanceCol,
                                      int parallelism ) {
        querySeq_ = querySeq;
        coneSearcher_ = coneSearcher;
        bestOnly_ = bestOnly;
        distanceCol_ = distanceCol;
        poolMax_ = parallelism * 3;
        resultPool_ = new TreeSet();
        workers_ = new Worker[ parallelism ];
        for ( int i = 0; i < parallelism; i++ ) {
            workers_[ i ] = new Worker( "Cone Query Worker #" + ( i + 1 ) );
            workers_[ i ].start();
        }
    }

    public boolean next() throws IOException {
        synchronized ( resultPool_ ) {

            /* Block until the next result in sequence is ready, 
             * or we know that no more will be forthcoming. */
            try {
                while ( ( ( resultPool_.size() == 0 ) ||
                          ((Result) resultPool_.first()).index_
                                                != nextIndex_ ) &&
                        ! workersFinished() ) {
                    resultPool_.wait();
                }
            }
            catch ( InterruptedException e ) {
                throw (IOException)
                      new IOException( "Interrupted" ).initCause( e );
            }

            /* If there is an error, throw it. */
            if ( error_ != null ) {
                throw (IOException)
                      new IOException( "Rethrowing error from read thread" )
                     .initCause( error_ );
            }

            /* If we have a result, note it as the current one for purposes
             * of returning values to users of this RowSequence and 
             * remove it from the pending results pool.  Since we take the
             * first item from the ordered set, it's the next one in the
             * output sequence, not necessarily the most recently acquired. */
            if ( resultPool_.size() > 0 ) {
                currentResult_ = (Result) resultPool_.first();
                assert currentResult_.index_ == nextIndex_;
                nextIndex_++;
                boolean removed = resultPool_.remove( currentResult_ );
                assert removed;
            }

            /* If there's no result, note that we have finished. */
            else {
                assert workersFinished();
                currentResult_ = null;
            }
            resultPool_.notifyAll();
            return currentResult_ != null;
        }
    }

    public Object getCell( int icol ) throws IOException {
        return getCurrentResult().row_[ icol ];
    }

    public Object[] getRow() throws IOException {
        return getCurrentResult().row_;
    }

    public double getRa() throws IOException {
        return getCurrentResult().ra_;
    }

    public double getDec() throws IOException {
        return getCurrentResult().dec_;
    }

    public double getRadius() throws IOException {
        return getCurrentResult().radius_;
    }

    public StarTable getConeResult() throws IOException {
        return getCurrentResult().table_;
    }

    public void close() throws IOException {
        for ( int i = 0; i < workers_.length; i++ ) {
            workers_[ i ].interrupt();
        }
        querySeq_.close();
    }

    /**
     * Returns the Result object containing data for the current state of
     * this row sequence (as far as readers are concerned).
     *
     * @return  current Result object; will not return null
     * @throws  IllegalStateException  if there is no current result
     * @throws  IOException   if one of the reader threads has encountered
     *                        an error
     */
    private Result getCurrentResult() throws IOException {
        if ( error_ != null ) {
            throw (IOException)
                  new IOException( "Rethrowing error from read thread" )
                 .initCause( error_ );
        }
        else if ( currentResult_ == null ) {
            String msg = "No current row";
            msg += workersFinished() ? " (iteration finished)"
                                     : " (next never called)";
            throw new IllegalStateException( msg );
        }
        else {
            return currentResult_;
        }
    }

    /**
     * Indicates whether all the worker threads have ceased operation.
     * If this returns true, no further results will be added to the 
     * result pool.
     *
     * @return  true  only if no further results are being gathered
     */
    private boolean workersFinished() {
        for ( int i = 0; i < workers_.length; i++ ) {
            if ( ! workers_[ i ].isFinished() ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper class encapsulating the parameters and result of a completed
     * cone search query.
     *
     * <p>Comparison between objects in this class is based only on the
     * index value.  This relation is consistent with the equality relation
     * <strong>provided that</strong> only one instance with the same 
     * index value is created within the lifetime of the containing object.
     * That condition should not be violated by the containing object.
     */
    private class Result implements Comparable {
        private final long index_;
        private final double ra_;
        private final double dec_;
        private final double radius_;
        private final Object[] row_;
        private final StarTable table_;

        /**
         * Constructor.
         *
         * @param  index in the sequence of queries defined by the input
         *         list for this row sequence
         * @param  ra  right ascenstion in degrees for cone query
         * @param  dec  declination in degrees for cone query
         * @param  radius  radius in degrees for cone query
         * @param  row  complete row of input table corresponding to this query
         * @param  table  result of cone search for this query
         */
        Result( long index, double ra, double dec, double radius, Object[] row,
                StarTable table ) {
            index_ = index;
            ra_ = ra;
            dec_ = dec;
            radius_ = radius;
            row_ = row;
            table_ = table;
        }

        /**
         * Compares on index value only.
         */
        public int compareTo( Object o ) {
            Result other = (Result) o;
            if ( this.index_ < other.index_ ) {
                return -1;
            }
            else if ( this.index_ > other.index_ ) {
                return +1;
            }
            else {
                assert this == other;
                return 0;
            }
        }

        public String toString() {
            return "R" + index_;
        }
    }

    /**
     * Thread which performs cone queries on behalf of this sequence.
     */
    private class Worker extends Thread {
        private boolean finished_;

        /**
         * Constructor.
         *
         * @param  name  thread name
         */
        Worker( String name ) {
            super( name );
            setDaemon( true );
        }

        public void run() {

            /* Loop while there are queries left to do and no error has been
             * encountered. */
            for ( boolean done = false; ! done; ) {
                try {
                    Result result = performNextQuery();
                    if ( result != null ) {
                        try {
                            submitResult( result );
                        }
                        catch ( InterruptedException e ) {
                            done = true;
                        }
                    }
                    else {
                        done = true;
                    }
                }
                catch ( IOException e ) {
                    setError( e );
                    done = true;
                }
                done = done || isInterrupted();
            }

            /* No more queries from this thread - record that fact. */
            synchronized ( resultPool_ ) {
                finished_ = true;
                resultPool_.notifyAll();
            }
        }

        /**
         * Indicates that this thread has finished.  If this returns true,
         * this thread will add no further results to the result pool.
         *
         * @return  true  only if this thread has finished
         */
        public boolean isFinished() {
            return finished_;
        }

        /**
         * Performs the next query required by this result sequence.
         *
         * @return   result of query, or null if there is no more work
         *           left to do
         */
        private Result performNextQuery() throws IOException {

            /* Get the parameters of the next required query, 
             * if there is one left to do. */
            final double ra;
            final double dec;
            final double radius;
            final long index;
            final Object[] row;
            synchronized ( querySeq_ ) {
                if ( querySeq_.next() ) {
                    ra = querySeq_.getRa();
                    dec = querySeq_.getDec();
                    radius = querySeq_.getRadius();
                    row = (Object[]) querySeq_.getRow().clone();
                    index = submitIndex_++;
                }

                /* If there is no work left - return null. */
                else {
                    return null;
                }
            }

            /* Perform the query. */
            StarTable table =
                ConeMatcher.getConeResult( coneSearcher_, bestOnly_,
                                           distanceCol_, ra, dec, radius );

            /* Return the completed result object. */
            return new Result( index, ra, dec, radius, row, table );
        }

        /**
         * Submit a result object to the pool of completed queries.
         * This makes it available for return to users of this result sequence.
         * This method may block until there is enough space in the 
         * pool of completed results, which prevents queries being done
         * (much) faster than they are consumed and hence piling up and
         * possibly using too much memory.
         *
         * @param   result   non-null result object
         */
        private void submitResult( Result result ) throws InterruptedException {

            /* Block until there is free space in the result pool. 
             * Note however that we add the result regardless of free space
             * if it is lower than the current highest one - otherwise we
             * risk the deadlock case in which the pool is full of later
             * results which cannot be extracted before an earlier one is
             * present. */
            synchronized ( resultPool_ ) {
                while ( resultPool_.size() > poolMax_ &&
                        result.compareTo( ((Result)
                                          resultPool_.last()) ) > 0 ) {
                    resultPool_.wait();
                }

                /* Add the result and notify waiters. */
                resultPool_.add( result );
                resultPool_.notifyAll();
            }
        }

        /**
         * Notify that an error has occurred.  This error is passed 
         * to the container object where it can be picked up and 
         * re-thrown later as appropriate.
         *
         * @param  error   error encountered during query resolution
         */
        private void setError( IOException error ) {
            synchronized ( ParallelResultRowSequence.this ) {
                if ( error_ == null ) {
                    error_ = error;
                    for ( int i = 0; i < workers_.length; i++ ) {
                        workers_[ i ].interrupt();
                    }
                }
            }
        }
    }
}