package krobot;
import java.io.*;

import javax.swing.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;



final class pmKRobot extends JFrame {
	private final static int DELAY_GAME  = 30;
	private final static int BORDER_SIZE = 1;
	private final static int GAME_MENU   = 0;
	private final static int GAME_LEVEL  = 1;
	private final static int GAME_PLAY   = 2;
	private final static int GAME_OVER   = 3;
	private final static int GAME_WIN    = 4;

	private final String file_level = "level.txt";
	private javax.swing.Timer timer = null;
	private long             uframe = 0L;
	private BufferedImage      img  = null;
	private Graphics           mdc  = null;
	private pmMaze             maze = null;
	private pmUser             user = null;
	private pmAliens         aliens = null;
	private ThdWork           twork = null;
	private Thread           thread = null;
	private BufferedImage     bgimg = null;
	private BufferedImage     borig = null;
	private boolean            drag = false;
	private int              gstate = GAME_MENU;
	private pmMenu           gmenu  = null;
	private Cursor         drag_cur = null;
	private Cursor          def_cur = null;
	private int pos_x, pos_y, level = 0;

	public pmKRobot(String caption){
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch(Exception e){}

		setTitle(caption);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		int         size = BORDER_SIZE * 2;
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(new Dimension(pmUtil.FIELD_SIZE + size, pmUtil.FIELD_SIZE + size));
		setSize(pmUtil.FIELD_SIZE + size, pmUtil.FIELD_SIZE + size);
		setLocation((screen.width - (pmUtil.FIELD_SIZE + size)) / 2, (screen.height - (pmUtil.FIELD_SIZE + size)) / 2);
		setUndecorated(true);
		setBackground(Color.BLUE);

		def_cur  = Cursor.getDefaultCursor();
		drag_cur = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

		img = new BufferedImage(pmUtil.FIELD_SIZE, pmUtil.FIELD_SIZE, BufferedImage.TYPE_INT_ARGB);
		mdc = img.createGraphics();

		try {
			on_create();
		} catch(Exception e){}

		KeyListener ekey = new KeyAdapter() {
			public void keyPressed(KeyEvent key){
				pmKRobot.this.key_down(key);
			}
		};
		addKeyListener(ekey);

		WindowListener wev = new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				pmKRobot.this.on_close(e);
			}
		};
		addWindowListener(wev);

		MouseListener mk = new MouseAdapter() {
			public void mousePressed(MouseEvent e){
				pmKRobot.this.mouse_down(e);
			}

			public void mouseReleased(MouseEvent e){
				pmKRobot.this.mouse_up(e);
			}
		};
		addMouseListener(mk);

		MouseMotionListener mp = new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e){
				pmKRobot.this.mouse_move(e);
  			}

			public void mouseMoved(MouseEvent e){
				pmKRobot.this.mouse_moved(e);
			}
		};
		addMouseMotionListener(mp);

		ActionListener etimer = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				pmKRobot.this.invalidate_game();
			}
		};
		timer = new javax.swing.Timer(20, etimer);
		timer.start();
		pos_x = pos_y = 0;
	}

	private void invalidate_game(){
		long cur;
		switch(gstate){
		case GAME_PLAY:
			cur = System.currentTimeMillis();
			if((cur - uframe) > DELAY_GAME) {
				uframe = cur;
				update_frame(cur);
				repaint();
			}
			break;
		case GAME_LEVEL:
			if(gmenu.delay_level())
				begin_game();
			break;
		}
	}

	private void on_create() throws Exception {
		borig  = pmUtil.loadImage("image/ground.jpg");
		bgimg  = new BufferedImage(pmUtil.FIELD_SIZE, pmUtil.FIELD_SIZE, BufferedImage.TYPE_INT_RGB);

		gmenu  = new pmMenu();
		maze   = new pmMaze();
		aliens = new pmAliens();
		user   = new pmUser();

		twork  = new ThdWork(maze.getMaze());
		thread = new Thread(twork);
		thread.start();

		gstate = GAME_MENU;
		load_level();
	}

	public void on_close(WindowEvent e){
		twork.close();
		try {
			thread.join();
		} catch(InterruptedException err){}
	}


	public void paint(Graphics dc){
		switch(gstate){
		case GAME_MENU:
			gmenu.draw_menu(mdc);
			break;
		case GAME_PLAY:
			mdc.drawImage(bgimg, 0, 0, null);
			maze.draw(mdc);
			aliens.draw(mdc);
			user.draw(mdc);
			break;
		case GAME_LEVEL:
			gmenu.draw_level(mdc);
			break;
		case GAME_WIN:
			gmenu.draw_win(mdc);
			break;
		case GAME_OVER:
			gmenu.draw_over(mdc);
			break;
		}
		dc.drawImage(img, BORDER_SIZE, BORDER_SIZE, null);
		dc.setColor(Color.BLUE);
		dc.drawRect(0, 0, pmUtil.FIELD_SIZE + 1, pmUtil.FIELD_SIZE + 1);
	}


	private void key_down(KeyEvent key){
		int ret;
		if(key.getKeyCode() == KeyEvent.VK_ESCAPE){
			on_close(null);
			System.exit(0);
			return;
		}

		switch(gstate){
		case GAME_OVER:
		case GAME_WIN:
		case GAME_MENU:
			ret = gmenu.key_down(key.getKeyCode());
			if(ret == 1)
				repaint();
			else if(ret == 2){//уровень
				gstate = GAME_LEVEL;
				gmenu.setLevel(level + 1);
				gmenu.setDelay();
				repaint();
			} else if(ret == 3){//выйти
				on_close(null);
				System.exit(0);
			}
			break;
		case GAME_PLAY:
			user.keyDown(key.getKeyCode());
			break;
		}
	}

	private void mouse_down(MouseEvent e){
		if(gstate == GAME_MENU || gstate == GAME_WIN || gstate == GAME_OVER){
			switch(gmenu.mouse_down(e.getX(), e.getY())){
			case 1:
				gstate = GAME_LEVEL;
				gmenu.setLevel(level + 1);
				gmenu.setDelay();
				repaint();
				return;
			case 2:
				on_close(null);
				System.exit(0);
				return;
                        }
		}

		if(e.getButton() == MouseEvent.BUTTON1){
			pos_x = e.getX();
			pos_y = e.getY();
			drag  = true;
			setCursor(drag_cur);
		}
	}

	private void mouse_up(MouseEvent e){
		if(drag){
			drag = false;
			setCursor(def_cur);
		}
	}

	private void mouse_moved(MouseEvent e){
		if(gstate == GAME_MENU || gstate == GAME_WIN || gstate == GAME_OVER){
			if(gmenu.mouse_move(e.getX(), e.getY()) == 1)
				repaint();
		}
	}

	private void mouse_move(MouseEvent e){
		if(drag)
			setLocation((getX() + e.getX()) - pos_x, (e.getY() + getY()) - pos_y);
	}

	public void update_frame(long msec){
		aliens.update_frame(maze.getMaze(), twork, user, msec);
		maze.update_frame(msec);

		switch(user.update_frame(maze.getMaze(), msec)){
		case pmUser.USER_WIN:
			++level;
			if(level >= maze.getCountLevels()){ //вы прошли всю игру
				gstate = GAME_WIN;
				level  = 0;
			} else {
				gstate = GAME_LEVEL;
				gmenu.setLevel(level + 1);
				gmenu.setDelay();
			}
			save_level();
			break;
		case pmUser.USER_DEATH:
			gstate = GAME_OVER;
			repaint();
			break;
		}
	}


	private void begin_game(){
		int left = 0, top = 0;
		switch(level % 4){
		case 1:
			left = 128;
			break;
		case 2:
			top = 128;
			break;
		case 3:
			left = top = 128;
			break;
		}
		pmUtil.fillImage(bgimg, borig, left, top, 128, 128);

		twork.reset();
		maze.setLevel(level);
		maze.put_objects(bgimg);
		aliens.put_objects(maze.getMaze());
		user.setLocation(maze.getMaze());
		gstate = GAME_PLAY;

		boolean speed = ((level + 1) <= (maze.getCountLevels() >> 1));
		aliens.setVelocity(speed);
		user.setVelocity(speed);
		repaint();
	}

	private void load_level(){
		int ch, n = 0;
		FileInputStream fp = null;
		try {
			fp = new FileInputStream(file_level);
			n  = 0;
			while((ch = fp.read()) != -1)
				n = n*10 + (ch - '0');
			fp.close();
		} catch(IOException e){}
		fp    = null;
		level = n;
	}

	private final byte[] chs = {0, 0, 0, 0};
	private void save_level(){
		FileOutputStream fp = null;
		try {
			fp    = new FileOutputStream(file_level);
			int i = 0;
			int n = level;
			do {
				chs[i++] = (byte) (n % 10 + '0');
			} while((n /= 10) != 0);

			for(i -= 1; i >= 0; --i)
				fp.write(chs[i]);
			fp.flush();
			fp.close();
		} catch(IOException e){}
		fp = null;
	}
}

