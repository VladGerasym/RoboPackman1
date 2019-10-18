
package krobot;
import java.awt.Image;
import java.io.IOException;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.sound.sampled.Clip;



final class pmRusText {
	public final static byte[] menu_item1 = {15, 0, 23, 0, 18, 28, 50, 8, 3, 16, 0, 18, 28};
	public final static byte[] menu_item2 = {2, 27, 9, 18, 8, 50, 8, 7, 50, 8, 3, 16, 27};
	public final static byte[] menu_desc  = {16, 0, 7, 1, 5, 9, 18, 5, 50, 2, 17, 5, 50, 24, 0, 16, 8, 10, 8};
	public final static byte[] menu_ctrl  = {19, 15, 16, 0, 2, 11, 5, 13, 8, 5, 50, 2, 11, 5, 2, 14, 50, 2, 15, 16, 0, 2, 14, 50, 2, 13, 8, 7, 50, 2, 2, 5, 16, 21};
	public final static byte[] menu_level = {19, 16, 14, 2, 5, 13, 28};
	public final static byte[] menu_over  = {2, 27, 50, 15, 16, 14, 8, 3, 16, 0, 11, 8, 50};
	public final static byte[] menu_win   = {2, 27, 50, 15, 16, 14, 24, 11, 8, 50, 2, 17, 30, 50, 8, 3, 16, 19};
	public final static byte[] menu_quit  = {2, 27, 21, 14, 4, 50, 5, 17, 10, 5, 9, 15};
	public final static byte[] menu_regame= {13, 0, 23, 0, 18, 28, 50, 7, 0, 13, 14, 2, 14};

	public final static byte space = 50;
}



final class pmMenu {
	private Image wimg   = null;
	private Image font   = null;
	private Clip  snd    = null;
	private int   select = 0;
	private Color color  = new Color(0, 0, 127);
	private String slvl  = "";
	private long   delay = 0L;
	private int   sizeX, sizeY;

	public pmMenu(){
		try {
			create();
		} catch(IOException e){}
	}

	private void create() throws IOException {
		wimg  = pmUtil.loadImage("image/window.jpg");
		font  = pmUtil.loadImage("image/font.png");
		sizeX = font.getWidth(null) / 31;
		sizeY = font.getHeight(null);
		snd   = pmUtil.loadSound(getClass().getResource("sound/menu.wav"));
	}


	public void draw_menu(Graphics dc){
		dc.setColor(Color.BLACK);
		dc.fillRect(0, 0, pmUtil.FIELD_SIZE, pmUtil.FIELD_SIZE);
		dc.drawImage(wimg, (pmUtil.FIELD_SIZE - wimg.getWidth(null))/2, (pmUtil.FIELD_SIZE - wimg.getHeight(null)) / 2, null);

		int left = (pmUtil.FIELD_SIZE - pmRusText.menu_ctrl.length * 12) / 2;
		drawText(dc, pmRusText.menu_ctrl, left, pmUtil.FIELD_SIZE - 76, 12, 14);

		left = (pmUtil.FIELD_SIZE - pmRusText.menu_quit.length * 14) / 2;
		drawText(dc, pmRusText.menu_quit, left, pmUtil.FIELD_SIZE - 38, 14, 16);

		left = (pmUtil.FIELD_SIZE - pmRusText.menu_desc.length * sizeX) / 2;
		drawText(dc, pmRusText.menu_desc, left, 20);

		draw_buttons(dc, pmRusText.menu_item1, pmRusText.menu_item2);
	}

	//вывод уровня
        public void draw_level(Graphics dc){
		dc.setColor(Color.BLACK);
		dc.fillRect(0, 0, pmUtil.FIELD_SIZE, pmUtil.FIELD_SIZE);

		int top  = (pmUtil.FIELD_SIZE - sizeY)/2 - sizeY;
		int left = (pmUtil.FIELD_SIZE - pmRusText.menu_level.length*sizeX) / 2 - sizeX;
		drawText(dc, pmRusText.menu_level, left, top);

		dc.setColor(Color.YELLOW);
		dc.drawString(slvl, left + pmRusText.menu_level.length*sizeX, top + 14);
	}

	public boolean delay_level(){
		return (System.currentTimeMillis() > delay);
	}


	public void draw_win(Graphics dc){
		dc.setColor(Color.BLACK);
		dc.fillRect(0, 0, pmUtil.FIELD_SIZE, pmUtil.FIELD_SIZE);
		dc.drawImage(wimg, (pmUtil.FIELD_SIZE - wimg.getWidth(null))/2, (pmUtil.FIELD_SIZE - wimg.getHeight(null)) / 2, null);

		int top  = 30;
		int left = (pmUtil.FIELD_SIZE - pmRusText.menu_win.length*sizeX) / 2;
		drawText(dc, pmRusText.menu_win, left, top);
		drawText(dc, pmRusText.menu_win, left, pmUtil.FIELD_SIZE - top - sizeY);

		draw_buttons(dc, pmRusText.menu_regame, pmRusText.menu_item2);
	}

	public void draw_over(Graphics dc){
		dc.setColor(Color.BLACK);
		dc.fillRect(0, 0, pmUtil.FIELD_SIZE, pmUtil.FIELD_SIZE);
		dc.drawImage(wimg, (pmUtil.FIELD_SIZE - wimg.getWidth(null))/2, (pmUtil.FIELD_SIZE - wimg.getHeight(null)) / 2, null);

		int top  = 30;
		int left = (pmUtil.FIELD_SIZE - pmRusText.menu_over.length*sizeX) / 2;
		drawText(dc, pmRusText.menu_over, left, top);
		drawText(dc, pmRusText.menu_over, left, pmUtil.FIELD_SIZE - top - sizeY);

		draw_buttons(dc, pmRusText.menu_regame, pmRusText.menu_item2);
	}

	private void draw_buttons(Graphics dc, byte[] item1, byte[] item2){
		int top = pmUtil.FIELD_SIZE / 2 - sizeY;
		int left    = (pmUtil.FIELD_SIZE - item1.length * sizeX) / 2;
		dc.setColor((select == 0) ? Color.BLUE : color);
		dc.fillRect(100, top - 15, 310, 46);
		if(select == 0){
                  dc.setColor(Color.RED);
                  dc.drawRect(100, top - 15, 310, 46);
		}
		drawText(dc, item1, left, top);

		top += sizeY * 4;
		dc.setColor((select == 1) ? Color.BLUE : color);
		dc.fillRect(100, top - 15, 310, 46);
		if(select == 1){
			dc.setColor(Color.RED);
			dc.drawRect(100, top - 15, 310, 46);
		}
 		drawText(dc, item2, (pmUtil.FIELD_SIZE - item2.length * sizeX)/2, top);
	}

	public int key_down(int key){
		switch(key){
                case KeyEvent.VK_UP:
		case KeyEvent.VK_W:
			if(select == 1){
				select = 0;
				snd.setFramePosition(0);
				snd.start();
				return 1;
			}
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_S:
			if(select == 0){
				select = 1;
				snd.setFramePosition(0);
				snd.start();
				return 1;
			}
			break;
		case KeyEvent.VK_ENTER:
			if(select == 0)
				return 2;
			else if(select == 1)
				return 3;
			break;
		}
		return 0;
	}

	public int mouse_move(int x, int y){
		int top     = pmUtil.FIELD_SIZE / 2 - sizeY - 15;
		int left    = 100;
		int bottom  = top + 46;
		int right   = left + 310;
		if((x >= left && x <= right) && (y >= top && y <= bottom)){
			if(select == 1){
				select = 0;
				snd.setFramePosition(0);
				snd.start();
				return 1;
			}
			return 2;
		}

		top    += sizeY * 4;
		bottom += sizeY * 4;
		if((x >= left && x <= right) && (y >= top && y <= bottom)){
			if(select == 0){
				select = 1;
				snd.setFramePosition(0);
				snd.start();
				return 1;
			}
			return 3;
		}
		return 0;
	}

	public int mouse_down(int x, int y){
		if(mouse_move(x, y) > 0)
			return select + 1;
		return 0;
	}

	private void drawText(Graphics dc, byte[] arr, int x, int y){
		int fx;
		for(byte ch : arr){
			if(ch != pmRusText.space){
				fx = (int)ch * sizeX;
				dc.drawImage(font, x, y, x + sizeX, y + sizeY, fx, 0, fx + sizeX, sizeY, null);
			}
			x += sizeX;
		}
	}

	private void drawText(Graphics dc, byte[] arr, int x, int y, int w, int h){
		int fx;
		for(byte ch : arr){
			if(ch != pmRusText.space){
				fx = (int)ch * sizeX;
				dc.drawImage(font, x, y, x + w, y + h, fx, 0, fx + sizeX, sizeY, null);
			}
			x += w;
		}
	}

	public void setLevel(int level){
		slvl = " - " + String.valueOf(level);
	}

	public void setDelay(){
		delay = System.currentTimeMillis() + 2000L;
	}
}
