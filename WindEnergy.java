package cloud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import javax.naming.Context;
import java.lang.Object;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.mapred.jobcontrol.JobControl;
import org.apache.hadoop.util.*;


public class WindEnergy extends Configured implements Tool{

	public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);

		private Text station = new Text();
		private Text windReadings = new Text();
		private static HashMap<String,String> StatesMap = new HashMap<String,String>();
		File f;
		public void configure(JobConf conf)
		{
			Path[] cacheFiles =null;
			try {
				cacheFiles = DistributedCache.getLocalCacheFiles(conf);

			} catch (IOException e) {

				e.printStackTrace();
			}
			f = new File(cacheFiles[0].toString());
			LoadUSStates();

		}
		private void LoadUSStates()
		{
			String strRead;
			String splitarray[]=null;
			int j=0;
			try 
			{
				BufferedReader reader = new BufferedReader(new FileReader(f));

				while ((strRead=reader.readLine() ) != null)
				{

					splitarray = strRead.split(",");
					j = splitarray.length;
					for ( int i = 0; i < j; i++)
					{
						splitarray[i] = splitarray[i].replaceAll("\"", "");
					}

					if(splitarray[3].trim().toLowerCase().equals("us")
							&& !StatesMap.containsKey(splitarray[0].trim() + "-" + splitarray[1].trim()))
					{
						StatesMap.put(splitarray[0].trim() + "-" + splitarray[1].trim(), splitarray[5].trim());
					}
				}
				reader.close();
			}
			catch (IOException e) 
			{
				System.out.println(e.getMessage());
			}

		}
		public void map(LongWritable key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {

			String line;
			String line1;
			String[] splitvals;
			Double d = 0.0;
			try
			{
				line = value.toString();
				if(line == null)
					return;
				line1 = line.replaceAll("[ ]{2,}", " ");
				if(line1 == null)
					return;
				splitvals = line1.split(" ");
				if(splitvals == null)
					return;
				if(splitvals[0].equals("STN---"))
					return;
				if(!StatesMap.containsKey(splitvals[0]+"-"+splitvals[1]))
					return;
				if(Double.parseDouble(splitvals[13]) >= 5.0 && Double.parseDouble(splitvals[13]) < 999.9)
				{

					station.set(splitvals[0]+"-"+splitvals[1] + "," + splitvals[2].substring(0, 4));
					windReadings.set(splitvals[13]);
					d = Double.parseDouble(splitvals[13]);					
					one.set(d.intValue());
					output.collect(station, one);
				}
			}
			catch(IOException e) 
			{

				e.printStackTrace();
			}
		}
	}
	public static class Map1 extends MapReduceBase implements Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);

		private static HashMap<String,String> StatesMap = new HashMap<String,String>();
		File f;
		public void configure(JobConf conf)
		{
			Path[] cacheFiles =null;
			try {
				cacheFiles = DistributedCache.getLocalCacheFiles(conf);

			} catch (IOException e) {

				e.printStackTrace();
			}
			f = new File(cacheFiles[0].toString());
			LoadUSStates();

		}
		private void LoadUSStates()
		{
			String strRead;
			String splitarray[]=null;
			int j=0;
			try 
			{
				BufferedReader reader = new BufferedReader(new FileReader(f));

				while ((strRead=reader.readLine() ) != null)
				{

					splitarray = strRead.split(",");
					j = splitarray.length;
					for ( int i = 0; i < j; i++)
					{
						splitarray[i] = splitarray[i].replaceAll("\"", "");
					}

					if(splitarray[3].trim().toLowerCase().equals("us")
							&& !StatesMap.containsKey(splitarray[0].trim() + "-" + splitarray[1].trim()))
					{
						StatesMap.put(splitarray[0].trim() + "-" + splitarray[1].trim(), splitarray[5].trim());
					}
				}
				reader.close();
			}
			catch (IOException e) 
			{
				System.out.println(e.getMessage());
			}

		}
		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
						throws IOException 
						{
			Log l1 = LogFactory.getLog(Map1.class);

			String[] stationYear = value.toString().split(",");
			String[] stationYearCount = null;
			String state = null;
			if(stationYear != null && stationYear.length == 2)
			{
				if(StatesMap.containsKey(stationYear[0]))
				{
					state = StatesMap.get(stationYear[0]);
					stationYearCount = stationYear[1].split("\\t");
					if(stationYearCount != null && stationYearCount.length == 2)
					{
						output.collect(new Text(state + "," + stationYearCount[0]), 
								new IntWritable(Integer.parseInt(stationYearCount[1])));
					}
				}
			}
						}

	}
	public static class Reduce extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable>
	{
		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output,
				Reporter reporter) throws IOException
				{
			int sum = 0;
			int c = 0;
			IntWritable value;
			while (values.hasNext())
			{
				value = (IntWritable) values.next();
				sum += value.get();
				c++;
			}
			output.collect(key, new IntWritable(Math.round((float)sum/c)));
				}

	}
	public static class Reduce1 extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable>
	{
		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, IntWritable> output,
				Reporter reporter) throws IOException
				{
			int sum = 0;
			int c = 0;
			
			while (values.hasNext())
			{
				IntWritable value= (IntWritable) values.next();
				sum += value.get();
				c++;
				
			}
			output.collect(key, new IntWritable(Math.round((float)sum/c)));
			//output.collect(key, new IntWritable(highesttemp));
				}

	}

	public int run(String[] args) throws IOException
	{
		String inputPath = args[0];
		String outputPath = args[1];
		String finalPath = args[2];

		JobConf conf = new JobConf(WindEnergy.class);
		conf.setJobName("WindEnergy");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		conf.setMapperClass(Map.class);
		conf.setCombinerClass(Reduce.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		Job jobOne = new Job(conf);

		JobConf conf2 = new JobConf(WindEnergy.class);
		conf2.setJobName("Avg");

		conf2.setOutputKeyClass(Text.class);
		conf2.setOutputValueClass(IntWritable.class);

		conf2.setMapperClass(Map1.class);
		//conf2.setCombinerClass(Reduce.class);
		conf2.setReducerClass(Reduce1.class);

		conf2.setInputFormat(TextInputFormat.class);
		conf2.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf2, new Path(outputPath));
		FileOutputFormat.setOutputPath(conf2, new Path(finalPath));

		Job jobTwo = new Job(conf2);

		try 
		{
			DistributedCache.addCacheFile(new URI("/ish-history.csv"), conf);
			DistributedCache.addCacheFile(new URI("/ish-history.csv"), conf2);
		} 
		catch (URISyntaxException e) 
		{
			e.printStackTrace();
		}

		jobTwo.addDependingJob(jobOne);

		JobControl jobControl = new JobControl("JobControl");
		jobControl.addJob(jobOne);
		jobControl.addJob(jobTwo);
		//jobControl.run();

		Thread t = new Thread(jobControl);
		t.setDaemon(true);
		t.start();

		while(!jobControl.allFinished())
		{
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}   		
		}
		jobControl.stop();

		return 0;
	}

	public static void main(String[] args) throws Exception
	{
		int exitcode = ToolRunner.run(new WindEnergy(), args);
		System.exit(exitcode);
	}
}

