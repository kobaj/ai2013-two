package grif1252;

import java.util.ArrayList;

public class AdjacencyMatrixGraph
{
	// labels that show rows come first (row major)
	final public static int ROW = 0;
	final public static int COLUMN = 1;
	
	// how many nodes are in this adjacency matrix
	final private int node_count;
	
	// the actual storage
	final private double[][] adjacency_matrix;
	
	// handy copy of nodes
	private ArrayList<Node> nodes;
	
	// all nodes are stored in adjanccy matrix
	// if two nodes are connected, the distance between them is stored in the matrix
	// if they are not connected, the distance between them is 0
	// if two nodes are the same, then they are connected.
	public AdjacencyMatrixGraph(int node_count)
	{
		if (node_count <= 0)
			node_count = 1;
		
		this.node_count = node_count;
		
		adjacency_matrix = new double[node_count][node_count];
	}
	
	public void storeNodes(ArrayList<Node> nodes)
	{
		// make a handy dandy copy of the nodes
		this.nodes = new ArrayList<Node>();
		for (Node n : nodes)
			this.nodes.add(n.copy());
	}
	
	public ArrayList<Node> getNodes()
	{
		ArrayList<Node> all_nodes = new ArrayList<Node>();
		for (Node n : nodes)
			all_nodes.add(n.copy());
		return all_nodes;
	}
	
	public void setConnected(Node A, Node B, double distance)
	{
		if (A.matrix_id == B.matrix_id)
			distance = 0;
		
		if (A.matrix_id >= node_count || B.matrix_id >= node_count)
			return;
		
		int[] rows_columns = fixRowColumn(A.matrix_id, B.matrix_id);
		
		try
		{
			adjacency_matrix[rows_columns[ROW]][rows_columns[COLUMN]] = distance;
		}
		catch (IndexOutOfBoundsException e)
		{
			System.out.println("EXCEPTION: Values for row, column, nodecount: " + rows_columns[ROW] + ":" + rows_columns[COLUMN] + ":" + node_count);
			throw (e);
		}
	}
	
	public boolean getConnected(Node A, Node B)
	{
		if (A.matrix_id == B.matrix_id)
			return true;
		
		if (A.matrix_id >= node_count || B.matrix_id >= node_count)
			return false;
		
		int[] rows_columns = fixRowColumn(A.matrix_id, B.matrix_id);
		
		try
		{
			return (adjacency_matrix[rows_columns[ROW]][rows_columns[COLUMN]] > 0.0);
		}
		catch (IndexOutOfBoundsException e)
		{
			System.out.println("EXCEPTION: Values for row, column, nodecount: " + rows_columns[ROW] + ":" + rows_columns[COLUMN] + ":" + node_count);
			throw (e);
		}
		
	}
	
	// this is not recursive. just one level deep
	public ArrayList<Node> getChildren(Node parent)
	{
		ArrayList<Node> children = new ArrayList<Node>();
		
		for (Node n : nodes)
			if (parent.matrix_id != n.matrix_id)
			{
				if (getConnected(parent, n))
				{
					n = n.copy();
					n.parent = parent;
					children.add(n);
				}
			}
		
		return children;
	}
	
	// set row major
	private int[] fixRowColumn(int row, int column)
	{
		if (row > column)
			return new int[] { row, column };
		else
			return new int[] { column, row };
	}
	
}
