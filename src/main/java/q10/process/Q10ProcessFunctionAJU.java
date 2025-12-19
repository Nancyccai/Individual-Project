package q10.process;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import q10.model.*;

public class Q10ProcessFunctionAJU
        extends KeyedProcessFunction<Long, UpdateEvent<?>, Q10Result> {

    public static final OutputTag<Q10Update> OUTPUT_TAG =
            new OutputTag<Q10Update>("q10-changelog") {};

    /* ========= dimension tables ========= */

    private MapState<Long, Customer> customers;
    private MapState<Long, Nation> nations;

    /* ========= AJU core state ========= */

    // orderKey to custKey
    private MapState<Long, Long> orderCustomer;

    // orderKey to revenue of this order
    private MapState<Long, Double> orderRevenue;

    // orderKey to alive
    private MapState<Long, Boolean> orderAlive;

    // orderKey to number of live lineitems
    private MapState<Long, Integer> liveLineItemCount;

    // custKey to number of alive orders
    private MapState<Long, Integer> liveOrderCount;

    // custKey to total revenue
    private MapState<Long, Double> customerRevenue;

    @Override
    public void open(Configuration parameters) {

        customers = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("customers", Long.class, Customer.class));

        nations = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("nations", Long.class, Nation.class));

        orderCustomer = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("orderCustomer", Long.class, Long.class));

        orderRevenue = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("orderRevenue", Long.class, Double.class));

        orderAlive = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("orderAlive", Long.class, Boolean.class));

        liveLineItemCount = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("liveLineItemCount", Long.class, Integer.class));

        liveOrderCount = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("liveOrderCount", Long.class, Integer.class));

        customerRevenue = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("customerRevenue", Long.class, Double.class));
    }

    @Override
    public void processElement(
            UpdateEvent<?> evt,
            Context ctx,
            Collector<Q10Result> out) throws Exception {

        switch (evt.table) {

            /* ========= dimensions ========= */

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

            /* ========= orders ========= */

            case "orders": {
                Order o = (Order) evt.record;

                // Q10 time filter
                if (o.orderDate.compareTo("1993-10-01") < 0 ||
                        o.orderDate.compareTo("1994-01-01") >= 0) {
                    return;
                }

                orderCustomer.put(o.orderKey, o.custKey);
                orderAlive.put(o.orderKey, false);
                break;
            }

            /* ========= lineitem ========= */

            case "lineitem": {
                LineItem li = (LineItem) evt.record;

                if (!"R".equals(li.returnFlag)) return;

                Long custKey = orderCustomer.get(li.orderKey);
                if (custKey == null) return;

                /* ---- update liveLineItemCount ---- */

                int cnt = liveLineItemCount.get(li.orderKey) == null
                        ? 0
                        : liveLineItemCount.get(li.orderKey);

                if (evt.op == UpdateEvent.Op.INSERT) {
                    cnt++;
                } else {
                    cnt--;
                }

                liveLineItemCount.put(li.orderKey, cnt);

                /* ---- compute delta ---- */

                double delta =
                        li.extendedPrice * (1.0 - li.discount);

                if (evt.op == UpdateEvent.Op.DELETE) {
                    delta = -delta;
                }

                double oldOrderRev =
                        orderRevenue.get(li.orderKey) == null
                                ? 0.0
                                : orderRevenue.get(li.orderKey);

                double newOrderRev = oldOrderRev + delta;
                orderRevenue.put(li.orderKey, newOrderRev);

                Boolean alive = orderAlive.get(li.orderKey);

                /* ========= order becomes alive ========= */

                if (cnt == 1 && (alive == null || !alive)) {
                    orderAlive.put(li.orderKey, true);

                    int oc = liveOrderCount.get(custKey) == null
                            ? 0
                            : liveOrderCount.get(custKey);
                    liveOrderCount.put(custKey, oc + 1);

                    applyCustomerDelta(custKey, newOrderRev, out, ctx);
                }

                /* ========= order still alive ========= */

                else if (cnt > 1 && alive != null && alive) {
                    applyCustomerDelta(custKey, delta, out, ctx);
                }

                /* ========= order becomes dead ========= */

                else if (cnt == 0 && alive != null && alive) {
                    orderAlive.put(li.orderKey, false);

                    int oc = liveOrderCount.get(custKey) - 1;
                    if (oc == 0) {
                        liveOrderCount.remove(custKey);
                    } else {
                        liveOrderCount.put(custKey, oc);
                    }

                    applyCustomerDelta(custKey, -oldOrderRev, out, ctx);
                }

                break;
            }
        }
    }

    /* ========= helper ========= */

    private void applyCustomerDelta(
            long custKey,
            double delta,
            Collector<Q10Result> out,
            Context ctx) throws Exception {

        double oldRev =
                customerRevenue.get(custKey) == null
                        ? 0.0
                        : customerRevenue.get(custKey);

        double newRev = oldRev + delta;

        if (newRev == 0.0) {
            customerRevenue.remove(custKey);
        } else {
            customerRevenue.put(custKey, newRev);
        }

        Integer aliveCnt = liveOrderCount.get(custKey);
        boolean alive = aliveCnt != null && aliveCnt > 0;

        Customer c = customers.get(custKey);
        Nation n = nations.get(c.nationKey);

        if (alive) {
            Q10Result r = new Q10Result();
            r.custKey = custKey;
            r.name = c.name;
            r.nation = n.name;
            r.acctBal = c.acctBal;
            r.address = c.address;
            r.phone = c.phone;
            r.comment = c.comment;
            r.revenue = newRev;

            out.collect(r);
        }

        Q10Update u = new Q10Update();
        u.custKey = custKey;
        u.name = c.name;
        u.nation = n.name;
        u.oldRevenue = oldRev;
        u.newRevenue = newRev;
        u.delta = delta;

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
