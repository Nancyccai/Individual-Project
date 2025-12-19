package q10;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import q10.model.Q10Result;
import q10.model.Q10Update;
import q10.process.Q10ProcessFunctionAJU;
import q10.process.Top20ProcessFunction;
import q10.source.TpchQ10Source;

public class Q10JobAJU {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        SingleOutputStreamOperator<Q10Result> main =
                env.addSource(new TpchQ10Source())
                        .keyBy(e -> 0L)
                        .process(new Q10ProcessFunctionAJU());

        // Top-20 snapshot
        main
                .keyBy(r -> 0)
                .process(new Top20ProcessFunction())
                .print("TOP20");

        // changelog
        main
                .getSideOutput(Q10ProcessFunctionAJU.OUTPUT_TAG)
                .print("Q10-CHANGELOG");

        long startTime = System.currentTimeMillis();

        env.execute("TPC-H Q10 AJU");

        long endTime = System.currentTimeMillis();

        System.out.println("Execution time: " + (endTime - startTime) + " ms");
    }
}
