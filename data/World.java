package data;

import data.map.GroundCoverMap;
import data.map.HeightMap;
import data.util.MapUtils;
import io.ImageIO;

public class World {
	private byte[][] upperTerrain;
	private byte[][] lowerTerrain;
	
	private GroundCoverMap groundCover;
	
	public World(int xSize, int ySize) {
		groundCover = new GroundCoverMap(xSize, ySize);
	}
	
	public GroundCoverMap getGroundCover() {
		return groundCover;
	}
	
	public void setGroundCover(GroundCoverMap groundCover) {
		this.groundCover = groundCover;
	}
	
	public HeightMap getUpperTerrain() {
		return MapUtils.toHeightMap(upperTerrain);
	}
	
	public HeightMap getLowerTerrain() {
		return MapUtils.toHeightMap(lowerTerrain);
	}
	
	public void setUpperTerrain(HeightMap h) {
		upperTerrain = MapUtils.toByteMap(h);
	}
	
	public void setLowerTerrain(HeightMap h) {
		this.lowerTerrain = MapUtils.toByteMap(h);
	}
	
	public void saveAll() {
		if(upperTerrain != null) ImageIO.saveAsGreyImage(MapUtils.toHeightMap(upperTerrain), "Terrain up");
		if(lowerTerrain != null) ImageIO.saveAsGreyImage(MapUtils.toHeightMap(lowerTerrain), "Terrain down");
		
		ImageIO.saveAsImage(groundCover, "Ground Cover");
	}

}
