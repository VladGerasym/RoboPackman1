
package krobot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Color;
import javax.sound.sampled.Clip;



final class pmBoom {
	public pmSprite sboom = null;
	public int x, y;
}


final class pmUser {
	public  final static int USER_DEATH  = 1;
	public  final static int USER_WIN    = 2;
	private final static int MOVE_TOP    = 0;
	private final static int MOVE_BOTTOM = 1;
	private final static int MOVE_LEFT   = 2;
	private final static int MOVE_RIGHT  = 3;

	private int   pos_x, pos_y, sel, velocity;
	private final BufferedImage[]   iusers = new BufferedImage[4];
	private final ObjList<pmBoom>   sbooms = new ObjList<pmBoom>(4);
	private BufferedImage    iboom  = null;
	private BufferedImage    icrush = null;
	private final pmSprite[] susers = new pmSprite[4];
	private final pmSprite   scrush = new pmSprite();
	private int         count_bonus = 0;
	private Clip           snd_boom = null;
	private Clip          snd_crash = null;
	private Clip          snd_move  = null;

	private int  crush_x, crush_y;
	private int  life;

	public pmUser(){
		try {
			create();
		} catch(Exception e){}
	}

	public void create() throws Exception {
		iusers[MOVE_TOP]    = pmUtil.loadClip(getClass().getResource("image/user_top.bmp"), 0, true);
		iusers[MOVE_BOTTOM] = pmUtil.loadClip(getClass().getResource("image/user_bottom.bmp"), 0, true);
		iusers[MOVE_LEFT]   = pmUtil.loadClip(getClass().getResource("image/user_left.bmp"), 0, true);
		iusers[MOVE_RIGHT]  = pmUtil.loadClip(getClass().getResource("image/user_right.bmp"), 0, true);

  		susers[0] = new pmSprite(iusers[MOVE_TOP].getWidth(), iusers[MOVE_TOP].getHeight(), 4, 1);
		susers[1] = susers[0];
		susers[2] = new pmSprite(iusers[MOVE_LEFT].getWidth(), iusers[MOVE_LEFT].getHeight(), 1, 4);
		susers[3] = susers[2];

		iboom  = pmUtil.loadClip(getClass().getResource("image/boom.gif"), 0, true);
		icrush = pmUtil.loadClip(getClass().getResource("image/crush.gif"), 0, true);
		for(int i = 0; i < sbooms.getMaxSize(); ++i){
			pmBoom bm = new pmBoom();
			bm.sboom  = new pmSprite(iboom.getWidth(), iboom.getHeight(), 1, 7);
			sbooms.add(bm);
		}

		scrush.initialize(icrush.getWidth(), icrush.getHeight(), 2, 5);

		for(pmSprite sp : susers){
			sp.play(pmUtil.SPRITE_PLAY_NP_LOOP);
		}
		velocity = 2;

		snd_boom  = pmUtil.loadSound(getClass().getResource("sound/boom.wav"));
		snd_crash = pmUtil.loadSound(getClass().getResource("sound/crash.wav"));
		snd_move  = pmUtil.loadSound(getClass().getResource("sound/robot.wav"));
	}

	public void setLocation(byte[][] maze){
		reset();
		count_bonus = 0;
		for(int i = 0; i < maze.length; ++i){
			for(int j = 0; j < maze[i].length; ++j){
				if(maze[i][j] == pmMaze.OBJ_USER){
					pos_x = j * pmUtil.CELL_SIZE;
					pos_y = i * pmUtil.CELL_SIZE;
					maze[i][j] = pmMaze.OBJ_NONE;
				} else if(maze[i][j] == pmMaze.OBJ_BONUS)
					++count_bonus;
			}
		}
		sel  = MOVE_TOP;
		life = 5;
		snd_move.loop(Clip.LOOP_CONTINUOUSLY);
	}

	public void draw(Graphics dc){
		susers[sel].draw(dc, iusers[sel], pos_x, pos_y);

		pmBoom p;
		sbooms.start();
		while(sbooms.isMove()){
			p = sbooms.current();
			p.sboom.draw(dc, iboom, p.x, p.y);
			sbooms.next();
		}

		//вывод урона
		if(scrush.isPlay()){
			scrush.draw(dc, icrush, crush_x, crush_y);
			int top = pos_y + pmUtil.CELL_SIZE + 8;
			dc.setColor(Color.RED);
			dc.drawLine(pos_x, top, pos_x + 25, top);

			dc.setColor(Color.GREEN);
			dc.drawLine(pos_x, top, pos_x + life*5, top);
		}
	}

	public int update_frame(byte[][] maze, long msec){
		susers[sel].update_animation(iusers[sel].getWidth(), iusers[sel].getHeight(), msec, 34L);

		sbooms.start();
		while(sbooms.isMove()){
			if(sbooms.current().sboom.update_animation(iboom.getWidth(), iboom.getHeight(), msec, 60L)){
				sbooms.deactivate();
				continue;
			}
			sbooms.next();
		}


		if(scrush.isPlay()){
			if(scrush.update_animation(icrush.getWidth(), icrush.getHeight(), msec, 50L)){
				if(--life < 1){
					snd_move.stop();
					return USER_DEATH;
				}
			}
		}

		switch(sel){
		case MOVE_TOP:
			pos_y -= velocity;
			if(pos_y < 0)
				pos_y = 0;

			if(! block_intersect(maze, pos_x + pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_EDGE))
				block_intersect(maze, pos_x + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_EDGE);
			break;
		case MOVE_BOTTOM:
			pos_y += velocity;
			if((pos_y + pmUtil.CELL_SIZE) >= pmUtil.FIELD_SIZE)
				pos_y = pmUtil.FIELD_SIZE - pmUtil.CELL_SIZE;

			if(! block_intersect(maze, pos_x + pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE))
				block_intersect(maze, pos_x + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE);
			break;
		case MOVE_LEFT:
			pos_x -= velocity;
			if(pos_x < 0)
				pos_x = 0;

			if(! block_intersect(maze, pos_x + pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_EDGE))
				block_intersect(maze, pos_x + pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE);
			break;
		case MOVE_RIGHT:
			pos_x += velocity;
			if((pos_x + pmUtil.CELL_SIZE) >= pmUtil.FIELD_SIZE)
				pos_x = pmUtil.FIELD_SIZE - pmUtil.CELL_SIZE;

			if(! block_intersect(maze, pos_x + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_EDGE))
				block_intersect(maze, pos_x + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE, pos_y + pmUtil.CELL_SIZE - pmUtil.CELL_EDGE);
			break;
		}

		if(count_bonus <= 0){
                	snd_move.stop();
			return USER_WIN;
		}
		return 0;
	}

	public void keyDown(int key){
		switch(key){
		case KeyEvent.VK_UP:
		case KeyEvent.VK_W:
			sel = MOVE_TOP;
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_S:
			sel = MOVE_BOTTOM;
			break;
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_A:
			sel = MOVE_LEFT;
			break;
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_D:
			sel = MOVE_RIGHT;
			break;
		}
	}

	private boolean block_intersect(byte[][] maze, int x, int y){
		int col = x / pmUtil.CELL_SIZE;
		int row = y / pmUtil.CELL_SIZE;
		col = Math.min(pmUtil.CELL_COUNT - 1, col);
		row = Math.min(pmUtil.CELL_COUNT - 1, row);

		byte obj = maze[row][col];
		if(obj == pmMaze.OBJ_BONUS){
			x = col * pmUtil.CELL_SIZE + pmUtil.CELL_MID;
			y = row * pmUtil.CELL_SIZE + pmUtil.CELL_MID;
			if(pmUtil.isBoxToCircle(pos_x, pos_y, pmUtil.CELL_SIZE, x, y, pmMaze.BONUS_RADIUS)){
				maze[row][col] = pmMaze.OBJ_NONE;

				pmBoom p = sbooms.activate();
				if(p != null){
					p.sboom.play(pmUtil.SPRITE_PLAY);
					p.x = x - p.sboom.getWidth()  / 2;
                                        p.y = y - p.sboom.getHeight() / 2;

					if(! snd_boom.isRunning()){
						snd_boom.setFramePosition(0);
						snd_boom.start();
					}
				}
				--count_bonus;
			}
			return false;
		} else if(obj == pmMaze.OBJ_NONE)
			return false;

		switch(sel){
		case MOVE_TOP:
			pos_y += velocity;
			break;
		case MOVE_BOTTOM:
			pos_y -= velocity;
			break;
		case MOVE_LEFT:
			pos_x += velocity;
			break;
		case MOVE_RIGHT:
			pos_x -= velocity;
			break;
		}
		return true;
	}

	public void beginUron(){
		if(scrush.isStop()){
			scrush.play(pmUtil.SPRITE_PLAY);
			crush_x = (pos_x + pmUtil.CELL_MID) - scrush.getWidth()/2;
			crush_y = (pos_y + pmUtil.CELL_MID) - scrush.getHeight()/2;
			if(! snd_crash.isRunning()){
				snd_crash.setFramePosition(0);
				snd_crash.start();
			}
		}
	}

	public void reset(){
		snd_crash.stop();
		snd_boom.stop();
		scrush.stop();
		sbooms.deactivateAll();
	}

	public void setVelocity(boolean low){
		velocity = (low) ? 2 : 3;
	}

	public int getX(){
		return pos_x;
	}

	public int getY(){
		return pos_y;
	}
}
