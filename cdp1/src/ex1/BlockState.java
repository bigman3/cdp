package ex1;

/*
 * class BlockState
 * 
 * Holds current state regarding the blocks' amount of neighbors
 * and the current computed generation for same block.
 * 
 */
public class BlockState {
	private int numOfNeigbours;
	private int generation;
	
	public BlockState(int numOfNeighbours, int generation) {
		this.numOfNeigbours = numOfNeighbours;
		this.generation = generation;
	}
	
	public BlockState() {
		this.numOfNeigbours = 0;
		this.generation = 0;
	}
	
	public void init(int numOfNeighbours, int generation) {
		this.numOfNeigbours = numOfNeighbours;
		this.generation = generation;
	}
	
	public int getNumOfNeigbours() {
		return numOfNeigbours;
	}
	
	public void setNumOfNeigbours(int numOfNeigbours) {
		this.numOfNeigbours = numOfNeigbours;
	}
	public void decNumOfNeigbours() {
		this.numOfNeigbours--;
	}
	int getGeneration() {
		return generation;
	}
	void incGeneration() {
		this.generation++;
	}
	boolean isWritable() {
		return (numOfNeigbours == 0);
	}
	boolean isReadable() {
		return (numOfNeigbours > 0);
	}
}
