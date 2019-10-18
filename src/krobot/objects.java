
package krobot;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;


final class pmLevel {
	private final byte[][] maze = new byte[pmUtil.CELL_COUNT][pmUtil.CELL_COUNT];

	public pmLevel(){}

	public pmLevel(byte[][] src){
		setData(src);
	}

	public void setData(byte[][] src){
		for(int i = 0; i < pmUtil.CELL_COUNT; ++i){
			for(int j = 0; j < pmUtil.CELL_COUNT; ++j)
				maze[i][j] = src[i][j];
		}
        }

	public void getData(byte[][] dst){
		for(int i = 0; i < pmUtil.CELL_COUNT; ++i){
			for(int j = 0; j < pmUtil.CELL_COUNT; ++j)
				dst[i][j] = maze[i][j];
		}
	}
}


final class pmLevels {
	private Hashtable<Integer, pmLevel> levels = new Hashtable<Integer,pmLevel>();

	public void load(byte[][] maze) throws IOException {
		int  row = 0, col = 0, inc = 0;
		BufferedReader fp = new BufferedReader(new InputStreamReader(getClass().getResource("data/levels.txt").openStream()));
		while(fp.ready()){
			String s = fp.readLine();
			if(s.length() <= 2){
				row = col = 0;
				continue;
			}

			col = 0;
			for(int i = 0; (i < s.length()) && (col < pmUtil.CELL_COUNT); ++i, ++col)
				maze[row][col] = (byte)(s.charAt(i) - '0');

			if(++row >= pmUtil.CELL_COUNT){
				row = 0;
				levels.put(inc, new pmLevel(maze));
				++inc;
			}
		}
		fp.close();
		fp = null;
	}

	public boolean levelAt(byte[][] maze, int level){
		boolean ret = levels.containsKey(level);
		if(ret)
			levels.get(level).getData(maze);
		return ret;
	}

	public int getCountLevels(){
		return levels.size();
	}
}



final class pmMaze {
	public final static byte OBJ_NONE    = 0;
	public final static byte OBJ_BLOCK_A = 1;
	public final static byte OBJ_BLOCK_B = 4;
	public final static byte OBJ_USER    = 5;
	public final static byte OBJ_BONUS   = 6;
	public final static byte OBJ_ALIEN   = 7;
	public final static byte OBJ_ALIEN_X = 8;

	public final static int BONUS_SIZE   = 14;
	public final static int BONUS_OFFSET = (pmUtil.CELL_SIZE - BONUS_SIZE)/2;
	public final static int BONUS_RADIUS = 7;

	private final byte[][]  maze  = new byte[pmUtil.CELL_COUNT][pmUtil.CELL_COUNT];
	private BufferedImage  tiles  = null;
	private BufferedImage  bonus  = null;
	private pmSprite       sbonus = null;
	private final pmLevels levels = new pmLevels();

	public pmMaze(){
		try {
			create();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private void create() throws Exception {
		tiles = javax.imageio.ImageIO.read(getClass().getResource("image/images.png"));
		bonus = pmUtil.loadClip(getClass().getResource("image/bonus.bmp"), 0, true);
		levels.load(maze);

		sbonus = new pmSprite(bonus.getWidth(), bonus.getHeight(), 1, 2);
		sbonus.play(pmUtil.SPRITE_PLAY_LOOP);

		for(int i = 0; i < maze.length; ++i){
			for(int j = 0; j < maze[i].length; ++j)
				maze[i][j] = OBJ_NONE;
		}
	}

	public boolean setLevel(int level){
		return levels.levelAt(maze, level);
	}


	public void draw(Graphics dc){
		for(int i = 0; i < maze.length; ++i){
			for(int j = 0; j < maze[i].length; ++j){
				if(maze[i][j] == OBJ_BONUS)
					sbonus.draw(dc, bonus, j * pmUtil.CELL_SIZE + BONUS_OFFSET, i * pmUtil.CELL_SIZE + BONUS_OFFSET);
			}
		}
	}


	public void update_frame(long msec){
		sbonus.update_animation(bonus.getWidth(), bonus.getHeight(), msec, 190L);
	}


	public void put_objects(BufferedImage img){
		Graphics dc = img.getGraphics();
		int x, y, ox;
		for(int i = 0; i < pmUtil.CELL_COUNT; ++i){
			for(int j = 0; j < pmUtil.CELL_COUNT; ++j){
				if(maze[i][j] >= OBJ_BLOCK_A && maze[i][j] <= OBJ_BLOCK_B){
					x = j * pmUtil.CELL_SIZE;
					y = i * pmUtil.CELL_SIZE;
					ox = (int) (maze[i][j] - 1) * pmUtil.CELL_SIZE;
					dc.drawImage(tiles, x, y, x + pmUtil.CELL_SIZE, y + pmUtil.CELL_SIZE, ox, 0, ox + pmUtil.CELL_SIZE, pmUtil.CELL_SIZE, null);
				}
			}
		}
		dc.dispose();
		dc = null;
	}

	public byte[][] getMaze(){
		return maze;
	}

      	public int getCountLevels(){
		return levels.getCountLevels();
	}
}



final class ThdWork implements Runnable {
	private final Object   obj = new Object();
	private volatile int  loop = 0;
	private final pmPath pfind = new pmPath(pmUtil.CELL_COUNT);
	private byte[][]  ref_maze = null;
	private final ConcurrentLinkedQueue<pmAlien> squs = new ConcurrentLinkedQueue<pmAlien>();

	public ThdWork(byte[][] maze){
		ref_maze = maze;
	}

        public void run(){
        	while(loop == 0){
			try {
				synchronized(obj){
					obj.wait();
				}
			} catch(InterruptedException e){}

			while(!squs.isEmpty()){
				pmAlien p = squs.peek();

				if(p.type == 0)
					pfind.shortest(ref_maze, pmMaze.OBJ_NONE, pmMaze.OBJ_BONUS, p.x, p.y, p.user_x, p.user_y, p.path);
				else
					pfind.find(ref_maze, pmMaze.OBJ_NONE, pmMaze.OBJ_BONUS, p.x, p.y, p.user_x, p.user_y, p.path);

				p.state.set(pmUtil.ALIEN_MOVE);
				squs.remove();
			}
		}
        }

	public void close(){
		squs.clear();
		loop = 1;
		synchronized(obj){
			obj.notify();
		}
	}

	public void putTask(pmAlien alien){
        	squs.add(alien);
		synchronized(obj){
			obj.notify();
		}
	}

	public void reset(){
		squs.clear();
	}
}



final class pmAlien {
	public final PointArray path = new PointArray(64);
	public int   x, y, cur;
	public int   user_x, user_y;
	public float scalar;
	public short type;
	public final AtomicInteger state = new AtomicInteger(pmUtil.ALIEN_STOP);
}


final class pmAliens {
	private pmSprite[]   sprites = new pmSprite[2];
	private BufferedImage  alien = null;
	private float[]     velocity = { 0.05f, 0.055f };
	private final ObjList<pmAlien> aliens = new ObjList<pmAlien>(7);

	public pmAliens(){
		try {
			create();
		} catch(Exception e){}
	}

	private void create() throws Exception {
		alien = pmUtil.loadClip(getClass().getResource("image/alien.bmp"), 0, true);
		sprites[0] = new pmSprite(alien.getWidth(), pmUtil.CELL_SIZE, 1, 4);
		sprites[0].play(pmUtil.SPRITE_PLAY_NP_LOOP);

		sprites[1] = new pmSprite(alien.getWidth(), pmUtil.CELL_SIZE, 1, 4);
		sprites[1].setOffset(0, pmUtil.CELL_SIZE);
		sprites[1].play(pmUtil.SPRITE_PLAY_NP_LOOP);

		for(int i = 0; i < aliens.getMaxSize(); ++i)
			aliens.add(new pmAlien());
	}


	public void put_objects(byte[][] maze){
		reset();
		pmAlien p;
		for(int i = 0; i < maze.length; ++i){
			for(int j = 0; j < maze[i].length; ++j){
				if(maze[i][j] == pmMaze.OBJ_ALIEN || maze[i][j] == pmMaze.OBJ_ALIEN_X){
					p = aliens.activate();
					if(p != null){
						p.scalar = 0.0f;
						p.cur    = 1;
						p.type   = (short)(maze[i][j] - pmMaze.OBJ_ALIEN);
						p.state.set(pmUtil.ALIEN_STOP);
						p.x = j * pmUtil.CELL_SIZE;
						p.y = i * pmUtil.CELL_SIZE;
					}
					maze[i][j] = pmMaze.OBJ_NONE;
				}
			}
		}
	}


	public void draw(Graphics dc){
		pmAlien p;
		aliens.start();
		while(aliens.isMove()){
			p = aliens.current();
			sprites[p.type].draw(dc, alien, p.x, p.y);
			aliens.next();
		}
	}


	public void update_frame(byte[][] maze, ThdWork thd, pmUser user, long msec){
		if(aliens.getSize() > 0){
			sprites[0].update_animation(alien.getWidth(), pmUtil.CELL_SIZE, msec, 60L);
			sprites[1].update_animation(alien.getWidth(), pmUtil.CELL_SIZE, msec, 70L);
		}


		pmAlien p;
		aliens.start();
		while(aliens.isMove()){
			p = aliens.current();
			switch(p.state.get()){
			case pmUtil.ALIEN_MOVE:

				if(p.cur >= p.path.getSize()){
					p.state.set(pmUtil.ALIEN_STOP);
					p.x = p.path.getX(p.cur - 1);
					p.y = p.path.getY(p.cur - 1);
					break;
				}
				p.x = pmUtil.lerp(p.path.getX(p.cur - 1), p.path.getX(p.cur), p.scalar);
				p.y = pmUtil.lerp(p.path.getY(p.cur - 1), p.path.getY(p.cur), p.scalar);

				p.scalar += velocity[p.type];
				if(p.scalar >= 1.0f){
					p.x      = p.path.getX(p.cur);
					p.y      = p.path.getY(p.cur);
					p.scalar = 0.0f;
					p.cur   += 1;
				}
				break;
			case pmUtil.ALIEN_STOP:
				p.state.set(pmUtil.ALIEN_FIND);
				p.scalar = 0.0f;
				p.user_x = user.getX() + pmUtil.CELL_MID;
				p.user_y = user.getY() + pmUtil.CELL_MID;
				p.cur    = 1;
				thd.putTask(p);
				break;
			}


			if(pmUtil.isBoxToCircle(user.getX() + pmUtil.CELL_EDGE, user.getY() + pmUtil.CELL_EDGE, pmUtil.CELL_SIZE - pmUtil.CELL_EDGE, p.x + pmUtil.CELL_MID, p.y + pmUtil.CELL_MID, pmUtil.CELL_MID - 2)){
				user.beginUron();
				if(p.state.get() == pmUtil.ALIEN_MOVE){
					if((p.scalar - velocity[p.type]*2.0f) >= 0.0f)
						p.scalar -= velocity[p.type] * 2.0f;
				}
			}
			aliens.next();
		}
	}

	public void reset(){
		aliens.deactivateAll();
	}

	public void setVelocity(boolean low){
		if(low){
			velocity[0] = 0.05f;
			velocity[1] = 0.055f;
		} else {
			velocity[0] = 0.06f;
			velocity[1] = 0.065f;
		}
	}
}

