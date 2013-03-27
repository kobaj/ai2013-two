package grif1252;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node>
{
	
	/**
	 * compare two nodes to sort them based on f(n);
	 */
	@Override
	public int compare(Node o1, Node o2)
	{
		if (o1.fn() < o2.fn())
			return -1;
		else if (o1.fn() > o2.fn())
			return 1;
		else
			return 0;
	}
}
