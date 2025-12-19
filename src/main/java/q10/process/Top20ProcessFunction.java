package q10.process;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import q10.model.Q10Result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Strict TPC-H Q10 Top-20
 *
 * - keeps latest Q10Result per customer
 * - sorts by revenue DESC
 * - outputs FULL TPCH Q10 schema
 */
public class Top20ProcessFunction
        extends KeyedProcessFunction<Integer, Q10Result, String> {

    /** custKey -> latest Q10Result */
    private MapState<Long, Q10Result> live;

    /** emit every N updates (avoid too noisy output) */
    private static final long EMIT_EVERY = 20_000;
//    private static final long EMIT_EVERY = 1; // genertate output
    private long cnt = 0;

    @Override
    public void open(Configuration parameters) {
        live = getRuntimeContext().getMapState(
                new MapStateDescriptor<>(
                        "live-q10",
                        Long.class,
                        Q10Result.class
                )
        );
    }

    @Override
    public void processElement(
            Q10Result r,
            Context ctx,
            Collector<String> out) throws Exception {

        // revenue <= 0 to not in Q10 result
        if (r.revenue <= 0) {
            live.remove(r.custKey);
        } else {
            live.put(r.custKey, r);
        }

        cnt++;
        if (cnt % EMIT_EVERY == 0) {
            emitTop20(out);
        }
    }

    /* ========= emit strict TPCH Q10 ========= */

    private void emitTop20(Collector<String> out) throws Exception {

        List<Q10Result> all = new ArrayList<>();
        for (Q10Result r : live.values()) {
            all.add(r);
        }

        // ORDER BY revenue DESC
        all.sort(
                Comparator.comparingDouble((Q10Result x) -> x.revenue)
                        .reversed()
        );

        StringBuilder sb = new StringBuilder();
        sb.append("\n================== TPCH Q10 TOP-20 ==================\n");
        sb.append(String.format(
                "%-4s | %-10s | %-25s | %-12s | %-10s | %-15s | %-20s | %-15s | %s\n",
                "Rank",
                "custKey",
                "name",
                "revenue",
                "acctBal",
                "nation",
                "address",
                "phone",
                "comment"
        ));
        sb.append("---------------------------------------------------------------------------------------------------------------------------------\n");

        for (int i = 0; i < Math.min(20, all.size()); i++) {
            Q10Result r = all.get(i);
            sb.append(String.format(
                    "%-4d | %-10d | %-25s | %-12.2f | %-10.2f | %-15s | %-20s | %-15s | %s\n",
                    i + 1,
                    r.custKey,
                    r.name,
                    r.revenue,
                    r.acctBal,
                    r.nation,
                    r.address,
                    r.phone,
                    r.comment
            ));
        }

        sb.append("=====================================================\n");
        out.collect(sb.toString());
    }
}
