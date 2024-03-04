package generator.erosion;

import data.World;
import data.map.HeightMap;

public class Droplet {
	private float sca;
	private float wa;
	
	private int x;
	private int y;
	
	private boolean isAlive = true;
	
	private float evaporation = 0.0001F;
	
	private HeightMap world;
	private byte prevDirection;
	private byte counter = 0;
	private float speed = 0;
	
	private float intensity;
	
	private float[][] change;
	private float[][] flow;
	
	boolean readOnly;
	
	private boolean flag = false;
	
	public Droplet(int posX, int posY, HeightMap w, float waterAmount, float[][] c, boolean readOnly) {
		x = posX;
		y = posY;
		world = w;
		speed = w.getValueAt(posX, posY);
		sca = 0;
		wa = waterAmount;
		change = c;
		this.readOnly = readOnly;
	}
	
	public boolean isAlive() {
		return isAlive;
	}
	
	public void setFlowMap(float[][] s) {
		flow = s;
	}
	
	public void doLivingTick() {
		if(!isOnValidTerrain() || wa <= 0) {
			isAlive = false;
		}
		if(isAlive) {
			float c = world.getValueAt(x, y);
			
			if((c == 0 && flag && Math.random() < 0.1) || c < 0.03125) {
				//System.out.println("Bug??");
				isAlive = false;
				return;
			}
			
			if(c == 0) {
				x = (int) (Math.random() * world.getXSize());
				y = (int) (Math.random() * world.getYSize());
				flag = true;
				return;
			}
			
			float u = world.getValueAt(x, y - 1);
			float r = world.getValueAt(x + 1, y);
			float d = world.getValueAt(x, y + 1);
			float l = world.getValueAt(x - 1, y);
			
			if(u < 0) u = 1F;
			if(r < 0) r = 1F;
			if(d < 0) d = 1F;
			if(l < 0) l = 1F;
			
			wa -= evaporation;
			
			float temp = Math.min(Math.min(u, r), Math.min(d, l));
			int tx = x;
			int ty = y;
			int e = 0;
			
			if(temp == u) {
				e++;
			} 
			if(temp == r) {
				e++;
			} 
			if(temp == d) {
				e++;
			} 
			if(temp == l) {
				e++;
			}
			
			if(counter > 32) {
				prevDirection = (byte) (Math.random() * 4);
			}
			
			if(temp < c) {
				if(e > 1) {
					switch (prevDirection) {
					case 0:ty--; break;
					case 1:tx++; break;
					case 2:ty++; break;
					case 3:tx--; break;

					default:
						break;
					}
					counter++;
				} else if(e == 4 && u == c) {
					if(Math.random() < 0.1) wa = 0;
					speed *= 0.7;
				} else {
					if(temp == u) {
						e++;
						ty--;
						prevDirection = 0;
					} else if(temp == r) {
						e++;
						tx++;
						prevDirection = 1;
					} else if(temp == d) {
						e++;
						ty++;
						prevDirection = 2;
					} else if(temp == l) {
						e++;
						tx--;
						prevDirection = 3;
					}
					counter = 0;
				}
			} else {
				speed = 0;
			}
			
			if(world.getValueAt(tx, ty) == -1) {
				tx = x;
				ty = y;
			}
			
			float grad = world.getValueAt(tx, ty);
			grad -= c;
			grad = -grad;
			grad /= 0.1F;
			speed = 0.5F * (speed + grad);
			if(speed > 1) speed = 0.5F;
			
			if(sca > wa) {
				float delta = sca - wa;
				//if(!readOnly) world.setHeightAt(x, y, (short) (c + delta));
				change[x][y] += delta;
				sca -= delta;
			} else if(sca < wa) {
				float delta = c - world.getValueAt(tx, ty);
				float de = delta * speed;
				delta = de;
				delta = Math.abs(delta);
				float height = c;
				height -= delta;
				height = Math.max(0 , height);
				height = Math.min(1, height);
				if(!readOnly) world.setValueAt(x, y, height);
				change[x][y] -= delta;
				sca += delta;
			}
			
			if(Math.random() < speed) {
				x = tx;
				y = ty;
				if(flow != null) {
					flow[x][y] += 0.0001F;
				}
			}
		}
	}
	
	public boolean isOnValidTerrain() {
		int maxX = world.getXSize();
		int maxY = world.getYSize();
		return x > 0 && y > 0 && x < maxX && y < maxY;
	}

}
