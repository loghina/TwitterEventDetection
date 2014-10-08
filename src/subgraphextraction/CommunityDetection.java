package subgraphextraction;

/**	Alexandru Loghin & Luise Torres
 * implementation of the algorithm for dense subgraph extraction proposed by: 
 * "Dense subgraph extraction with application to community detection"	by Jie Chen and Yousef Saad
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.*;

public class CommunityDetection {
	/* Consider the graph as undirected */
	private static SimpleGraph<String, DefaultWeightedEdge> myGraph=new SimpleGraph<String,DefaultWeightedEdge>(DefaultWeightedEdge.class);
	/*Tuples are in the format: (node_i node_j mij) where mij is the value in the similarity matrix*/
	private static TreeSet<Tuple>C;
	/*Combination of union-find data structure with binary tree -->
	 * used for the implementation of the dendogram*/
	private static DisjointSets disjointSet;
	private static int subgraphs=0;
	public CommunityDetection(SimpleGraph<String, DefaultWeightedEdge> hashtagGraph){
		myGraph=hashtagGraph;
	}
	
	public static void ReadGraph(String filename) throws IOException{
		//read the adjacency matrix of the sparsed graph
		//the file must contain just a pair of nodes (i j) if i and j are connected
		//weight is ignored at this step
		myGraph=new SimpleGraph<String,DefaultWeightedEdge>(DefaultWeightedEdge.class);
		BufferedReader br = new BufferedReader(new FileReader(filename));
	    try {
	        String line = br.readLine();
	        while (line != null) {
	        	StringTokenizer token=new StringTokenizer(line);
		        if (token.countTokens()>=2){
		        	String node1=token.nextToken();
		        	String node2=token.nextToken();
		        			        	
		        	if (!myGraph.containsVertex(node1))
		        		myGraph.addVertex(node1);
		        	if (!myGraph.containsVertex(node2))
		        		myGraph.addVertex(node2);
		        	if (!myGraph.containsEdge(node2, node1))	//add the same edge just once
		        		myGraph.addEdge(node1, node2);
		        }
	            line = br.readLine();
	        }
	    } finally {
	        br.close();
	    }
	}
	
	@SuppressWarnings("unchecked")
	public static void ComputeSimilarityMatrix(String outputFilename, int nr_tuples) throws IOException{
		//output matrix as non zero tuples (i,j,Mij)
		Set<DefaultWeightedEdge> edges_r;
		Set<DefaultWeightedEdge> edges_c;
		Set<String> nodes;
		Set<String> connected_nodes=new HashSet<String>();
		double perc, norm_r, norm_c, res;
		System.out.println("Compute "+nr_tuples+" tuples");
		C=new TreeSet<Tuple>();
		int count=0;
		nodes=myGraph.vertexSet();
		for (String node_r:nodes){
			count++;
			if (count%1000==0){
				perc=(double)count/nodes.size()*100;
				System.out.println("Similarity matrix: "+perc +"%");
				//call the garbage collector once in a while to avoid memory overload
				System.gc();
			}
			edges_r=myGraph.edgesOf(node_r);
			norm_r=1.0/Math.sqrt(edges_r.size());
			connected_nodes.clear();
			for(DefaultWeightedEdge e:edges_r){
				String src=myGraph.getEdgeSource(e);
				if (src.compareTo(node_r)!=0)
					connected_nodes.add(src);
				else{
					String trg=myGraph.getEdgeTarget(e);
					if (trg.compareTo(node_r)!=0)
						connected_nodes.add(trg);
				}
			}
			for (String node_c:nodes){
				if(node_r.compareTo(node_c)>0){
					edges_c=myGraph.edgesOf(node_c);
					norm_c=1.0/Math.sqrt(edges_c.size());					
					res=0;
					for(DefaultWeightedEdge e:edges_c){
						String src=myGraph.getEdgeSource(e);
						if (src.compareTo(node_r)!=0 && src.compareTo(node_c)!=0){
							if (connected_nodes.contains(src)){
								res += norm_r*norm_c;
							}
						}else{
							String trg=myGraph.getEdgeTarget(e);
							if (trg.compareTo(node_r)!=0 && trg.compareTo(node_c)!=0)
								if (connected_nodes.contains(trg)){
									res += norm_r*norm_c;
								}
						}
					}
					if (res!=0){
						Tuple t=new Tuple(node_r,node_c,res);
						C.add(t);
					}
				}
			}
		}
		System.out.println("Created "+C.size()+" tuples");
		
		//write the tuples of C in a text file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFilename));		   
		int cont=0;
		for (Tuple t:C){
			out.write(t.geti()+" "+t.getj()+" "+String.format("%.5f",t.getMij()));
			out.newLine();
			cont++;
			if (cont>=nr_tuples)	break;
		}
		out.close();
    }
	
	@SuppressWarnings("unused")
	public static void BuildBinaryTree(String outputGraph, String filename, int nrTuplesToExtract) throws IOException{
	  Set<String> nodes=myGraph.vertexSet();
	  disjointSet = new DisjointSets();
		
	  if (myGraph==null)
		  ReadGraph(outputGraph);
	  
	  if (C==null){
		//read tuples from a file
		C=new TreeSet<Tuple>();
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new FileReader(filename));
	    try {
	        String line = br.readLine();
	        while (line != null) {
	        	StringTokenizer token=new StringTokenizer(line);
		        if (token.countTokens()==3){
		        	String s1=token.nextToken();
		        	String s2=token.nextToken();
		        	double mij=Double.valueOf(token.nextToken());
		        	Tuple t=new Tuple(s1,s2,mij);
		        	C.add(t);
		        }
		        line = br.readLine();
		    }
	    }catch(Exception e){}
	  }
		
	  for (String node:nodes){
		disjointSet.create_set(node);
	  }
		
	  int count=0;
	  for (Tuple t:C){
			Node n1 = null,n2=null;
			Map.Entry<String, Elements> si=disjointSet.find_set(t.geti(),n1);
			Map.Entry<String, Elements> sj=disjointSet.find_set(t.getj(),n2);
			if (si.getKey().compareTo(sj.getKey())!=0){
				if (si.getValue().node!=null && sj.getValue().node!=null){
					Node n=new Node(si.getKey().concat(sj.getKey()),si.getValue().node,sj.getValue().node);
					si.getValue().node.setParent(n);
					sj.getValue().node.setParent(n);
					disjointSet.union(si.getKey(), sj.getKey(),n);
				}
			}
			count++;
			if (count%10000==0){
				double perc=(double)count/C.size()*100;
				System.out.println("Binary tree construction: "+perc +"%");
				System.gc();
			}
			if (nrTuplesToExtract>0 && count>nrTuplesToExtract)
					break;
	  }
	  System.out.println("Finished to build Binary tree");
	}
	
	public static void CountVerticesAndEdges(){
		Node i = null,j=null;
		System.out.println("Counting vertices and edges...");
		int count=0;
		Set<Node>roots=disjointSet.getRootNodes();
		
		for(DefaultWeightedEdge e:myGraph.edgeSet()){
			count++;
			if (count%10000==0){
				double perc=(double)count/myGraph.edgeSet().size()*100;
				System.out.println("Counting vertices and edges: "+perc +"%");
				System.gc();
			}
			for (Node r:roots){
				i=r.search(myGraph.getEdgeSource(e));
				if (i!=null)	break;
			}
			for (Node r:roots){
				j=r.search(myGraph.getEdgeTarget(e));
				if (j!=null)	break;
			}
			
			if (i!=null && j!=null){
				//find the LCA(Lowest Common Ancestor)
				Node n=LCA(i,j);
				if (n!=null)
					n.num_edges++;
			}
		}
		for (Node r:roots){
			CountVerticesAndEdgesWrapUp(r);
		}		
	}
	
	private static Node LCA(Node i, Node j){
		
		List<String> ancestors_i=new ArrayList<String>();
		List<String> ancestors_j=new ArrayList<String>();
		Node k=i;
		while (k.getParent()!=null){
			ancestors_i.add(k.getParent().name);
			k=k.getParent();
		}
		while (j.getParent()!=null){
			ancestors_j.add(j.getParent().name);
			j=j.getParent();
		}
		for (String s:ancestors_i){
			if (ancestors_j.contains(s)){
				while (i.getParent()!=null){
					if (i.name.compareTo(s)==0)
						return i;
					i=i.getParent();
				}
				break;
			}
		}
		return null;
	}
	
	private static void CountVerticesAndEdgesWrapUp(Node root){
		if (root.leftChild!=null && root.rightChild!=null){
			CountVerticesAndEdgesWrapUp(root.leftChild);
			CountVerticesAndEdgesWrapUp(root.rightChild);
		}
		if (root.leftChild!=null && root.rightChild!=null){
			
			root.num_vertex=root.leftChild.num_vertex + root.rightChild.num_vertex;
			root.num_edges=root.leftChild.num_edges + root.rightChild.num_edges+root.num_edges;
		}else{
			root.num_vertex=1;
		}
	}
	
	public static void Extract(double dmin,double dmax, String outputfilename){
		System.out.println("Extracting the subgraphs...");
		Set<Node>roots=disjointSet.getRootNodes();
		for (Node r:roots){
			ExtractSubgraphs(r,dmin,dmax,outputfilename);
		}
		System.out.println("done");
	}
	
	private static void ExtractSubgraphs(Node r,double dmin,double dmax,String outputfilename){
		//calculate node density
		double density;
		if (r.num_vertex<=1)
			density=0;
		else
			density=(double)r.num_edges/(r.num_vertex*(r.num_vertex-1)/2);
		if (density>dmin && density<=dmax){
			//output the leaves
			try{
			    FileWriter fw = new FileWriter(outputfilename,true); //the true will append the new data
			    subgraphs++;
			    fw.write("\nSubgraph "+subgraphs+": density "+String.format("%.5f",density)+"\n");
			    fw.close();
			    System.out.println("Found subgraph "+subgraphs);
			}catch(IOException ioe){
			    System.err.println("IOException: " + ioe.getMessage());
			}
			TraverseTree(r,outputfilename);	
		}else{
			if (r.leftChild!=null && r.rightChild!=null){
				ExtractSubgraphs(r.leftChild,dmin,dmax,outputfilename);
				ExtractSubgraphs(r.rightChild,dmin,dmax,outputfilename);
			}
		}
	}
	
	private static void TraverseTree(Node root,String outputfilename){
		if (root!=null){
			if (root.leftChild == null && root.rightChild==null) {
				try{
				    FileWriter fw = new FileWriter(outputfilename,true); 
				    fw.write(root.name+"\n");//appends the string to the file
				    fw.close();
				}catch(IOException ioe){
				    System.err.println("IOException: " + ioe.getMessage());
				}
			}else{
				TraverseTree(root.leftChild,outputfilename);
				TraverseTree(root.rightChild,outputfilename);
			}
		}
	}
}