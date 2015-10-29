package ex1;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

/*
 * class BlockSolver
 * 
 * implements run() methods.
 * Computes each of the h*v block(tiles) of the board.
 */

public class BlockSolver implements Runnable {
	private boolean[][] b_fullBoard;
	private ArrayList<ArrayList<BlockState>> b_blockState;
	private CyclicBarrier b_barrier;
	private Point b_blockCoor;
	private int b_generations;
	private final int b_numOfNeighbours;
	private SolutionPyramid b_pyramid;
	private final boolean DEBUG = false;
	
	public BlockSolver(
			boolean[][] b_fullBoard, ArrayList<ArrayList<BlockState>> b_blockState, 
			CyclicBarrier b_barrier, Point b_blockCoor, 
			int numOfNeighbours, Point b_startCoor, 
			Point b_dimension, int b_generations) {
		
		this.b_fullBoard = b_fullBoard;
		this.b_blockState = b_blockState;
		this.b_barrier = b_barrier;
		this.b_blockCoor = b_blockCoor;
		this.b_numOfNeighbours = numOfNeighbours;
		this.b_generations = b_generations;
		this.b_pyramid = new SolutionPyramid(
				b_fullBoard, b_blockState, b_numOfNeighbours, b_generations, 
				b_dimension.x, b_dimension.y, b_startCoor, b_blockCoor);
	}
	
	
	/*
	 * 
	 * The main routine for each thread.
	 * 1. init the relevant blockState - synchronization object between blocks
	 * 2. wait on barrier until all finished step 1
	 * 3. compute the first generation board
	 * 4. compute all other generations. This separation is explained in addFirstGeneration().
	 */
	@Override public void run(){
		b_blockState.get(b_blockCoor.x).get(b_blockCoor.y).init(b_numOfNeighbours, 0);
        try {
            b_barrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        b_pyramid.addFirstGeneration(b_fullBoard);
        for (int i = 1; i < b_generations; i++) {
        	try {
    			b_pyramid.computeAndAddNextGeneration();	
			} catch (Exception e) {
				e.printStackTrace();
				try {
					finalize();
				} catch (Throwable e1) {
					e1.printStackTrace();
				}
			}
			if (b_blockCoor.x == 0 && b_blockCoor.y == 0)
				print_board(i+1);
		}
	}
	
	void print_board(int gen){
		if (!DEBUG)
			return;

		System.out.println("Generation:" + gen + "\n");
		System.out.print("  ");
		for (int i = 0; i < b_fullBoard.length; i++) {
			System.out.print(i%10);
		}
		for (int i = 0; i < b_fullBoard.length; i++) {
			System.out.print("\n");
			System.out.print(i%10 + ":");
			for (int j = 0; j < b_fullBoard[0].length; j++) {
				char val = (b_fullBoard[i][j]) ? 'X' : 'O';
				System.out.print(val);
			}
		}

		System.out.print("\n\n ---------------------------------- \n\n");
		
	}
	
}