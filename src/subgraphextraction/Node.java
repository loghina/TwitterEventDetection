package subgraphextraction;
/**
 * @author Alexandru Loghin & Luise Torres
 * Class defining a node inside a binary tree
 */
public class Node {
	String name;
    int num_vertex;
    int num_edges;
    Node leftChild;
    Node rightChild;
    Node parent;
    
    Node(String _name){
    	this.name=_name;
    	num_vertex=0;
    	num_edges=0;
    	parent=null;
    }
    
    Node(String _name, Node left, Node right) {   	 
        this.name = _name;
        leftChild=left;
        rightChild=right;
        num_vertex=0;
    	num_edges=0;
    	parent=null;
    }
    
    public void setParent(Node par){
    	parent=par;
    }
    
    public Node getParent(){
    	return parent;
    }
 
    public String toString() {
        return name;
    }
    
    //linear search because the nodes are not ordered by a key
    public Node search(String n){
    	if (this.name.compareTo(n)==0)		return this;
    	if (this.leftChild!=null){
    		Node l=this.leftChild.search(n);
    		if (l!=null)	return l;
    		else{
    			if (this.rightChild!=null)	return this.rightChild.search(n);
    		}
    	}
    	return null;
    }
 
}
