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

public class Top20ProcessFunction3attritubes
        extends KeyedProcessFunction<Integer, Q10Result, String> {

    private MapState<Long, Q10Result> live;
    private long cnt = 0;

    private static final long EMIT_EVERY = 20_000;

    @Override
    public void open(Configuration parameters) {
        live = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("live", Long.class, Q10Result.class));
    }

    @Override
    public void processElement(
            Q10Result r,
            Context ctx,
            Collector<String> out) throws Exception {

        if (r.revenue <= 0) {
            live.remove(r.custKey);
        } else {
            live.put(r.custKey, r);
        }

        cnt++;
        if (cnt % EMIT_EVERY == 0) {
            emit(out);
        }
    }

    private void emit(Collector<String> out) throws Exception {
        List<Q10Result> all = new ArrayList<>();
        for (Q10Result r : live.values()) all.add(r);

        all.sort(Comparator.comparingDouble((Q10Result x) -> x.revenue).reversed());

        StringBuilder sb = new StringBuilder("\n===== TOP-20 Q10 =====\n");
        for (int i = 0; i < Math.min(20, all.size()); i++) {
            Q10Result r = all.get(i);
            sb.append(String.format(
                    "%2d | cust=%d | revenue=%.2f | %s | %s\n",
                    i + 1, r.custKey, r.revenue, r.name, r.nation));
        }
        out.collect(sb.toString());
    }
}
