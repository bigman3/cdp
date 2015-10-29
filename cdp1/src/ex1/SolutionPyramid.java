package ex1;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

/*
 * SolutionPyramid
 * 
 * a pyramid of game boards, where the first one represents 
 * the first generation game result.
 * Game result from generation n is represented partially by
 * the n-th board excluding n width envelope of cells around it.
 * 
 */

public class SolutionPyramid {
	private boolean[][] p_fullBoard;
	private ArrayList<ArrayList<BlockState>> p_blockState;
	private int p_generations;
	private LinkedList<boolean[][]> p_pyramid;
	private int p_blockHeight;
	private int p_blockWidth;
	private int p_fullBoardHeight;
	private int p_fullBoardWidth;
	private Point p_blockStartCoor;
	private Point p_blockCoor;
	private int p_numOfNeighbours;
	private final boolean DEBUG = false;
	
	public SolutionPyramid(
			boolean[][] p_fullBoard, ArrayList<ArrayList<BlockState>> p_blockState, 
			int numOfNeighbours, int p_generations, int p_blockHeight, 
			int p_blockWidth, Point p_blockStartCoor, Point p_blockCoor) {
		
		this.p_fullBoard = p_fullBoard;
		this.p_blockState = p_blockState;  
		this.p_generations = p_generations;
		this.p_blockHeight = p_blockHeight;
		this.p_blockWidth = p_blockWidth;
		this.p_fullBoardHeight = p_fullBoard.length;
		this.p_fullBoardWidth = p_fullBoard[0].length;
		this.p_blockStartCoor = new Point(p_blockStartCoor);
		this.p_blockCoor = new Point(p_blockCoor);
		this.p_numOfNeighbours = numOfNeighbours;
		p_pyramid = new LinkedList<>();
	}
	
	/*
	 * addFirstGeneration
	 * 
	 * initialize the pyramid with amount of generations boards 
	 * in the size of single block and compute all boards (full and partials) 
	 * the first generation.
	 * Update the game board from generation 1 with the block board.
	 */
	public void addFirstGeneration(boolean[][] fullBoard) {
		int currHeight = p_blockHeight;
		int currWidth = p_blockWidth;

		p_pyramid.addLast(extractRelativeBlock(fullBoard, p_blockStartCoor, p_blockHeight, p_blockWidth));
		
		while ((currHeight > 0) && (currWidth > 0)) {
			// add some condition on p_generations left to limit list size
			boolean[][] currBoard = new boolean[p_blockHeight][p_blockWidth];
			int horMargin = (p_blockHeight-currHeight)/2;
			int verMargin = (p_blockWidth-currWidth)/2;
			for (int i = horMargin; i < p_blockHeight-horMargin; i++) {
				for (int j = verMargin; j < p_blockWidth-verMargin; j++) {
					// fill according to lower level state
					currBoard[i][j] = calcCell(i, j, p_pyramid.getLast(), p_blockState);
				}
			}
			// free neighbors right after filling the lowest level
			if (currHeight == p_blockHeight && currWidth == p_blockWidth)
				freeAllNeighbours();
			p_pyramid.addLast(currBoard);
			currHeight -= 2;
			currWidth -= 2;
		}
		
		// write first level (skip prev state) to this.p_fullBoard
		p_pyramid.removeFirst();
		writeBoard(p_pyramid.getFirst());
	}
	
	/*
	 * computeAndAddNextGeneration
	 * 
	 * compute all boards in the pyramid (of the partial block) for
	 * next generation.
	 * 
	 *  Update the game board with the block board.
	 */
	public void computeAndAddNextGeneration() throws Exception {
		int currMarginSize = 0;
		ListIterator<boolean[][]> itr = p_pyramid.listIterator();
		boolean[][] prev;
		
		// p_pyramid was already initialized
		if (itr.hasNext())
			prev = itr.next();
		else
			throw(new Exception("No first node in List!"));
		
		while(itr.hasNext())
		{
			boolean[][] curr = itr.next(); 
			for (int i = currMarginSize; i < p_blockHeight-currMarginSize; i++) {
				curr[i][currMarginSize] = calcCell(i, currMarginSize, prev, p_blockState);
				curr[i][(p_blockWidth-1)-currMarginSize] = calcCell(i, ((p_blockWidth-1)-currMarginSize), prev, p_blockState);
			} 
			for (int j = currMarginSize; j < p_blockWidth-currMarginSize; j++) {
				curr[currMarginSize][j] = calcCell(currMarginSize, j, prev, p_blockState);
				curr[(p_blockHeight-1)-currMarginSize][j] = calcCell(((p_blockHeight-1)-currMarginSize), j, prev, p_blockState);
			}
			if (currMarginSize == 0)
				freeAllNeighbours();
			prev = curr;
			currMarginSize++;
		}
		
		boolean[][] newBoard = new boolean[p_blockHeight][p_blockWidth];
		
		for (int i = currMarginSize; i < p_blockHeight-currMarginSize; i++)
			for (int j = currMarginSize; j < p_blockWidth-currMarginSize; j++)
				newBoard[i][j] = calcCell(i, j, p_pyramid.getLast(), p_blockState);		
		
		p_pyramid.addLast(newBoard);

		p_pyramid.removeFirst();
		writeBoard(p_pyramid.getFirst());
		
	}

	
	/*
	 * calcCell
	 * 
	 * calculate game cell liveness
	 */
	private boolean calcCell(int i, int j, boolean[][] prevBoard, ArrayList<ArrayList<BlockState>> blockState) {
		int liveCells = 0;
		for (int k = i-1; k <= i+1; k++) {
			for (int m = j-1; m <= j+1; m++) {
				if (k != i || m != j) {
					if (i == 0 || j == 0 || i == p_blockHeight-1 || j == p_blockWidth-1) {
						if (p_blockStartCoor.x+k<0 || p_blockStartCoor.y+m<0 || k+p_blockStartCoor.x>=p_fullBoardHeight || m+p_blockStartCoor.y>=p_fullBoardWidth)
							liveCells += 0; // out of p_fullBoard bounds
						else if (k<0 && m<0)
							liveCells += requestCell(blockState.get(p_blockCoor.x-1).get(p_blockCoor.y-1), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						
						else if (k<0 && m>=p_blockWidth)
							liveCells += requestCell(blockState.get(p_blockCoor.x-1).get(p_blockCoor.y+1), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						else if (k>=p_blockHeight && m<0)
							liveCells += requestCell(blockState.get(p_blockCoor.x+1).get(p_blockCoor.y-1), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						else if (k>=p_blockHeight && m>=p_blockWidth)
							liveCells += requestCell(blockState.get(p_blockCoor.x+1).get(p_blockCoor.y+1), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						else if (k<0)
							liveCells += requestCell(blockState.get(p_blockCoor.x-1).get(p_blockCoor.y), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						else if (m<0)
							liveCells += requestCell(blockState.get(p_blockCoor.x).get(p_blockCoor.y-1), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						else if (k>=p_blockHeight)
							liveCells += requestCell(blockState.get(p_blockCoor.x+1).get(p_blockCoor.y), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						else if (m>=p_blockWidth)
							liveCells += requestCell(blockState.get(p_blockCoor.x).get(p_blockCoor.y+1), new Point(p_blockStartCoor.x+k, p_blockStartCoor.y+m));//, prevp_fullBoard);
						else
							liveCells += (prevBoard[k][m]) ? 1 : 0;
					} else { // skip all edge cases
						liveCells += (prevBoard[k][m]) ? 1 : 0;
					}
				}
			}
		}
		// according to game's rules
//		if (p_blockCoor.x == 0 && p_blockCoor.y == 0)
//			System.out.print("####" + "i = " + i + " & j = " + j + "####\n");
		return ((prevBoard[i][j] && liveCells < 4 && liveCells > 1) ||
				(!(prevBoard[i][j]) && liveCells == 3));
	}
	
	// request cell value from neighbor block    
	private int requestCell(BlockState neighborBlockState, Point globCoor) { //, boolean[][] p_fullBoard
		synchronized (neighborBlockState) {
			boolean flag = false;
			// wait while neighbor holds block for writing
//			System.out.print("Reading:" + glob_coor + "\n");
			// wait until neighbor is in the same generation and readable state 
			while (!neighborBlockState.isReadable() || neighborBlockState.getGeneration() != p_blockState.get(p_blockCoor.x).get(p_blockCoor.y).getGeneration()) {
	            try{
	            	flag = true;
	            	printDebug("requestCell: Waiting for block_readOnly for global-coor: " + globCoor.x + "," + globCoor.y + " to complete...");
	                neighborBlockState.wait();
	            }catch(InterruptedException e){
	                e.printStackTrace();
	            }
			}
			if (flag)
				printDebug("requestCell: Finished waiting for global-coor: " + globCoor.x + "," + globCoor.y + " to complete...");
			return (p_fullBoard[globCoor.x][globCoor.y]) ? 1 : 0 ;
		}
	}
	
	// actively change readOnly to false (for neighbors to write)
	private void freeAllNeighbours() {
		for (int i = p_blockCoor.x-1; i <= p_blockCoor.x+1; i++) {
			for (int j = p_blockCoor.y-1; j <= p_blockCoor.y+1; j++) {
				if (i != p_blockCoor.x || j != p_blockCoor.y) {
					if (i>=0 & i<p_blockState.size() && j>=0 && j<p_blockState.get(0).size()) {
						synchronized (p_blockState.get(i).get(j)) {
//							if (p_fullReadOnly[i][j].get()) {
//								System.out.print("inside lock: " + i + "," + j + " - values is: " + p_fullReadOnly[i][j].get());
								p_blockState.get(i).get(j).decNumOfNeigbours();
								p_blockState.get(i).get(j).notifyAll();
//							}
						}
					}
				}
			}
		}
	}
	
	/*
	 * update full board with partial block
	 */
	private void writeBoard(boolean[][] updateBlock) {
		synchronized (p_blockState.get(p_blockCoor.x).get(p_blockCoor.y)) {
			boolean flag = false;
			while (!p_blockState.get(p_blockCoor.x).get(p_blockCoor.y).isWritable()) {
	            try{
	            	flag = true;
	            	printDebug("Write-Back: Waiting for block_readOnly[" + p_blockCoor.x + "," + p_blockCoor.y + "] to become writable");
	            	p_blockState.get(p_blockCoor.x).get(p_blockCoor.y).wait();
	            }catch(InterruptedException e){
	                e.printStackTrace();
	            }
			}
			for (int i = 0; i < p_blockHeight; i++) {
				for (int j = 0; j < p_blockWidth; j++) {
					p_fullBoard[p_blockStartCoor.x+i][p_blockStartCoor.y+j] = updateBlock[i][j];
				}
			}
			if (flag)
				printDebug("Write-Back: Finished waiting for block_readOnly[" + p_blockCoor.x + "," + p_blockCoor.y + "] to become writable");
			// actively turn the block to read-only for neighbors use
//			if (block_readOnly.get())
			p_blockState.get(p_blockCoor.x).get(p_blockCoor.y).incGeneration();
			p_blockState.get(p_blockCoor.x).get(p_blockCoor.y).setNumOfNeigbours(p_numOfNeighbours);
			p_blockState.get(p_blockCoor.x).get(p_blockCoor.y).notifyAll();
		}
	}
	
	private boolean[][] extractRelativeBlock(boolean[][] fullBoard, Point blockStartCoor, int blockHeight, int blockWidth) {
		boolean[][] newBlock = new boolean[blockHeight][blockWidth];
		for (int i = 0; i < blockHeight; i++)
			for (int j = 0; j < blockWidth; j++)
				newBlock[i][j] = fullBoard[i+blockStartCoor.x][j+blockStartCoor.y];
		
		return newBlock;
	}
	
	private void printDebug(String str){
		if (DEBUG)
			System.out.println(str);
	}
}
