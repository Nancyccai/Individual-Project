package q10.source;

import org.apache.flink.streaming.api.functions.source.SourceFunction;
import q10.model.*;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class TpchQ10Source implements SourceFunction<UpdateEvent<?>> {

    private volatile boolean running = true;

    private static final int WARMUP_INSERT = 200_000;
    private static final int WINDOW_SIZE   = 100_000;

    @Override
    public void run(SourceContext<UpdateEvent<?>> ctx) throws Exception {

        readNation(ctx);
        readCustomer(ctx);
        readOrders(ctx);

        Deque<LineItem> buffer = new ArrayDeque<>();

        try (BufferedReader br = reader("sf1/lineitem.tbl")) {
            String l;
            int cnt = 0;

            // PHASE 1: INSERT
            while (running && cnt < WARMUP_INSERT && (l = br.readLine()) != null) {
                LineItem li = parseLineItem(l);
                ctx.collect(new UpdateEvent<>(UpdateEvent.Op.INSERT, "lineitem", li));
                buffer.addLast(li);
                cnt++;
            }

            // PHASE 2: INSERT + DELETE
            while (running && (l = br.readLine()) != null) {
                LineItem li = parseLineItem(l);
                ctx.collect(new UpdateEvent<>(UpdateEvent.Op.INSERT, "lineitem", li));
                buffer.addLast(li);

                if (buffer.size() > WINDOW_SIZE) {
                    LineItem old = buffer.removeFirst();
                    ctx.collect(new UpdateEvent<>(UpdateEvent.Op.DELETE, "lineitem", old));
                }
            }
        }
    }

    /* ===== helpers ===== */

    private LineItem parseLineItem(String l) {
        String[] f = l.split("\\|");
        LineItem li = new LineItem();
        li.orderKey = Long.parseLong(f[0]);
        li.extendedPrice = Double.parseDouble(f[5]);
        li.discount = Double.parseDouble(f[6]);
        li.returnFlag = f[8];
        return li;
    }

    private void readNation(SourceContext<UpdateEvent<?>> ctx) throws Exception {
        try (BufferedReader br = reader("sf1/nation.tbl")) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] f = l.split("\\|");
                Nation n = new Nation();
                n.nationKey = Long.parseLong(f[0]);
                n.name = f[1];
                ctx.collect(new UpdateEvent<>(UpdateEvent.Op.INSERT, "nation", n));
            }
        }
    }

    private void readCustomer(SourceContext<UpdateEvent<?>> ctx) throws Exception {
        try (BufferedReader br = reader("sf1/customer.tbl")) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] f = l.split("\\|");
                Customer c = new Customer();
                c.custKey = Long.parseLong(f[0]);
                c.name = f[1];
                c.address = f[2];
                c.nationKey = Long.parseLong(f[3]);
                c.phone = f[4];
                c.acctBal = Double.parseDouble(f[5]);
                c.comment = f[7];
                ctx.collect(new UpdateEvent<>(UpdateEvent.Op.INSERT, "customer", c));
            }
        }
    }

    private void readOrders(SourceContext<UpdateEvent<?>> ctx) throws Exception {
        try (BufferedReader br = reader("sf1/orders.tbl")) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] f = l.split("\\|");
                Order o = new Order();
                o.orderKey = Long.parseLong(f[0]);
                o.custKey = Long.parseLong(f[1]);
                o.orderDate = f[4];   //  Q10
                ctx.collect(new UpdateEvent<>(UpdateEvent.Op.INSERT, "orders", o));
            }
        }
    }

    private BufferedReader reader(String file) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(file);
        if (is == null) throw new RuntimeException("Resource not found: " + file);
        return new BufferedReader(new InputStreamReader(is));
    }

    @Override
    public void cancel() {
        running = false;
    }
}
