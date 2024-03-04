package io;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;

import data.map.HeightMap;
import data.map.IPrintableMap;

public class ImageIO {
	private static int id = 0;
	
	public static void saveImage(BufferedImage image, String name) {
		File f = new File("C:\\Users\\User\\Desktop\\WorldBuildingImages\\" + name + ".png");
		try {
			javax.imageio.ImageIO.write(image, "png", f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("saved custom image " + name);
	}
	
	public static void saveAsImage(IPrintableMap map) {
		saveAsImage(map, "" + id);
		id++;
	}
	
	public static void saveAsImage(IPrintableMap map, String name) {
		long time = System.currentTimeMillis();
		System.out.print("Attempting to write Image " + name + " with size " + map.getXSize() + "/" + map.getYSize());
		BufferedImage image = new BufferedImage(map.getXSize(), map.getYSize(), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		for(int i = 0; i < map.getXSize(); i++) {
			for(int j = 0; j < map.getYSize(); j++) {
				g.setColor(map.getColorAt(i, j));
				g.drawRect(i, j, 1, 1);
			}
		}
		File f = new File("C:\\Users\\User\\Desktop\\WorldBuildingImages\\" + name + ".png");
		try {
			javax.imageio.ImageIO.write(image, "png", f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("   -completed in " + time + " ms");
	}
	
	public static void saveAsGreyImage(IPrintableMap map, String name) {
		long time = System.currentTimeMillis();
		System.out.print("Attempting to write Image " + name + " with size " + map.getXSize() + "/" + map.getYSize());
		BufferedImage image = new BufferedImage(map.getXSize(), map.getYSize(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = image.getGraphics();
		for(int i = 0; i < map.getXSize(); i++) {
			for(int j = 0; j < map.getYSize(); j++) {
				g.setColor(map.getColorAt(i, j));
				g.drawRect(i, j, 1, 1);
			}
		}
		File f = new File("C:\\Users\\User\\Desktop\\WorldBuildingImages\\" + name + ".png");
		try {
			javax.imageio.ImageIO.write(image, "png", f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		time = System.currentTimeMillis() - time;
		System.out.println("   -completed in " + time + " ms");
	}
	
	
	public static HeightMap loadGreyImage(String name) {
		File f = new File("C:\\Users\\User\\Desktop\\WorldBuildingImages\\" + name + ".jpg");
		BufferedImage image = null;
		try {
			image = javax.imageio.ImageIO.read(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(image == null) return null;
		//BufferedImage test = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
		//Graphics g = test.getGraphics();
		HeightMap map = new HeightMap(image.getWidth(), image.getHeight());
		for(int i = 0; i < map.getXSize(); i++) {
			for(int j = 0; j < map.getYSize(); j++) {
				Color c = new Color(image.getRGB(i, j));
				int t = c.getBlue();
			//	Color b = new Color(t, t, t);
				//g.setColor(b);
				//g.fillRect(i, j, 1, 1);
				double d = t;
				d = d / 256;
				map.setValueAt(i, j, (float) d);
			}
		}
		//f = new File("C:\\Users\\User\\Desktop\\WorldBuildingImages\\" + name + "1.png");
		//try {
			//javax.imageio.ImageIO.write(test, "png", f);
		//} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
		return map;
	}
	
}
