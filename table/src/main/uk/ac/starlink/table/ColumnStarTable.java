package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A random-access StarTable that manages its data in columns.
 * The data in each column
 * is managed by a <tt>ColumnData</tt> object which can be accessed
 * directly using the {@link #getColumnData} method.  Columns can be
 * added and substituted.  If the columns permit it then table cells
 * can be written to as well as read from.
 * Concrete subclasses of this abstract class must implement 
 * {@link #getRowCount}.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ColumnStarTable extends RandomStarTable {

    public List columns = new ArrayList();

    /**
     * Gets the number of rows in the table (which must be applicable to
     * all the columns).  Since this is a <tt>RandomStarTable</tt> the
     * return value must be non-negative.
     *
     * @return  number of rows
     */
    public abstract long getRowCount();

    public int getColumnCount() {
        return columns.size();
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return getColumnData( icol ).getColumnInfo();
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        return getColumnData( icol ).readValue( lrow );
    }

    /**
     * Stores an object in a given cell of the table.
     *
     * @param  lrow  the row index
     * @param  icol  the column index
     * @param  value  the value to store
     * @throws  IOException if an I/O error occurs
     * @throws  UnsupportedOperationException  if column <tt>icol</tt> is not
     *          writable (<tt>!getColumnData(icol).isWritable()</tt>);
     */
    public void setCell( long lrow, int icol, Object value )
            throws IOException {
        ColumnData coldata = getColumnData( icol );
        if ( coldata.isWritable() ) {
            coldata.storeValue( lrow, value );
        }
        else {
            throw new UnsupportedOperationException( 
                "Column " + coldata + " not writable" );
        }
    }

    /**
     * Returns the <tt>ColumnData</tt> object for a given column.
     *
     * @param  icol  the index of the column for which the result is required
     * @return the ColumnData for column <tt>icol</tt>
     */
    public ColumnData getColumnData( int icol ) {
        return (ColumnData) columns.get( icol );
    }

    /**
     * Appends a new column to the end of this model.
     *
     * @param  coldata the new column object to add
     */
    public void addColumn( ColumnData coldata ) {
        columns.add( coldata );
    }

    /**
     * Substitutes a new column for the one which is currently in a given
     * position.  The old one is discarded.
     *
     * @param  icol  the column index to change
     * @param  coldata  the new column data object
     */
    public void setColumn( int icol, ColumnData coldata ) {
        columns.set( icol, coldata );
    }

}
