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
	
	@Override
	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit, int generations) {
		_generations = generations;
		_nextWorld = new boolean[initalField.length][initalField[0].length];
		_currWorld = new boolean[initalField.length][initalField[0].length];
		for (int i=0; i<initalField.length; i++) {
			_currWorld[i] = new boolean[initalField[i].length];
			_nextWorld[i] = new boolean[initalField[i].length];
			System.arraycopy(initalField[i], 0, _currWorld[i], 0, initalField[i].length);
		}
		
		int width  = initalField[0].length / hSplit;
		int height = initalField.length / vSplit;
		
		// split the world map to WorldSections
		for (int i=0; i<hSplit; i++) {
			for (int j=0; j<vSplit; j++) {
				_worldSections.add(new WorldSection());
			}
		}
		
		// launch all threads
		for (int i=0; i<_worldSections.size(); i++) {
			new Worker();
		}
		
		while (_generations > 0) {
			// wait for workers to finish
			_workerSem.acquire(_worldSections.size());
			
			// switch _currWorld and _nextWorld
			boolean[][] tmp = _currWorld;
			_currWorld = _nextWorld;
			_nextWorld = tmp;
			
			// advance gen and signal workers to advance to next gen
			_generations--;
			_genSem.release(_worldSections.size());
		}
		
		_workerSem.acquire(_worldSections.size());
		
		
		return new boolean[][][]{_nextWorld,_currWorld};
	}
	
	private class Worker implements Runnable {

		private WorldSection _section;
		
		Worker() {
			new Thread(this).start();
		}
		
		@Override
		public void run() {
			while (_generations > 0) {
				WorldSection section = null;
				while (true) {
					synchronized (DirtyGameOfLife.this) {
						if (_worldSections.size() > 0) {
							section = _worldSections.removeFirst();
						} else {
							break;
						}
						processSection(section);
					}
				}
				_workerSem.release(1);
				_genSem.acquire(1);
			}
			
			_workerSem.release(1);
		}
		
		private void processSection(WorldSection section) {
			
		}
	}

	private static class WorldSection {
		private List<Cell> outer = new LinkedList<Cell>();
		private List<Cell> inner = new LinkedList<Cell>();
	}
	
	private static class Cell extends Point {
	}
	
	private static int numNeighbors(int x,int y, boolean[][] field ){
		int counter=(field[x][y]?-1:0);
		for (int i=(x-1 + field.length); i<(x+2 + field.length); ++i){
			for (int j=(y-1 + field[0].length); j<(y+2 + field[0].length); j++) {
				counter+=(field[i%field.length][j%field[0].length]?1:0);
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
			synchronized (this) {
				while (_permits == 0) {
					vait(this);
				}
				_permits -= permits;
			}
		}
		
		void release(int permits) {
			synchronized (this) {
				_permits += permits;
				notifyAll();
			}
		}
	}
	
	private static void vait(Object o) {
		try {
			o.wait();
		} catch (InterruptedException e) {
		}
	}

}
