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
		_nextWorld = new boolean[initalField.length][initalField[0].length];
		_currWorld = new boolean[initalField.length][initalField[0].length];
		for (int i = 0; i < initalField.length; i++) {
			_currWorld[i] = new boolean[initalField[i].length];
			_nextWorld[i] = new boolean[initalField[i].length];
		}

		for (int i = 0; i < initalField.length; i++) {
			System.arraycopy(initalField[i], 0, _currWorld[i], 0, initalField[i].length);
		}

		print_board(initalField);

		// split the world map to WorldSections
		_worldSections = splitToSections(hSplit, vSplit, initalField);

		// backup _worldSections
		LinkedList<WorldSection> sections = new LinkedList<WorldSection>();
		for (WorldSection section : _worldSections) {
			sections.add(section);
		}

		int workerNum = _worldSections.size();

		// launch all threads
		for (int i = 0; i < workerNum; i++) {
			new Worker();
		}

		while (_generations > 0) {
			// wait for workers to finish
			_workerSem.acquire(workerNum);
			// switch _currWorld and _nextWorld

			print_board(_currWorld);
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

	private static LinkedList<WorldSection> splitToSections(int hSplit, int vSplit, boolean[][] initialField) {
		int width = initialField[0].length / vSplit;
		int height = initialField.length / hSplit;
		Integer dbgBoard[][] = new Integer[vSplit * width][hSplit * height];

		LinkedList<WorldSection> sections = new LinkedList<WorldSection>();
		dbg("width: " + width + ", height: " + height + ", vSplit: " + vSplit + ", hSplit: " + hSplit);
		for (int i = 0; i < hSplit; i++) {
			for (int j = 0; j < vSplit; j++) {
				WorldSection section = new WorldSection();
				for (int l = 0; l < height; l++) {
					for (int k = 0; k < width; k++) {
						Cell cell = new Cell();
						cell.x = i * height + l;
						cell.y = j * width + k;
						section.cells.add(cell);

						dbgBoard[cell.x][cell.y] = sections.size();
					}
				}
				sections.add(section);
			}
		}

		for (WorldSection section : sections) {
			System.out.print("[");
			for (Cell cell : section.cells) {
				System.out.print("(" + cell.x + "," + cell.y + ") ");
			}
			System.out.println("]");
		}

		for (int x = 0; x < vSplit * width; x++) {
			for (int y = 0; y < hSplit * height; y++) {

				System.out.print(dbgBoard[x][y] + ", ");
			}
			System.out.println();
		}

		return sections;
	}

	private class Worker implements Runnable {

		Worker() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			dbg("Started");
			try {
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
						processSection(section);
					}
					_workerSem.release(1);
					_genSem.acquire(1);
				}
				_workerSem.release(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void processSection(WorldSection section) {
			dbg("Processing " + section.cells);
			for (Cell cell : section.cells) {
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
			return "(" + y + "," + x + ")";
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
			//dbg("Entering acquire(" + permits + ")");
			synchronized (this) {
				while (_permits < permits) {
					vait(this);
				}
				_permits -= permits;
			}
			//dbg("Exiting");
		}

		void release(int permits) {
			//dbg("Entering release(" + permits + ")");
			synchronized (this) {
				_permits += permits;
				notifyAll();
			}
			//dbg("Exiting");
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
