package ex1;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

/*
 * class WorkerMain
 * 
 * This is the main thread, creating all other worker threads,
 * and dividing the h*v blocks between them.
 */

public class WorkerMain {
	private boolean[][] w_gameBoard;
	private int w_hSplit, w_vSplit;
	private int w_generations;
	Thread[][] w_threads;
	private static int w_numberOfThreads;
	private CyclicBarrier w_barrier;
	
	public WorkerMain(boolean[][] initalField, int hSplit, int vSplit, int generations){
		w_gameBoard = initalField;
		w_vSplit = vSplit;
		w_hSplit = hSplit;
		w_generations = generations;
		w_numberOfThreads = w_vSplit*w_hSplit;
		w_threads = new Thread[w_vSplit][w_hSplit];
		w_barrier = new CyclicBarrier(w_numberOfThreads);
		ArrayList<ArrayList<BlockState>> w_blockState = new ArrayList<ArrayList<BlockState>>();
		int w_boardWidth = w_gameBoard[0].length;
		int w_boardHeight = w_gameBoard.length;


		// create all threads
		for (int i = 0; i < w_vSplit; i++) {
			ArrayList<BlockState> newRow = new ArrayList<BlockState>();
			w_blockState.add(newRow);
			for (int j = 0; j < w_hSplit; j++) {
				Point th_index = new Point(i,j);
				Point th_startCoor = new Point(w_boardHeight/w_vSplit*i,w_boardWidth/w_hSplit*j);
				// last in a row/col gets the remainder	
				int h_dim = (i == w_vSplit-1) ? w_boardHeight-(w_boardHeight/w_vSplit*i) : w_boardHeight/w_vSplit; 
				int w_dim = (j == w_hSplit-1) ? w_boardWidth-(w_boardWidth/w_hSplit*j) : w_boardWidth/w_hSplit;
				Point th_dimension = new Point(h_dim, w_dim); 
				newRow.add(new BlockState());
				w_threads[i][j] = new Thread(new BlockSolver(initalField, w_blockState, w_barrier, th_index, numOfNeighbours(i,j), th_startCoor, th_dimension, w_generations));
			}
		}

		// run all threads
		for (int i = 0; i < w_vSplit; i++) {
			for (int j = 0; j < w_hSplit; j++) {
				w_threads[i][j].start();
			}
		}
		
		// wait to all threads to finish
		while (true) {
		    try {
				for (int i = 0; i < w_vSplit; i++) {
					for (int j = 0; j < w_hSplit; j++) {
						w_threads[i][j].join();
					}
				}
		        break;
		    }
		    catch (InterruptedException e) {
		        e.printStackTrace();
		    }
		}
	}

	/* numOfNeighbours
	 * Returns the amount of neighbors for the tile (i,j),
	 * by simple boundary checking
	 * 
	 */
	private int numOfNeighbours(int i, int j) {
		int numOfNegibours = 0;
		
		if (i>0)							numOfNegibours++;
		if (i<w_vSplit-1)					numOfNegibours++;
		if (j>0)							numOfNegibours++;
		if (j<w_hSplit-1)					numOfNegibours++;
		if (i>0 && j>0)						numOfNegibours++;
		if (i<w_vSplit-1 && j<w_hSplit-1)	numOfNegibours++;
		if (i>0 && j<w_hSplit-1)			numOfNegibours++;
		if (i<w_vSplit-1 && j>0)			numOfNegibours++;
		
		return numOfNegibours;
	}
		
	/*
	 * getResult
	 * Returns the game board
	 */
	public boolean[][] getResult(){
		return w_gameBoard;
	}
	
	@SuppressWarnings("unused")
	private void print_board(){
		System.out.print("  ");
		for (int i = 0; i < w_gameBoard.length; i++) {
			System.out.print(i%10);
		}
		for (int i = 0; i < w_gameBoard.length; i++) {
			System.out.print("\n");
			System.out.print(i%10 + ":");
			for (int j = 0; j < w_gameBoard[0].length; j++) {
				char val = (w_gameBoard[i][j]) ? 'X' : 'O';
				System.out.print(val);
			}
		}

		System.out.print("\n\n ---------------------------------- \n\n");
		
	}
}
