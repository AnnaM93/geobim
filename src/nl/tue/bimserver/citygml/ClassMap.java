package nl.tue.bimserver.citygml;

import java.util.LinkedList;
import java.util.List;

public class ClassMap<T> {
	private class Entry {
		Class<?> klass;
		T value;
		
		public Entry(Class<?> klass, T value) {
			this.klass = klass;
			this.value = value;
		}
	}
	
	private List<Entry> list = new LinkedList<Entry>();
	
	public void add(Class<?> klass, T value) {
		list.add(new Entry(klass, value));
	}
	
	public boolean has(Object key) {
		return get(key) != null;
	}
	
	public T get(Object key) {
		for(Entry entry : list) {
			if(entry.klass.isInstance(key)) {
				return entry.value;
			}
		}
		return null;
	}	
}
