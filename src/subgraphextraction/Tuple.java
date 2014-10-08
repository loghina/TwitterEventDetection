package subgraphextraction;
/**
 * @author Alexandru Loghin & Luise Torres
 * Class defining a sparse matrix element
 */
public class Tuple implements Comparable{
	private String i,j;
	private double mij;
	public Tuple(String _i, String _j, double _mij){
		i=_i;
		j=_j;
		mij=_mij;
	}
	
	public String geti(){
		return i;
	}
	public String getj(){
		return j;
	}
	public double getMij(){
		return mij;
	}

	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		if (((Tuple)arg0).getMij()>mij)
			return 1;
		if (((Tuple)arg0).getMij()<mij)
			return -1;
		return 0;
	}
}