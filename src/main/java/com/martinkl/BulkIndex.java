package com.martinkl;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.elasticsearch.hadoop.mr.EsOutputFormat;

/**
 * To run this:
 * 
 * <pre>
 * mvn package
 * hadoop jar target/elastic-test-0.0.1-mapreduce.jar com.martinkl.BulkIndex
 * </pre>
 */
public class BulkIndex {

    public static final String ES_NODES = "localhost:9200";
    public static final String ES_RESOURCE = "docs/doc";

    public static class HBaseTableMapper extends TableMapper<NullWritable, MapWritable> {
        private static final Text ID_FIELD = new Text("id");
        private static final Text TEXT_FIELD = new Text("text");

        @Override
        protected void map(ImmutableBytesWritable key, Result value, Context context)
                throws IOException, InterruptedException {
            MapWritable doc = new MapWritable();
            doc.put(ID_FIELD, new Text(key.get()));
            doc.put(TEXT_FIELD, new Text(value.getValue(BulkLoad.HBASE_COL_FAMILY, BulkLoad.HBASE_COL_NAME)));
            context.write(NullWritable.get(), doc);
        }
    }

    public static void main(String[] args) throws Exception {
        new JobConf().setSpeculativeExecution(false);
        Configuration conf = new Configuration();
        conf.setBoolean("mapred.map.tasks.speculative.execution", false);    
        conf.setBoolean("mapred.reduce.tasks.speculative.execution", false); 
        conf.set("es.nodes", ES_NODES);
        conf.set("es.resource", ES_RESOURCE);

        Job job = new Job(conf);
        job.setJarByClass(BulkIndex.class);
        job.setMapperClass(HBaseTableMapper.class);
        job.setNumReduceTasks(0);
        job.setOutputFormatClass(EsOutputFormat.class);
        job.setMapOutputValueClass(MapWritable.class); 

        TableMapReduceUtil.initTableMapperJob(BulkLoad.HBASE_TABLE_NAME, new Scan(),
            HBaseTableMapper.class, ImmutableBytesWritable.class, Result.class, job);

        job.waitForCompletion(true);
    }
}
