package q10.model;

import java.io.Serializable;

/**
 * Result schema for TPC-H Query 10
 */
public class Q10Result implements Serializable {

    // group by keys
    public long custKey;
    public String name;
    public String nation;

    // projected attributes
    public double acctBal;
    public String address;
    public String phone;
    public String comment;

    // aggregated value
    public double revenue;

    public Q10Result() {}

    @Override
    public String toString() {
        return "Q10Result{" +
                "custKey=" + custKey +
                ", name='" + name + '\'' +
                ", nation='" + nation + '\'' +
                ", acctBal=" + acctBal +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", revenue=" + revenue +
                '}';
    }
}
