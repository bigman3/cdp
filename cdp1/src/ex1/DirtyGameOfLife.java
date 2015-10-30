package ex1;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

public class DirtyGameOfLife implements GameOfLife {

	private volatile boolean[][] _nextWorld;
	private volatile boolean[][] _currWorld;
	private LinkedList<WorldSection> _worldSections = new LinkedList<WorldSection>();
	private Semaphore _workerSem = new Semaphore(0);
	private Semaphore _genSem = new Semaphore(0);
	private volatile int _generations;
	private static final boolean debug = (System.getenv("DEBUG") != null);

	@Override
	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit, int generations) {
		_generations = generations;
		createWorld(initalField);

//		print_board(initalField);

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

//			print_board(_currWorld);
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
		
		for (Worker worker : workers) {
			worker.join();
		}

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


	private static LinkedList<WorldSection> splitToSections(int hSplit, int vSplit, int boardWidth, int boardHeight) {
		int width = (int)Math.floor((double) boardWidth / hSplit);
		int height = (int)Math.floor((double) boardHeight / vSplit);

		Integer dbgBoard[][] = new Integer[boardWidth][boardHeight];

		LinkedList<WorldSection> sections = new LinkedList<>();
		dbg("width: " + width + ", height: " + height + ", vSplit: " + vSplit + ", hSplit: " + hSplit);
		for (int i = 0; i < hSplit; i++) {
			int effectiveWidth = i + 1 < hSplit ? width : boardWidth - i*width;

			for (int j = 0; j < vSplit; j++) {
				WorldSection section = new WorldSection();

				int effectiveHeight = j + 1 < vSplit ? height : boardHeight - j*height;
				for (int l = 0; l < effectiveWidth; l++) {
					for (int k = 0; k < effectiveHeight; k++) {
						Cell cell = new Cell();
						cell.x = j * height + k;
						cell.y = i * width + l;
						section.cells.add(cell);

//					dbgBoard[cell.x][cell.y] = sections.size();
					}
				}
				if (!section.cells.isEmpty())
					sections.add(section);
			}
		}
//
//		for (WorldSection section : sections) {
//			System.out.print("[");
//			for (Cell cell : section.cells) {
//				System.out.print("(" + cell.x + "," + cell.y + ") ");
//			}
//			System.out.println("] " + section.cells.size() + " cells");
//		}


//		for (int x=0; x < boardHeight; x++) {
//			for (int y=0; y < boardWidth; y++) {
//
//				System.out.print(dbgBoard[y][x] + ", ");
//			}
//			System.out.println();
//		}

		return sections;
	}

	private class Worker implements Runnable {

		private Thread _t;
		
		Worker() {
			_t = new Thread(this);
			_t.start();
		}

		private void join() {
			try {
				_t.join();
			} catch (InterruptedException e) {
			}
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
						} catch (Exception e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
					_workerSem.release(1);
					_genSem.acquire(1);
				}
				_workerSem.release(1);
		}

		private void processSection(WorldSection section) {
			dbg("Processing " + section.cells);
			for (Cell cell : section.cells) {
				dbg(cell.toString());
				int numNeighbors = numNeighbors(cell.x, cell.y, _currWorld);
				if (_currWorld[cell.x][cell.y]) { // alive
					if (numNeighbors == 3 || numNeighbors == 2) {
						_nextWorld[cell.x][cell.y] = true;
					} else {
						_nextWorld[cell.x][cell.y] = false;
					}
				} else { // dead
					if (numNeighbors == 3) {
						_nextWorld[cell.x][cell.y] = true;
					} else {
						_nextWorld[cell.x][cell.y] = false;
					}
				}
			}
		}
	}

	private static class WorldSection {
		private List<Cell> cells = new LinkedList<Cell>();
	}

	@SuppressWarnings("serial")
	private static class Cell extends Point {
		public String toString() {
			return "(" + x + "," + y + ")";
		}
	}

	private static int numNeighbors(int x, int y, boolean[][] field) {
		int counter = (field[x][y] ? -1 : 0);

		for (int i = (x - 1 + field.length); i < (x + 2 + field.length); ++i) {
			for (int j = (y - 1 + field[0].length); j < (y + 2 + field[0].length); j++) {
				counter += (field[i % field.length][j % field[0].length] ? 1 : 0);
			}
		}
		return counter;
	}

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
