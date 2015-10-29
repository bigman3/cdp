package ex1;

public class ParallelGameOfLife implements GameOfLife {

	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit,
			int generations) {
		boolean[][][] x = new boolean[2][][];	
		boolean[][] copy1 = deepCopy(initalField);
		boolean[][] copy2 = deepCopy(initalField);
		WorkerMain last = new WorkerMain(copy1, hSplit, vSplit, generations);
		x[1] = last.getResult();
		WorkerMain preLast = new WorkerMain(copy2, hSplit, vSplit, generations-1);
		x[0] = preLast.getResult();
		return x;
		// TODO Auto-generated method stub
	}
	
	private boolean[][] deepCopy(boolean[][] src){
		boolean[][] dst = new boolean[src.length][src[0].length];
		for (int i = 0; i < dst.length; i++) {
			for (int j = 0; j < dst[0].length; j++) {
				dst[i][j] = src[i][j];
			}
		}
		return dst;
	}

}
