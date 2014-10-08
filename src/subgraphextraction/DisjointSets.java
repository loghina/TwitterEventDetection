package subgraphextraction;

/** http://www.sanfoundry.com/java-program-implement-disjoint-set-data-structure/ 
Modified by Alexandru Loghin & Luise Torres*/

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DisjointSets 
{
    List<Map<String, Elements>> disjointSet;
    
    public DisjointSets(){
        disjointSet = new ArrayList<Map<String, Elements>>();
    }

    public void create_set(String element){
        Map<String, Elements> map = new HashMap<String, Elements>();
        Set<String> set = new HashSet<String>();
        set.add(element);
        //map.put(element, set);
        //disjointSet.add(map);
        //***********************************
        Node n=new Node(element);
        Elements elements=new Elements(set,n);
        map.put(element, elements);
        disjointSet.add(map);
    }

    public void union(String first, String second, Node n){
    	Map.Entry<String, Elements> first_rep = find_set(first,null);
    	Map.Entry<String, Elements> second_rep = find_set(second,null);
        Elements first_set = null;
        Elements second_set = null;
        for (int index = 0; index < disjointSet.size(); index++){
            Map<String, Elements> map = disjointSet.get(index);
            if (map.containsKey(first_rep.getKey()))
            {
                first_set = map.get(first_rep.getKey());
            } else if (map.containsKey(second_rep.getKey())){ 
                second_set = map.get(second_rep.getKey());
            }
        }
        if (first_set != null && second_set != null)
        	first_set.set.addAll(second_set.set);
        int rem=-1;
        for (int index = 0; index < disjointSet.size(); index++){
            Map<String, Elements> map = disjointSet.get(index);
            if (map.containsKey(first_rep.getKey())){
            	first_set.node=n;
                map.put(first_rep.getKey(), first_set);
            } else if (map.containsKey(second_rep.getKey())){
                map.remove(second_rep.getKey());
                rem=index;
            }
        }
        disjointSet.remove(rem);

        return;
    }

    public Map.Entry<String, Elements> find_set(String element, Node result){
    	
    	result=null;	
        for (int index = 0; index < disjointSet.size(); index++){
            Map<String, Elements> map = disjointSet.get(index);
            Set<String> keySet = map.keySet();
            for (String key : keySet){
                Elements elements = map.get(key);
                if (elements.set.contains(element)){   
                	return new AbstractMap.SimpleEntry<String,Elements>(key,map.get(key));
                }
            }
        }
        return null;
    }

    public int getNumberofDisjointSets(){
        return disjointSet.size();
    }
    
    //*************************************************
    public Set<Node> getRootNodes(){
    	Set<Node> roots=new HashSet<Node>();
    	for (int index = 0; index < disjointSet.size(); index++){
			Map<String, Elements> map = disjointSet.get(index);
			if (!map.isEmpty()){
				for (Map.Entry<String, Elements>entry:map.entrySet()){
					if (entry.getValue().node!=null){
						roots.add(entry.getValue().node);
						break;
					}
				}
			}
		}
    	return roots;
    }
}
//******************************************************
class Elements{
	Set<String> set;
	Node node;
	public Elements(Set<String>s,Node n){
		set=s;
		node=n;
	}
}