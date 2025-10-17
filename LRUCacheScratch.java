import java.util.HashMap;
import java.util.Map;

public class LRUCacheScratch {

    public class DNode{
        int key;
        int value;
        DNode prev;
        DNode next;

        DNode(int key, int value){
            this.key = key;
            this.value = value;
        }
    }

    private Map<Integer, DNode> cache;  
    private int capacity;
    private DNode head, tail;

    public LRUCacheScratch(int capacity){
        this.capacity = capacity;
        cache = new HashMap<>();
        head = new DNode(0, 0);
        tail = new DNode(0, 0);
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key){
        if(!cache.containsKey(key)){
            return -1;
        }
        DNode node = cache.get(key);
        remove(node);
        insertAtFront(node);
        return node.value;
    }

    private void remove(DNode node){
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void insertAtFront(DNode node){
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }
    
    public void put(int key, int value){
        if (cache.containsKey(key)) {
            remove(cache.get(key));
        }
        DNode newNode = new DNode(key, value);
        cache.put(key, newNode);
        insertAtFront(newNode);
        if (cache.size() > capacity) {
            DNode tailNode = removeTail();
            cache.remove(tailNode.key);
        }
    }

    private DNode removeTail() {
        DNode tailNode = tail.prev;
        remove(tailNode);
        return tailNode;
    }
}
