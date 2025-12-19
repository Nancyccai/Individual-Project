package q10;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import q10.model.Q10Result;
import q10.model.Q10Update;
import q10.process.Q10ProcessFunction;
import q10.process.Top20ProcessFunction;
import q10.source.TpchQ10Source;

public class Q10Job {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        SingleOutputStreamOperator<Q10Result> main =
                env.addSource(new TpchQ10Source())
                        .keyBy(e -> 0L)
                        .process(new Q10ProcessFunction());

        // Top-20 (snapshot)
        main
                .keyBy(r -> 0)
                .process(new Top20ProcessFunction())
                .print("TOP20");

        // Changelog (THIS is new)
        main
                .getSideOutput(Q10ProcessFunction.OUTPUT_TAG)
                .print("Q10-CHANGELOG");

        env.execute("TPC-H Q10 Streaming + Changelog");
    }
}
