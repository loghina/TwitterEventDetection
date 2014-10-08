package subgraphextraction;

/** Alexandru Loghin & Luise Torres
 * Class for retrieving tweets from MongoDB and creating a graph
 * */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ParallelScanOptions;

import subgraphextraction.*;

import org.jgrapht.graph.*;


public class MongoDbConnection {

	final static String DATABASE = "newparis";
	final static String COLLECTION = "tweets";
	final static String HASHTAG_TO_RETRIEVE1 = "paris";
	final static String HASHTAG_TO_RETRIEVE2 = "paris";

	final static String OUTPUTGRAPH = "ParisGraph.txt";
	final static String OCCURENCESFILE = "ParisOccurences.txt";
	final static String TUPLES_FILE = "ParisTuples.txt";
	final static String SUBGRAPHS_FILE = "ParisSubgraphs.txt";
	final static int TUPLES_TO_INSERT = 1000000;
	final static double DMIN = 0.75;
	final static double DMAX = 1.0;


	private static SimpleGraph<String, DefaultWeightedEdge> hashtagGraph=new SimpleGraph<String,DefaultWeightedEdge>(DefaultWeightedEdge.class);
	private static HashMap <String,Integer> occurences=new HashMap <String,Integer>();
	private static HashMap<String,Integer> languages=new HashMap<String,Integer>();

	public static DB connect(String host, int port) throws UnknownHostException{
		MongoClient mongoClient = new MongoClient(host, port);
		DB db = mongoClient.getDB(DATABASE);
		return db;
	}

	public static void retrieveTweets(DB mydb) throws IOException{
		System.out.println("Retrieving tweets from database "+mydb.getName());
		int nrVertex=0, occ_hashtagtoretrieve=0;
		long nr_of_hashtags=0;
		long n_tweets=0;

		DBCollection coll = mydb.getCollection(COLLECTION);
		//retrieve all hashtags inside tweets
		DBCursor cursor = coll.find();
		try {
			while(cursor.hasNext()) {
				BasicDBObject obj=(BasicDBObject)cursor.next();
				String lang=obj.getString("lang");
				if (!languages.containsKey(lang))
					languages.put(lang, 1);
				else
					languages.put(lang, languages.get(lang)+1);
				n_tweets++;
				String tweet=obj.getString("text");
			    if (tweet.indexOf("RT @")<0){
			       //consider only initial tweets, not retweets
			       BasicDBObject entitiesList = (BasicDBObject) obj.get("entities");
			       BasicDBList hashtagsList = (BasicDBList) entitiesList.get("hashtags");
			       ArrayList<String>currentTags=new ArrayList<String>();
			       nr_of_hashtags +=hashtagsList.size();
			       for (int i = 0; i < hashtagsList.size(); i++) {
			    	   BasicDBObject hashObj = (BasicDBObject) hashtagsList.get(i);
			           String hashtag = hashObj.getString("text").toLowerCase();
			           if (hashtag.compareTo(HASHTAG_TO_RETRIEVE1)==0 || hashtag.compareTo(HASHTAG_TO_RETRIEVE2)==0){
			        	   occ_hashtagtoretrieve++;
			           }else{
			           //all tweets contain "paris" hashtag - ignore it
			              if (!currentTags.contains(hashtag)){
			        		   currentTags.add(hashtag);
			        	   }
			        	   //create graph nodes and edges
			        	   if (!hashtagGraph.containsVertex(hashtag)){
			        		   hashtagGraph.addVertex(hashtag);
			        		   occurences.put(hashtag, 1);
			        		   nrVertex++;
			        	   }else{
			        		   Integer occ=occurences.get(hashtag);
			        		   if (occ!=null)
			        			   occurences.put(hashtag, occ+1);
			        	   }
			           }
			       }
			       //add edges between currentTags
			       for (int i=0;i<currentTags.size();i++){
			    	   String tag=currentTags.get(i);
			    	   for (int j=i+1;j<currentTags.size();j++){	//don't iterate on the whole set
			    		   String tag2=currentTags.get(j);
			    		   if (tag.compareTo(tag2)!=0){
			    			   DefaultWeightedEdge e=hashtagGraph.getEdge(tag, tag2);
			    			   if (e==null){
			    				   e=hashtagGraph.addEdge(tag, tag2);
			    				   hashtagGraph.setEdgeWeight(e, 1);
			    			   }else{
			    				   double weight=hashtagGraph.getEdgeWeight(e);
			    				   hashtagGraph.setEdgeWeight(e, weight+1);
			    			   }
			    		   }
			    	   }
			       }
			    }
			}


			//elaborate the graph
			Set<DefaultWeightedEdge> edges=hashtagGraph.edgeSet();
			List<DefaultWeightedEdge> list = new ArrayList<DefaultWeightedEdge>(edges);
			Collections.sort(list,MongoDbConnection.EdgeComparator);

			//Print on file the binary associations between hashtags that occur more frequently
			BufferedWriter out = new BufferedWriter(new FileWriter(OUTPUTGRAPH));
			for(DefaultWeightedEdge e:list){
			   out.write(hashtagGraph.getEdgeSource(e)+" "+hashtagGraph.getEdgeTarget(e)+" "+hashtagGraph.getEdgeWeight(e));
			   out.newLine();
			}
			out.close();

			//Print on file the single hashtags that occur more frequently
			//order the occurences collection
			ValueComparator bvc =  new ValueComparator();
		    TreeMap<String,Integer> sorted_map = new TreeMap<String,Integer>(bvc);
		    sorted_map.putAll(occurences);
		    BufferedWriter out2 = new BufferedWriter(new FileWriter(OCCURENCESFILE));
		    for(Map.Entry<String, Integer>entry:sorted_map.entrySet()){
			    out2.write(entry.getKey()+"->"+entry.getValue());
				out2.newLine();
			}
			out2.close();

			System.out.println("Number of vertexes: "+hashtagGraph.vertexSet().size());
			System.out.println("Number of edges: "+hashtagGraph.edgeSet().size());
			System.out.println("#"+HASHTAG_TO_RETRIEVE1+": "+occ_hashtagtoretrieve);
			System.out.println("Average number of hashtags per tweet: "+(double)nr_of_hashtags/occ_hashtagtoretrieve);

			//statistics on the languages used
			System.out.println("Languages used: ");
			for(Map.Entry<String, Integer>entry:languages.entrySet()){
				System.out.println(entry.getKey()+": "+(double)entry.getValue()/n_tweets*100);
			}

		} finally {
			cursor.close();
		}
	}

	public static Comparator<DefaultWeightedEdge> EdgeComparator=new Comparator<DefaultWeightedEdge>(){
		@Override
		public int compare(DefaultWeightedEdge arg0,DefaultWeightedEdge arg1) {
			// TODO Auto-generated method stub
			double w1=hashtagGraph.getEdgeWeight(arg0);
			double w2=hashtagGraph.getEdgeWeight(arg1);
			if (w1<w2)
				return -1;
			if (w1>w2)
				return 1;
			return 0;
		}
     };

	 //comparator for the hashtags that occur more frequently
     public static class ValueComparator implements Comparator<String> {
    	    public ValueComparator() {}
    	    // Note: this comparator imposes orderings that are inconsistent with equals.
    	    public int compare(String a, String b) {
    	        if (occurences.get(a) >= occurences.get(b)) {
    	            return 1;
    	        } else {
    	            return -1;
    	        } // returning 0 would merge keys
    	    }
    }


public static void main(String[] args) throws IOException {
	// TODO Auto-generated method stub
	DB mydb=null;
	try {
		mydb=connect("localhost" , 27017);
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	retrieveTweets(mydb);
	try {
		CommunityDetection.ReadGraph(OUTPUTGRAPH);
		CommunityDetection.ComputeSimilarityMatrix(TUPLES_FILE,TUPLES_TO_INSERT);
		CommunityDetection.BuildBinaryTree(OUTPUTGRAPH,TUPLES_FILE,TUPLES_TO_INSERT);
		CommunityDetection.CountVerticesAndEdges();
		CommunityDetection.Extract(DMIN, DMAX, SUBGRAPHS_FILE);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

}





