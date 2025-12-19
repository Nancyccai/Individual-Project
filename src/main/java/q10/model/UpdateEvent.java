package q10.model;

public class UpdateEvent<T> {

    public enum Op { INSERT, DELETE }

    public Op op;
    public String table;
    public T record;

    public UpdateEvent() {}

    public UpdateEvent(Op op, String table, T record) {
        this.op = op;
        this.table = table;
        this.record = record;
    }
}
