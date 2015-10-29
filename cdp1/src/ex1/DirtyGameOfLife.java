package ex1;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

public class DirtyGameOfLife implements GameOfLife {

	private volatile boolean[][] _nextWorld;
	private volatile boolean[][] _currWorld;
	private LinkedList<WorldSection> _worldSections = new LinkedList<WorldSection>();
	
	@Override
	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit, int generations) {
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
			new Worker(generations);
		}
		
		while (generations > 0) {
			// wait for gen to finish
			// switch _currWorld and _nextWorld
			// wake up workers again
		}
		
		
		return null;
	}
	
	private class Worker implements Runnable {

		private int _gens;
		private WorldSection _section;
		
		Worker(int gens) {
			_gens = gens;
			new Thread(this).start();
		}
		
		@Override
		public void run() {
			while (_gens > 0) {
				WorldSection section = null;
				synchronized (DirtyGameOfLife.this) {
					if (_worldSections.size() > 0) {
						section = _worldSections.removeFirst();
						processSection(section);
					}
				}
				_gens--;
				
			}
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

}
