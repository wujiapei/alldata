package com.platform.website.transformer.mr;

import com.google.common.collect.Lists;
import com.platform.website.common.EventLogConstants;
import com.platform.website.common.GlobalConstants;
import com.platform.website.util.TimeUtil;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.MultipleColumnPrefixFilter;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

public abstract class TransformBaseRunner implements Tool {

  private static final Logger logger = Logger.getLogger(TransformBaseRunner.class);
  private static String ClusterName = "nn";
  private static final String HADOOP_URL = "hdfs://" + ClusterName;
  private static Configuration conf = new Configuration();
  protected String jobName;

  private Class<?> runnerClass;
  private Class<? extends TableMapper<?, ?>> mapperClass;
  private Class<? extends Reducer<?, ?, ?, ?>> reducerClass;
  private Class<? extends OutputFormat<?, ?>> outputFormatClass;
  private Class<? extends WritableComparable<?>> mapOutputKeyClass;
  private Class<? extends Writable> mapOutputValueClass;
  private Class<?> outputKeyClass;
  private Class<?> outputValueClass;
  private long startTime;
  private boolean isCallSetUpRunnerMethod = false;

  static {
    conf.set("fs.defaultFS", HADOOP_URL);
    conf.set("dfs.nameservices", ClusterName);
    conf.set("dfs.ha.namenodes." + ClusterName, "Master,Master2");
    conf.set("dfs.namenode.rpc-address." + ClusterName + ".Master", "192.168.52.156:9000");
    conf.set("dfs.namenode.rpc-address." + ClusterName + ".Master2", "192.168.52.147:9000");
    conf.set("dfs.client.failover.proxy.provider." + ClusterName,
        "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider");

//    conf.addResource("hbase-site.xml");
    conf.addResource("output-collector.xml");
    conf.addResource("query-mapping.xml");
    conf.addResource("transformer-env.xml");
  }

  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass) {
    this.setupRunner(jobName, runnerClass, mapperClass, reducerClass, outputKeyClass, outputValueClass, TransformerOutputFormat.class);
  }

  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass,
      Class<? extends OutputFormat<?, ?>> outputFormatClass) {
      this.setupRunner(jobName, runnerClass, mapperClass, reducerClass, outputKeyClass, outputValueClass, outputKeyClass, outputValueClass, outputFormatClass);

  }


  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> mapOutputKeyClass,
      Class<? extends Writable> mapOutputValueClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass) {
    this.setupRunner(jobName, runnerClass, mapperClass, reducerClass, mapOutputKeyClass, mapOutputValueClass, outputKeyClass, outputValueClass, TransformerOutputFormat.class);
  }


  public void setupRunner(String jobName, Class<?> runnerClass,
      Class<? extends TableMapper<?, ?>> mapperClass, Class<? extends Reducer<?, ?, ?, ?>> reducerClass,
      Class<? extends WritableComparable<?>> mapOutputKeyClass,
      Class<? extends Writable> mapOutputValueClass,
      Class<? extends WritableComparable<?>> outputKeyClass,
      Class<? extends Writable> outputValueClass,
      Class<? extends OutputFormat<?, ?>> outputFormatClass) {

    this.jobName = jobName;
    this.runnerClass = runnerClass;
    this.mapperClass = mapperClass;
    this.reducerClass = reducerClass;
    this.mapOutputKeyClass = mapOutputKeyClass;
    this.mapOutputValueClass = mapOutputValueClass;
    this.outputKeyClass = outputKeyClass;
    this.outputValueClass = outputValueClass;
    this.outputFormatClass = outputFormatClass;
    this.isCallSetUpRunnerMethod = true;
  }

  /**
   * ??????????????????
   * @param args
   * @throws Exception
   */
  public void startRunner(String []args)throws  Exception{
    ToolRunner.run(new Configuration(), this, args);
  }

  @Override
  public int run(String[] args) throws Exception {
    if (!this.isCallSetUpRunnerMethod){
      throw new RuntimeException("????????????setupRunner????????????????????????");
    }
    Configuration conf = this.getConf();
    this.processArgs(conf, args);
    Job job = this.initJob(conf);//??????job
    //??????job
    this.beforeRunJob(job);
    Throwable error = null;
    try {
      this.startTime = System.currentTimeMillis();
      return job.waitForCompletion(true) ? 0 : -1;
    } catch (Throwable e) {
      error = e;
      logger.error("??????" + this.jobName + "job????????????", e);
      throw new RuntimeException(e);
    } finally {
      this.afterRunJob(job, error);
    }
  }

  protected void beforeRunJob(Job job) throws IOException {
  }

  protected void afterRunJob(Job job, Throwable e) throws IOException {
    try {

      long endTime = System.currentTimeMillis();
      logger.info("Job" + job.getJobName() + "??????????????????:" + (e == null ? job.isSuccessful() : false)
          + "; ????????????:" + startTime
          + "; ????????????:" + endTime + "; ??????" + (endTime - startTime) + "ms" + (e == null ? ""
          : "; ???????????????:" + e));
    } catch (Throwable e1) {
      //nothing
    }
  }

  protected Job initJob(Configuration conf) throws IOException {
    Job job = Job.getInstance(conf, this.jobName);

    job.setJarByClass(this.runnerClass);
    // ????????????
//    TableMapReduceUtil.initTableMapperJob(initScans(job), this.mapperClass, this.mapOutputKeyClass, this.mapOutputValueClass, job, false);
    TableMapReduceUtil.initTableMapperJob(initScans(job), this.mapperClass, this.mapOutputKeyClass, this.mapOutputValueClass, job, true);
    // ????????????????????????????????????(jar)??????
    // TableMapReduceUtil.initTableMapperJob(initScans(job),
    // this.mapperClass, this.mapOutputKeyClass, this.mapOutputValueClass,
    // job);
    job.setReducerClass(this.reducerClass);
    job.setOutputKeyClass(this.outputKeyClass);
    job.setOutputValueClass(this.outputValueClass);
    job.setOutputFormatClass(this.outputFormatClass);
    return job;
  }


  @Override
  public void setConf(Configuration conf) {
  }

  @Override
  public Configuration getConf() {
    return this.conf;
  }

  /**
   * ?????????scan??????
   *
   * @param job
   * @return
   */
  protected List<Scan> initScans(Job job) {
    Configuration conf = job.getConfiguration();
    // ??????????????????: yyyy-MM-dd
    String date = conf.get(GlobalConstants.RUNNING_DATE_PARAMS);
    long startDate = TimeUtil.parseString2Long(date);
    long endDate = startDate + GlobalConstants.DAY_OF_MILLISECONDS;

    Scan scan = new Scan();
    // ??????hbase???????????????rowkey?????????rowkey
    scan.setStartRow(Bytes.toBytes("" + startDate));
    scan.setStopRow(Bytes.toBytes("" + endDate));

    scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, Bytes.toBytes(EventLogConstants.HBASE_NAME_EVENT_LOGS));
    Filter filter = this.fetchHbaseFilter();
    if (filter != null) {
      scan.setFilter(filter);
    }

    // ????????????cache
//    scan.setBatch(500);
//    scan.setCacheBlocks(true); // ??????cache blocks
//    scan.setCaching(1000); // ???????????????????????????????????????100???????????????????????????????????????(??????rpc??????)???????????????????????????????????????????????????
    return Lists.newArrayList(scan);
  }

  protected Filter getColumnFilter(String[] columns) {
    int length = columns.length;
    byte[][] filter = new byte[length][];
    for (int i = 0; i < length; i++) {
      filter[i] = Bytes.toBytes(columns[i]);
    }
    return new MultipleColumnPrefixFilter(filter);
  }

  protected void processArgs(Configuration conf, String[] args) {
    String date = null;
    for (int i = 0; i < args.length; i++) {
      if ("-d".equals(args[i])) {
        if (i + 1 < args.length) {
          date = args[++i];
          break;
        }
      }
    }

    //??????date?????????yyyy-MM-dd
    if (StringUtils.isBlank(date) || !TimeUtil.isValidateRunningDate(date)) {
      //date??????
      date = TimeUtil.getYesterday();
    }
    conf.set(GlobalConstants.RUNNING_DATE_PARAMS, date);
  }

  /**
   * ??????hbase???????????????filter??????
   * @return
   */
  protected Filter fetchHbaseFilter() {
    return null;
  }


}
