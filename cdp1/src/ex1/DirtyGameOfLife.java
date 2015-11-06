package ex1;

import java.awt.Point;
import java.util.LinkedList;

/***
 * Parallel version of Game Of Life
 * Dirty because everything is in a single class file
 * Basically, the algorithm is dividing the world map into sections
 * and have each section be processed by a thread
 *
 * The main loop launches the threads, waits for them to finish their cycle
 * and then switches between the nextWorld and currWorld map arrays
 * and notifies them to execute the next generation.
 *
 * @author Yacov Manevich & Arieh Leviav
 *
 */

public class DirtyGameOfLife implements GameOfLife {

	/**
	 * World maps. True for alive cell and false for dead cell
	 * Volatile because the main loop switches their values each iteration.
	 */
	private volatile boolean[][] _nextWorld;
	private volatile boolean[][] _currWorld;

	/**
	 * The division of the world map to threads
	 */
	private LinkedList<WorldSection> _worldSections = new LinkedList<WorldSection>();

	/**
	 * Semaphore that only workers release, and the main thread acquires.
	 */
	private Semaphore _workerSem = new Semaphore(0);

	/**
	 * Semaphore that the main thread releases and the workers acquire.
	 */
	private Semaphore _genSem = new Semaphore(0);

	/**
	 * The number of generations left until world destruction
	 */
	private volatile int _generations;

	/**
	 * debug
	 */
	private static final boolean debug = (System.getenv("DEBUG") != null);

	@Override
	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit, int generations) {

		if (generations == 0) {
			return new boolean[][][]{initalField,initalField};
		}

		_generations = generations;
		createWorld(initalField);

		// split the world map to WorldSections
		_worldSections = splitToSections(hSplit, vSplit, initalField[0].length, initalField.length);

		// backup _worldSections
		LinkedList<WorldSection> sections = new LinkedList<WorldSection>();
		sections.addAll(_worldSections);

		int workerNum = _worldSections.size();

		Worker[] workers = new Worker[workerNum];

		// launch all threads
		for (int i = 0; i < workerNum; i++) {
			workers[i] = new Worker();
		}

		while (_generations > 0) {
			// wait for workers to finish
			_workerSem.acquire(workerNum);
			// switch _currWorld and _nextWorld

			boolean[][] tmp = _currWorld;
			_currWorld = _nextWorld;
			_nextWorld = tmp;

			// restore sections
			_worldSections.addAll(sections);

			// advance gen and signal workers to advance to next gen
			_generations--;
			_genSem.release(workerNum);
		}

		_workerSem.acquire(workerNum);

		return new boolean[][][] { _nextWorld, _currWorld };
	}


	private void createWorld(boolean[][] initalField) {
		_nextWorld = new boolean[initalField.length][initalField[0].length];
		_currWorld = new boolean[initalField.length][initalField[0].length];
		for (int i = 0; i < initalField.length; i++) {
			_currWorld[i] = new boolean[initalField[i].length];
			_nextWorld[i] = new boolean[initalField[i].length];
		}

		for (int i = 0; i < initalField.length; i++) {
			System.arraycopy(initalField[i], 0, _currWorld[i], 0, initalField[i].length);
		}
	}

	/**
	 * Ugly indices game
	 */
	private static LinkedList<WorldSection> splitToSections(int hSplit, int vSplit, int boardWidth, int boardHeight) {
		int width = (int)Math.floor((double) boardWidth / hSplit);
		int height = (int)Math.floor((double) boardHeight / vSplit);

		LinkedList<WorldSection> sections = new LinkedList<>();
		dbg("width: " + width + ", height: " + height + ", vSplit: " + vSplit + ", hSplit: " + hSplit);
		for (int i = 0; i < hSplit; i++) {
			int effectiveWidth = i + 1 < hSplit ? width : boardWidth - i*width;

			for (int j = 0; j < vSplit; j++) {
				WorldSection section = new WorldSection();

				int effectiveHeight = j + 1 < vSplit ? height : boardHeight - j*height;
				for (int l = 0; l < effectiveWidth; l++) {
					for (int k = 0; k < effectiveHeight; k++) {

						if (k == 0 && l ==0) {
							Cell cell = new Cell();
							cell.x = j * height + k;
							cell.y = i * width + l;
							section.setLeftTop(cell);
						}
//						else if (k == 0 && l+1 == effectiveWidth) {
//							Cell cell = new Cell();
//							cell.x = j * height + k;
//							cell.y = i * width + l;
//							section.setRightTop(cell);
//
//						}
//						else if (k+1 == effectiveHeight && l == 0) {
//							Cell cell = new Cell();
//							cell.x = j * height + k;
//							cell.y = i * width + l;
//							section.setRightBottom(cell);
//						}
						else if (k+1 == effectiveHeight && l+1 == effectiveWidth) {
							Cell cell = new Cell();
							cell.x = j * height + k;
							cell.y = i * width + l;
							section.setRightBottom(cell);
						}
					}
				}
//				if (!section.cells.isEmpty())
					sections.add(section);
			}
		}

		return sections;
	}


	/**
	 * The worker class
	 *
	 */
	private class Worker implements Runnable {

		Worker() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			dbg("Started");
				while (_generations > 0) {
					while (true) {
						WorldSection section = null;
						synchronized (_worldSections) {
							if (_worldSections.size() > 0) {
								section = _worldSections.removeFirst();
							} else {
								break;
							}
						}
						try {
							processSection(section);
							Thread.yield();
						} catch (Exception e) {
							// don't make a fuss over this small thing
						}
					}
					_workerSem.release(1);
					_genSem.acquire(1);
				}
				_workerSem.release(1);
		}

		/**
		 * Mutate the world map according to exercise logic
		 * @param section the set of cells in the section
		 */
		private void processSection(WorldSection section) {
//			dbg("Processing " + section.cells);
//			for (Cell cell : section.cells) {
			for (int x = section.getLeftTop().x; x <= section.getRightBottom().x; x++) {
				for (int y = section.getLeftTop().y; y <= section.getRightBottom().y; y++) {
//					dbg(cell.toString());
					int numNeighbors = numNeighbors(x, y, _currWorld);
					if (_currWorld[x][y]) { // alive
						if (numNeighbors == 3 || numNeighbors == 2) {
							_nextWorld[x][y] = true;
						} else {
							_nextWorld[x][y] = false;
						}
					} else { // dead
						if (numNeighbors == 3) {
							_nextWorld[x][y] = true;
						} else {
							_nextWorld[x][y] = false;
						}
					}
				}
			}
//			}
		}
	}

	/**
	 * A set of cells which represents a continuous
	 * subset of the world map
	 */
	private static class WorldSection {
		//		private List<Cell> cells = new LinkedList<Cell>();
		private Cell leftTop;
//		private Cell rightTop;
//		private Cell leftBottom;
		private Cell rightBottom;

		public void setLeftTop(Cell leftTop) { this.leftTop = leftTop; }
//		public void setRightTop(Cell rightTop) { this.rightTop = rightTop; }
//		public void setLeftBottom(Cell leftBottom) { this.leftBottom = leftBottom; }
		public void setRightBottom(Cell rightBottom) { this.rightBottom = rightBottom; }

		public Cell getLeftTop() { return leftTop; }
//		public Cell getRightTop() { return rightTop; }
//		public Cell getLeftBottom() { return leftBottom; }
		public Cell getRightBottom() { return rightBottom; }
	}

	@SuppressWarnings("serial")
	private static class Cell extends Point {
		public String toString() {
			return "(" + x + "," + y + ")";
		}
	}

	/**
	 * Copied from the exercise code
	 */
	private static int numNeighbors(int x, int y, boolean[][] field) {
		int counter = (field[x][y] ? -1 : 0);

		for (int i = (x - 1 + field.length); i < (x + 2 + field.length); ++i) {
			for (int j = (y - 1 + field[0].length); j < (y + 2 + field[0].length); j++) {
				counter += (field[i % field.length][j % field[0].length] ? 1 : 0);
			}
		}
		return counter;
	}

	/**
	 * A Semaphore implementation
	 */
	private static class Semaphore {
		private int _permits;

		Semaphore(int permits) {
			_permits = permits;
		}

		void acquire(int permits) {
			dbg("Entering acquire(" + permits + ") on " + this.hashCode());
			synchronized (this) {
				while (_permits < permits) {
					vait(this);
				}
				_permits -= permits;
			}
			dbg("Exiting");
		}

		void release(int permits) {
			dbg("Entering release(" + permits + ") on " + this.hashCode());
			synchronized (this) {
				_permits += permits;
				notifyAll();
			}
			dbg("Exiting");
		}
	}

	private static void vait(Object o) {
		try {
			o.wait();
		} catch (InterruptedException e) {
		}
	}

	private static void dbg(String msg) {
		// For branch prediction, debug will most likely not be used in the tests
		if (!debug) {
			return;
		}
		System.out.println(Thread.currentThread().getId() + ": " + msg);
	}

	@SuppressWarnings("unused")
	private static void print_board(boolean w_gameBoard[][]) {
		System.out.print("  ");
		for (int i = 0; i < w_gameBoard.length; i++) {
			System.out.print(i % 10);
		}
		for (int i = 0; i < w_gameBoard.length; i++) {
			System.out.print("\n");
			System.out.print(i % 10 + ":");
			for (int j = 0; j < w_gameBoard[0].length; j++) {
				char val = (w_gameBoard[i][j]) ? 'X' : '-';
				System.out.print(val + " ");
			}
		}
		System.out.print("\n\n ---------------------------------- \n\n");
	}

}
