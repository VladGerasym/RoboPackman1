
package krobot;



final class Element<E> {
	public E          value;
	public int        state;
	public Element<E> next;
}


final class ObjList<E> {
	private final static int ST_LIFE  = 1;
	private final static int ST_DEATH = 2;

	private Element<E> lst = null;
	private Element<E> pos = null;
	private int        cnt = 0;
	private int        max = 0;

	public ObjList(int size){
		setLength(size);
	}

	public void add(E val){
		Element<E> p = lst;
		while((p != null) && (p.value != null))
			p = p.next;

		if(p != null)
			p.value = val;
	}

	public E activate(){
		Element<E> p = lst, prev = lst;
		while(p != null){
			if(p.state == ST_DEATH){
				p.state = ST_LIFE;
				++cnt;
				return p.value;
			}
			prev = p;
			p    = p.next;
		}

		if(prev != null){
			prev.state = ST_LIFE;
			return prev.value;
		}
		return null;
	}

	public void deactivate(E val){
		Element<E> p = lst;
		while((p != null) && !p.value.equals(val))
			p = p.next;

		if(p != null){
    			p.state = ST_DEATH;
			--cnt;
		}
	}

	public void setLength(int size){
		clear();
		for(int i = 0; i < size; ++i){
			Element<E> p = new Element<E>();
			p.state = ST_DEATH;
			p.value = null;
			p.next  = lst;
			lst     = p;
		}
		max = size;
	}

	public void deactivateAll(){
		for(Element<E> p = lst; p != null; p = p.next)
			p.state = ST_DEATH;
	}

	public void clear(){
		Element<E> tmp;
		while(lst != null){
			tmp = lst;
			lst = lst.next;
			tmp.value = null;
 			tmp = null;
		}
		lst = null;
		pos = null;
		cnt = 0;
	}

	public int getSize(){
		return cnt;
	}

	public int getMaxSize(){
		return max;
	}

	public void start(){
		pos = lst;
	}

	public boolean isMove(){
		while((pos != null) && (pos.state != ST_LIFE))
			pos = pos.next;
		return (pos != null);
        }

	public E current(){
		return pos.value;
        }

	public void next(){
		pos = pos.next;
	}

	public void deactivate(){
		if(pos != null){
			pos.state = ST_DEATH;
			--cnt;
		}
	}
}



final class PointArray {
	private int[] arr = null;
	private int   cnt = 0;
	private int   max = 0;

	public PointArray(){}
	public PointArray(int size){
        	resize(size);
	}

	public void add(int x, int y){
		if(cnt < max)
			arr[cnt++] = (x << 16) | y;
	}

	public void removeAt(int index){
		if(index < cnt){
			System.arraycopy(arr, index + 1, arr, index, cnt - (index + 1));
			--cnt;
		}
	}

	public void reverse(){
		int t, j = cnt - 1;
		for(int i = 0; i < j; ++i, --j){
			t      = arr[i];
			arr[i] = arr[j];
			arr[j] = t;
		}
	}

	public void resize(int size){
		clear();
		arr = new int[size];
		max = size;
	}

	public void setAt(int index, int x, int y){
		arr[index] = (x << 16) | y;
	}

	public void setX(int index, int x){
		arr[index] = (arr[index] & 0xFFFF) | (x << 16);
	}

	public void setY(int index, int y){
		arr[index] = (arr[index] & 0xFFFF0000) | y;
	}

	public int getX(int index){
		return (arr[index] >> 16);
	}

	public int getY(int index){
		return (arr[index] & 0xFFFF);
	}

	public int getSize(){
		return cnt;
	}

	public void reset(){
		cnt = 0;
	}

	public void clear(){
		arr = null;
		cnt = max = 0;
	}

	public int getMaxSize(){
		return max;
	}
}



final class PointSQ {
	public final static int STACK_LIFO = 0;
	public final static int QUEUE_FIFO = 1;
	private int[] arr  = null;
	private int   cnt  = 0;
	private int   max  = 0;
	private int   type = STACK_LIFO;

	public PointSQ(){}

	public PointSQ(int size){
		setSize(size);
	}

	public void setTypeSQ(int _type){
		type = _type;
	}

	public int getTypeSQ(){
		return type;
	}

	public void push(int x, int y){
		if(cnt < max)
			arr[cnt++] = (x << 16) | y;
	}

	public void pop(){
		if(cnt > 0){
			if(type == QUEUE_FIFO)
				System.arraycopy(arr, 1, arr, 0, cnt - 1);
			--cnt;
		}
        }

	public int topX(){
		int index = (type == QUEUE_FIFO) ? 0 : (cnt - 1);
		return (arr[index] >> 16);
	}

	public int topY(){
		int index = (type == QUEUE_FIFO) ? 0 : (cnt - 1);
		return (arr[index] & 0xFFFF);
	}

	boolean empty(){
		return (cnt == 0);
	}

	public void setSize(int size){
		clear();
		arr = new int[size];
		max = size;
	}

	public void reset(){
		cnt = 0;
	}

	public void clear(){
		arr = null;
		cnt = max = 0;
	}

	public int getSize(){
		return cnt;
	}
}
