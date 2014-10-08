package subgraphextraction;

public class Hashtag {
	private String name;
	private int occurences;
	
	public Hashtag(String _name){
		name=_name.toLowerCase();
		occurences=1;
	}
	
	public void addOccurence(){
		occurences++;
	}
	
	public String toString(){
		return name;
	}
	
	public int nrOccurences(){
		return occurences;
	}
}
