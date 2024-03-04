package generator.erosion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import data.map.HeightMap;

public class ErosionThread implements Runnable {
	private HeightMap world;
	private int boxSize;
	private int xBox;
	private int yBox;
	private Random rng;
	private int lifespan;
	private int radius;
	private float dropDensity;
	boolean mountainsOnly;
	private float maxChange;
	
	Thread t;
	
	public ErosionThread(HeightMap w, int boxSize, int xBox, int yBox, long seed, int radius, int lifespan, float density, float maxChange, boolean mountsOnly) {
		if(xBox == w.getXSize() || yBox == w.getYSize()) {
			System.out.println("Wrong drop placement");
			xBox -= boxSize;
			yBox -= boxSize;
		}
		
		world = w;
		this.boxSize = boxSize;
		this.xBox = xBox;
		this.yBox = yBox; 
		rng = new Random(seed);
		this.lifespan = lifespan;
		this.radius = radius;
		dropDensity = density;
		mountainsOnly = mountsOnly;
		this.maxChange = maxChange;
	}
	
	@Override
	public void run() {
		float dropCount = boxSize * boxSize;
		dropCount *= dropDensity;
		BetterDroplet d = new BetterDroplet(world, radius);
		float[][] changes = new float[lifespan * radius * radius][];
		int x, y;
		int[] c;
		for(int i = 0; i < dropCount; i++) {
			
			c = createCoords();
			x = c[0];
			y = c[1];
			if(mountainsOnly && world.getValueAt(x, y) < 0.8F) continue;
			changes = new float[(lifespan + 1) * (2 * radius + 1) * (2 * radius + 1)][];
			d.reset(lifespan, changes, radius, maxChange);
			d.relocate(x, y);
			for(int t = 0; t < lifespan; t++) {
				
				if(!d.doLivingTick()) break;
				
			}
			d.drop();
			//checkChanges(changes);
			world.perfChanges(changes);
			
		}
		System.out.print("|");
	}
	
	private int[] createCoords() {
		int x = rng.nextInt(boxSize);
		int y = rng.nextInt(boxSize);
		x = Math.max(x, 0);
		y = Math.max(y, 0);
		x = Math.min(x, boxSize);
		y = Math.min(y, boxSize);
		x += xBox;
		y += yBox;
		int[] c = {x, y};
		return c;
	}
	
	public void checkChanges(HashSet<float[]> changes) {
		Iterator it = changes.iterator();
		float sum = 0;
		while(it.hasNext()) {
			float[] c = (float[]) it.next();
			sum += c[2];
		}
		if(sum > 0) {
			System.out.println(sum + " ");
		}
		
	}
	
	public void start() {
		t = new Thread(this);
		t.start();
	}
	
	public void join() throws InterruptedException {
		t.join();
	}
	
	public boolean isAlive() {
		return t.isAlive();
	}
}
