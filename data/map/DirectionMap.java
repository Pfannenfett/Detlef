package data.map;

public class DirectionMap {
	private byte[][] content;
	
	public DirectionMap(int xSize, int ySize) {
		content = new byte[xSize][ySize];
	}
	
	public void setValueAt(int x, int y, int i, int j) {
		byte t = 0;
		if(i == 0) {
			if(j == -1) {
				t = 1;
			} else if(j == 1) {
				t = 3;
			} else if(j == 0) {
				t = 0;
			}
		} else if(i == 1){
			if(j == -1) {
				t = 5;
			} else if(j == 1) {
				t = 6;
			} else if(j == 0) {
				t = 2;
			}
		} else if(i == -1){
			if(j == -1) {
				t = 8;
			} else if(j == 1) {
				t = 7;
			} else if(j == 0) {
				t = 4;
			}
		}
		content[x][y] = t;
	}
	
	public byte[] getValueAt(int x, int y) {
		switch(content[x][y]) {
		case 0: byte[] b0 ={0, 0}; return b0;
		case 1: byte[] b1 ={0, -1}; return b1;
		case 2: byte[] b2 ={0, 1}; return b2;
		case 3: byte[] b3 ={-1, 0}; return b3;
		case 4: byte[] b4 ={1, 0}; return b4;
		case 5: byte[] b5 ={1, -1}; return b5;
		case 6: byte[] b6 ={1, 1}; return b6;
		case 7: byte[] b7 ={-1, 1}; return b7;
		case 8: byte[] b8 ={-1, -1}; return b8;
		}
		return new byte[2];
	}

}
