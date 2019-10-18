
package krobot;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Random;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;


final class pmUtil {
	public final static short SPRITE_STOP         = 0x1;
	public final static short SPRITE_PAUSE        = 0x2;
	public final static short SPRITE_PLAY         = 0x4;
	public final static short SPRITE_PLAY_LOOP    = 0x8;
	public final static short SPRITE_PLAY_NP      = 0x10;
	public final static short SPRITE_PLAY_NP_LOOP = 0x20;

	public final static int CELL_SIZE  = 32;
	public final static int CELL_MID   = 16;
	public final static int CELL_EDGE  = 5;
	public final static int CELL_COUNT = 16;
	public final static int FIELD_SIZE = 512;

	public final static int ALIEN_STOP = 0;
	public final static int ALIEN_FIND = 1;
	public final static int ALIEN_MOVE = 2;

	public static BufferedImage loadClip(Object res, int color_key, boolean first_pixel) throws IOException {
		BufferedImage simg, dimg;
		if(res instanceof String)
			simg = javax.imageio.ImageIO.read(new File((String)res));
		else if(res instanceof java.net.URL)
			simg = javax.imageio.ImageIO.read((java.net.URL)res);
		else
			return null;

		if(first_pixel)
			color_key = simg.getRGB(0, 0);

		int rgb;
		if(simg.getColorModel().getPixelSize() == 32){
			for(int y = 0; y < simg.getHeight(); ++y) {
				for(int x = 0; x < simg.getWidth(); ++x) {
					if((rgb = simg.getRGB(x, y)) != color_key)
						simg.setRGB(x, y, 0xFF000000 | rgb);
					else
						simg.setRGB(x, y, 0);
				}
			}
			dimg = simg;
		} else {
			dimg = new BufferedImage(simg.getWidth(), simg.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for(int y = 0; y < simg.getHeight(); ++y) {
				for(int x = 0; x < simg.getWidth(); ++x) {
					if((rgb = simg.getRGB(x, y)) != color_key)
						dimg.setRGB(x, y, 0xFF000000 | rgb);
					else
						dimg.setRGB(x, y, 0);
				}
			}
			simg = null;
		}
		return dimg;
	}

	public static void fillImage(BufferedImage dst, BufferedImage src, int off_x, int off_y, int width, int height){
		int cx = off_x + width;
		int cy = off_y + height;
		for(int i = 0; i < dst.getWidth(); i += width){
			for(int j = 0; j < dst.getHeight(); j += height){
				for(int c = off_x; c < cx; ++c){
					for(int r = off_y; r < cy; ++r)
						dst.setRGB(i + c - off_x, j + r - off_y, src.getRGB(c, r));
				}
			}
		}
	}

	public static BufferedImage loadImage(String filename) throws IOException {
		return javax.imageio.ImageIO.read(pmKRobot.class.getResource(filename));
	}

	public static int lerp(int a, int b, float t){
		return a + (int)((float)(b - a) * t);
	}

	public static boolean isBoxToCircle(int rx, int ry, int size, int cx, int cy, int r){
		int x = cx;
		int y = cy;

		if(x < rx)
			x = rx;
		else if(x > (rx + size))
			x = rx + size;

		if(y < ry)
			y = ry;
		else if(y > (ry + size))
			y = ry + size;

		x = cx - x;
		y = cy - y;
		return ((x*x + y*y) <= (r * r));
	}


	public static Clip loadSound(java.net.URL url){
		Clip            clip = null;
		AudioFileFormat  fmt = null;
		try {
			fmt  = AudioSystem.getAudioFileFormat(url);
			clip = (Clip)AudioSystem.getLine(new DataLine.Info(Clip.class, fmt.getFormat()));
			clip.open(AudioSystem.getAudioInputStream(url));
		} catch(Exception e){
			return null;
		} finally {
			fmt = null;
		}
		return clip;
	}
}


//
final class pmSprite {
	private long  delay;
	private int   px, py;
	private int   cx, cy;
	private int   off_x, off_y;
	private short iplay;
	private short mode;

	public pmSprite(){
		initialize(0, 0, 1, 1);
	}

	public pmSprite(int width, int height, int rows, int cols){
		initialize(width, height, rows, cols);
	}

	public void initialize(int width, int height, int rows, int cols){
		px    = py = 0;
		delay = 0L;
		iplay = pmUtil.SPRITE_STOP;
		cx    = width  / cols;
		cy    = height / rows;
		off_x = off_y = 0;
	}

	public void setOffset(int x, int y){
		off_x = x;
		off_y = y;
	}

	public void draw(Graphics dc, BufferedImage img, int x, int y){
		dc.drawImage(img, x, y, x + cx, y + cy, off_x + px, off_y + py, off_x + px + cx, off_y + py + cy, null);
	}

	public void draw(Graphics dc, BufferedImage img, int x, int y, int width, int height){
		dc.drawImage(img, x, y, x + width, y + height, off_x + px, off_y + py, off_x + px + cx, off_y + py + cy, null);
	}

	public boolean update_animation(int width, int height, long msec, long velocity){
		if(iplay < pmUtil.SPRITE_PLAY)
			return false;
		if((msec - delay) < velocity)
			return false;
		delay = msec;

		switch(iplay){
		case pmUtil.SPRITE_PLAY_LOOP:
		case pmUtil.SPRITE_PLAY:
			px += cx;
			if(px >= width){
				px  = 0;
				py += cy;
				if(py >= height){
					py = 0;
					if(iplay == pmUtil.SPRITE_PLAY){
						iplay = pmUtil.SPRITE_STOP;
						return true;
					}
					px = 0;
				}
			}
			break;
		case pmUtil.SPRITE_PLAY_NP:
		case pmUtil.SPRITE_PLAY_NP_LOOP:
			if(mode == 0){
				px += cx;
				if(px >= width){
					px  = 0;
					py += cy;
					if(py >= height){
						py   = height - cy;
						px   = width  - cx;
						mode = 1;
					}
				}
			} else if(mode == 1){
				px -= cx;
				if(px < 0){
					px  = width - cx;
					py -= cy;
					if(py < 0){
						if(iplay == pmUtil.SPRITE_PLAY_NP){
							px    = py = 0;
							mode  = 0;
							iplay = pmUtil.SPRITE_STOP;
							return true;
						}
						px   = py = 0;
						mode = 0;
					}
				}
			}
			break;
		}
		return false;
	}

	public void play(short flag){
		if((iplay & pmUtil.SPRITE_PAUSE) != 0){
			iplay &= ~pmUtil.SPRITE_PAUSE;
			return;
		}

		iplay = flag;
		switch(iplay){
                case pmUtil.SPRITE_PLAY:
		case pmUtil.SPRITE_PLAY_LOOP:
		case pmUtil.SPRITE_PLAY_NP:
		case pmUtil.SPRITE_PLAY_NP_LOOP:
			px   = py = 0;
			mode = 0;
			break;
		}
	}

	public void stop(){
		iplay = pmUtil.SPRITE_STOP;
	}

	public void pause(){
		iplay |= pmUtil.SPRITE_PAUSE;
	}

	public boolean isPlay(){
		return !isPause() && (iplay >= pmUtil.SPRITE_PLAY);
	}

	public boolean isPause(){
		return ((iplay & pmUtil.SPRITE_PAUSE) != 0);
	}

	public boolean isStop(){
		return (iplay == pmUtil.SPRITE_STOP);
	}

	public int getWidth(){
		return cx;
	}

	public int getHeight(){
		return cy;
	}
}



final class pmPath {
	private short[][]    cmaze = null;
	private short[][]   parent = null;
	private final Random   rnd = new Random();
	private final PointSQ  stk = new PointSQ(128);
	private final int[][] dirs = { {1,0}, {0,1}, {-1,0}, {0,-1} };

	public pmPath(int size){
		cmaze  = new short[size][size];
		parent = new short[size][size];
	}


	public boolean shortest(byte[][] maze, byte low, byte high, int x1, int y1, int x2, int y2, PointArray path){
		int      nx, ny;
		boolean fnd = false;
		short   cnt = 1;

		x1 /= pmUtil.CELL_SIZE;
		y1 /= pmUtil.CELL_SIZE;
		x2 /= pmUtil.CELL_SIZE;
		y2 /= pmUtil.CELL_SIZE;

		for(int i = 0; i < maze.length; ++i){
			for(int j = 0; j < maze[i].length; ++j)
				cmaze[i][j] = (maze[i][j] == low || maze[i][j] == high) ? 0 : Short.MAX_VALUE;
		}
		cmaze[y1][x1] = 0;
		cmaze[y2][x2] = 1;

		for(boolean loop = true; loop; ++cnt){
			loop = false;
			for(int r = 0; r < cmaze.length; ++r){
				for(int c = 0; c < cmaze[r].length; ++c){
					if(cmaze[r][c] != cnt)
						continue;

					for(int i = 0; i < dirs.length; ++i){
						nx = c + dirs[i][0];
						ny = r + dirs[i][1];
						if((nx < 0) || (ny < 0) || (nx >= cmaze.length) || (ny >= cmaze.length))
							continue;

						if(cmaze[ny][nx] == 0){
							cmaze[ny][nx] = (short)(cnt + 1);
							loop = true;
						}
					}

					if((r == y2) && (c == x2))
						fnd = true;
				}
			}
		}

		if(fnd){
			path.reset();
			int i = y1;
			int j = x1;
			path.add(j * pmUtil.CELL_SIZE, i * pmUtil.CELL_SIZE);
			while(cmaze[i][j] > 1){
				for(int d = 0; d < dirs.length; ++d){
					nx = j + dirs[d][0];
					ny = i + dirs[d][1];
					if((nx > -1 && nx < cmaze.length) && (ny > -1 && ny < cmaze.length)){
						if(cmaze[ny][nx] < cmaze[i][j]){
							i = ny;
							j = nx;
							break;
						}
					}
				}
				path.add(j * pmUtil.CELL_SIZE, i * pmUtil.CELL_SIZE);
			}
		}
		return fnd;
	}


	public void find(byte[][] maze, byte low, byte high, int x1, int y1, int x2, int y2, PointArray path){
		int x, y, cur_x = 0, cur_y = 0;
		for(int i = 0; i < cmaze.length; ++i){
			for(int j = 0; j < cmaze[i].length; ++j)
				cmaze[i][j] = Short.MAX_VALUE;
		}
		x1 /= pmUtil.CELL_SIZE;
		y1 /= pmUtil.CELL_SIZE;
		x2 /= pmUtil.CELL_SIZE;
		y2 /= pmUtil.CELL_SIZE;

		cmaze[y1][x1] = 0;

		stk.setTypeSQ((rnd.nextInt(2) == 0) ? PointSQ.QUEUE_FIFO : PointSQ.STACK_LIFO);
		stk.reset();
		stk.push(x1, y1);
		while(! stk.empty()){
			cur_x = stk.topX();
			cur_y = stk.topY();
			stk.pop();

			for(int i = 0; i < dirs.length; ++i) {
				x = cur_x + dirs[i][0];
				y = cur_y + dirs[i][1];
				if((x < 0) || (y < 0) || (x >= cmaze.length) || (y >= cmaze.length))
					continue;
				else if(maze[y][x] != low && maze[y][x] != high)
					continue;

				if(cmaze[y][x] > (cmaze[cur_y][cur_x] + 1)){
					parent[y][x] = (short)((cur_x << 8) | cur_y);
					cmaze[y][x]  = (short)(cmaze[cur_y][cur_x] + 1);
					stk.push(x, y);
				}
			}
		}
		stk.reset();

		cur_x = x2;
		cur_y = y2;
		path.reset();
		path.add(cur_x * pmUtil.CELL_SIZE, cur_y * pmUtil.CELL_SIZE);
		x2 = cur_x;
		y2 = cur_y;
		while((x1 != x2) || (y1 != y2)){// выделяем путь
			if((x1 == x2) && (y1 == y2))
				break;
			x = parent[y2][x2] >> 8;
			y = parent[y2][x2] & 0xFF;
			path.add(x * pmUtil.CELL_SIZE, y * pmUtil.CELL_SIZE);
			x2 = x;
			y2 = y;
		}

		if(path.getSize() > 0)
			path.reverse();
	}
}
