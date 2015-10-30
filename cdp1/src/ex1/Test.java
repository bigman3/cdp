package ex1;


public class Test {

	public static void main(String[] args) {
		for (int row = 1; row < 100; row++) {
			System.out.println(row);
			for (int col = 1; col < 100; col++) {
				boolean[][] field = new boolean[row][col];
				for (int i = 1; i < row; i++) {
					field[i] = new boolean[col];
					for (int j = 1; j < col; j++) {
						field[i][j] = (Math.random() > 0.25);
					}
				}
				int nGenerations = (int) (Math.random() * 10);
				for (int hSplit = 1; hSplit < 10; hSplit++) {
					for (int vSplit = 1; vSplit < 10; vSplit++) {
						GameOfLife sGol = new SerialGameOfLife();
						GameOfLife pGol = new ParallelGameOfLife();
						long start = System.currentTimeMillis();
						boolean[][][] resultSerial = sGol.invoke(field, hSplit, vSplit, nGenerations);
						long end = System.currentTimeMillis();
						long serialTime = end - start;
						boolean[][][] resultParallel;

/*						System.out.println("Testing hSplit: " + hSplit + ", vSplit: " + vSplit + ", rows: " + row
								+ ", cols: " + col);*/

						start = System.currentTimeMillis();

						try {
							resultParallel = pGol.invoke(field, hSplit, vSplit, nGenerations);
						} catch (Exception e) {
							System.err.println("Failed, hSplit: " + hSplit + ", vSplit: " + vSplit + ", rows: " + row
									+ ", cols: " + col);
							e.printStackTrace();
							return;
						}

						end = System.currentTimeMillis();
						long parallelTime = end - start;

						boolean success = (compareArrays(resultParallel[0], resultSerial[0])
								&& (compareArrays(resultParallel[1], resultSerial[1])));
						if (!success) {
							System.out.println("serial: ");
							for (int k=0; k<resultSerial[0].length; k++) {
								for (int l=0; l<resultSerial[0][0].length; l++) {
									System.out.print(resultSerial[0][k][l] ? "1 " : "0 ");
								}
								System.out.println();
							}
							System.out.println("parallel:");
							for (int k=0; k<resultParallel[0].length; k++) {
								for (int l=0; l<resultParallel[0][0].length; l++) {
									System.out.print(resultParallel[0][k][l] ? "1 " : "0 ");
								}
								System.out.println();
							}
							
/*							System.err.println("**SUCESSS! Failed, hSplit: " + hSplit + ", vSplit: " + vSplit + ", rows: " + row
									+ ", cols: " + col);*/
							return;
						} else {
							if (parallelTime != 0 && serialTime != 0) {
/*								System.out.println("**SUCESSS! speedup: " + (serialTime / parallelTime) + " hSplit: " + hSplit
										+ ", vSplit: " + vSplit + ", rows: " + row + ", cols: " + col);*/
							}
						}
					}
				}
			}
		}
	}
	
	public static boolean compareArrays(boolean[][] arr1, boolean[][] arr2) {
		if (arr1 == null || arr2 == null) {
			return false;
		}
		if (arr1.length != arr2.length) {
			return false;
		}
		for (int i = 0; i < arr1.length; i++) {
			if (arr1[i].length != arr2[i].length) {
				return false;
			}
			for (int j = 0; j < arr1[i].length; j++) {
				if (arr1[i][j] != arr2[i][j]) {
					return false;
				}
			}
		}
		return true;
	}

}
