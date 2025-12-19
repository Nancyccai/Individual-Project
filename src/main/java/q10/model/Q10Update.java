package q10.model;

import java.io.Serializable;

/**
 * Changelog record for Q10
 * One record per INSERT / UPDATE / DELETE
 */
public class Q10Update implements Serializable {

    public enum Kind {
        INSERT,
        UPDATE,
        DELETE
    }

    public Kind kind;

    public long custKey;

    /** before change */
    public double oldRevenue;

    /** change amount */
    public double delta;

    /** after change */
    public double newRevenue;

    public String name;
    public String nation;

    public Q10Update() {}

    @Override
    public String toString() {
        return String.format(
                "%s | cust=%d | %s | %s | old=%.2f | delta=%+.2f | new=%.2f",
                kind,
                custKey,
                name,
                nation,
                oldRevenue,
                delta,
                newRevenue
        );
    }
}
