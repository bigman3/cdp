package ex1;

public class ParallelGameOfLife implements GameOfLife {

	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit, int generations) {
		return new DirtyGameOfLife().invoke(initalField, hSplit, vSplit, generations);
	}
}
