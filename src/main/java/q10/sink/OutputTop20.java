package q10.sink;

import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.DataStream;

import q10.model.Q10Result;
import q10.process.Q10ProcessFunctionAJU;
import q10.process.Top20ProcessFunction;
import q10.source.TpchQ10Source;

public class OutputTop20 {

    public static void main(String[] args) throws Exception {

        /* ========= Environment ========= */

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(1);

        env.enableCheckpointing(2000);

        env.getCheckpointConfig().setCheckpointStorage(
                new FileSystemCheckpointStorage("file:///flink-checkpoints")
        );

        /* ========= Streaming Q10 (AJU) ========= */

        SingleOutputStreamOperator<Q10Result> main =
                env.addSource(new TpchQ10Source())
                        .keyBy(e -> 0L)
                        .process(new Q10ProcessFunctionAJU());

        /* ========= TOP-20 ========= */

        DataStream<String> top20 =
                main
                        .keyBy(r -> 0)
                        .process(new Top20ProcessFunction());

        /* ========= Console Output ========= */

        top20.print("TOP20");

        /* ========= File Output ========= */

        FileSink<String> top20Sink =
                FileSink
                        .forRowFormat(
                                new Path("output/top20"),
                                new SimpleStringEncoder<String>("UTF-8")
                        )
                        .build();

        top20.sinkTo(top20Sink);

        /* ========= Execute ========= */

        env.execute("TPC-H Q10 AJU - TOP20 OUTPUT");
    }
}
