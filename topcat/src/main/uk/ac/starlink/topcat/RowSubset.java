package uk.ac.starlink.topcat;

/**
 * Defines a selection of rows in a table model.
 * 
 * @author   Mark Taylor (Starlink)
 */
public interface RowSubset {

    /**
     * A subset containing all rows (<tt>isIncluded</tt> always true).
     */
    RowSubset ALL = new RowSubset() {
        public String getName() {
            return "All";
        }
        public boolean isIncluded( long lrow ) {
            return true;
        }
        public String toString() {
            return getName();
        }
    };

    /**
     * A subset containing no rows (<tt>isIncluded</tt> always false).
     */
    RowSubset NONE = new RowSubset() {
        public String getName() {
            return "None";
        }
        public boolean isIncluded( long lrow ) {
            return false;
        }
        public String toString() {
            return getName();
        }
    };

    /**
     * The name of this subset.
     *
     * @return name
     */
    String getName();

    /**
     * Indicates whether a given row is in the subset or not.
     *
     * @param  lrow  the index of the row in question
     * @return  <tt>true</tt> iff row <tt>lrow</tt> is to be included
     */
    boolean isIncluded( long lrow );
}