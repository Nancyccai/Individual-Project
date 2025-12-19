package q10.process;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import q10.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TRUE Streaming TPC-H Q10
 *
 * - incremental join
 * - supports INSERT / DELETE
 * - nation / customer are static dimension tables
 * - emits Q10Result (main output)
 * - emits Q10Update changelog (side output)
 */
public class Q10ProcessFunction
        extends KeyedProcessFunction<Long, UpdateEvent<?>, Q10Result> {

    /* ========= side output ========= */

    public static final OutputTag<Q10Update> OUTPUT_TAG =
            new OutputTag<Q10Update>("q10-changelog") {};

    /* ========= base tables (dimension / lookup) ========= */

    private MapState<Long, Customer> customers;
    private MapState<Long, Nation> nations;
    private MapState<Long, Order> orders;

    /* ========= buffering (out-of-order) ========= */

    private MapState<Long, List<LineItem>> pendingLineItems;

    /* ========= aggregation state ========= */

    private MapState<Long, Double> revenue;   // custKey -> revenue

    @Override
    public void open(Configuration parameters) {

        customers = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("customers", Long.class, Customer.class));

        nations = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("nations", Long.class, Nation.class));

        orders = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("orders", Long.class, Order.class));

        pendingLineItems = getRuntimeContext().getMapState(
                new MapStateDescriptor<>(
                        "pendingLineItems",
                        Long.class,
                        (Class<List<LineItem>>) (Class<?>) List.class));

        revenue = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("revenue", Long.class, Double.class));
    }

    @Override
    public void processElement(
            UpdateEvent<?> evt,
            Context ctx,
            Collector<Q10Result> out) throws Exception {

        switch (evt.table) {

            /* ========= static dimension tables ========= */

            case "nation": {
                Nation n = (Nation) evt.record;
                nations.put(n.nationKey, n);
                break;
            }

            case "customer": {
                Customer c = (Customer) evt.record;
                customers.put(c.custKey, c);
                break;
            }

            case "orders": {
                Order o = (Order) evt.record;
                orders.put(o.orderKey, o);

                // replay buffered lineitems
                List<LineItem> buf = pendingLineItems.get(o.orderKey);
                if (buf != null) {
                    for (LineItem li : buf) {
                        processLineItem(li, UpdateEvent.Op.INSERT, out, ctx);
                    }
                    pendingLineItems.remove(o.orderKey);
                }
                break;
            }

            /* ========= fact table ========= */

            case "lineitem": {
                LineItem li = (LineItem) evt.record;
                processLineItem(li, evt.op, out, ctx);
                break;
            }
        }
    }

    /* ===================================================== */

    private void processLineItem(
            LineItem li,
            UpdateEvent.Op op,
            Collector<Q10Result> out,
            Context ctx) throws Exception {

        // Q10 filter: returnFlag = 'R'
        if (!"R".equals(li.returnFlag)) return;

        Order o = orders.get(li.orderKey);
        if (o == null) {
            // order not arrived yet -> buffer
            List<LineItem> buf = pendingLineItems.get(li.orderKey);
            if (buf == null) buf = new ArrayList<>();
            buf.add(li);
            pendingLineItems.put(li.orderKey, buf);
            return;
        }

        // Q10 date filter
        if (o.orderDate.compareTo("1993-10-01") < 0 ||
                o.orderDate.compareTo("1994-01-01") >= 0) {
            return;
        }

        Customer c = customers.get(o.custKey);
        if (c == null) return;

        Nation n = nations.get(c.nationKey);
        if (n == null) return;

        double delta = li.extendedPrice * (1.0 - li.discount);
        if (op == UpdateEvent.Op.DELETE) {
            delta = -delta;
        }

        double oldRev = revenue.get(o.custKey) == null
                ? 0.0
                : revenue.get(o.custKey);

        double newRev = oldRev + delta;

        if (newRev == 0.0) {
            revenue.remove(o.custKey);
        } else {
            revenue.put(o.custKey, newRev);
        }

        /* ========= main result ========= */

        Q10Result r = new Q10Result();
        r.custKey = c.custKey;
        r.name = c.name;
        r.nation = n.name;
        r.acctBal = c.acctBal;
        r.address = c.address;
        r.phone = c.phone;
        r.comment = c.comment;
        r.revenue = newRev;

        out.collect(r);

        /* ========= changelog ========= */

        Q10Update u = new Q10Update();
        u.custKey = c.custKey;
        u.name = c.name;
        u.nation = n.name;
        u.oldRevenue = oldRev;
        u.delta = delta;
        u.newRevenue = newRev;

        if (oldRev == 0.0 && newRev != 0.0) {
            u.kind = Q10Update.Kind.INSERT;
        } else if (oldRev != 0.0 && newRev == 0.0) {
            u.kind = Q10Update.Kind.DELETE;
        } else {
            u.kind = Q10Update.Kind.UPDATE;
        }

        ctx.output(OUTPUT_TAG, u);
    }
}
