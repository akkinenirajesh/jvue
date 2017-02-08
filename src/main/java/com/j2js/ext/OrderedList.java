package com.j2js.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderedList<T> {

	private Map<Integer, List<T>> list = new HashMap<>();
	private List<Integer> orders = new ArrayList<>();
	private int size;

	public void add(T e) {
		add(e, 0);
	}

	public void add(T e, int order) {
		size++;
		List<T> l = list.get(order);
		if (l == null) {
			orders.add(order);
			Collections.sort(orders);
			l = new ArrayList<>();
			list.put(order, l);
		}
		l.add(e);
	}

	public T get(int i) {
		int s = 0;
		for (int o : orders) {
			List<T> l = list.get(o);
			int total = s + l.size();
			if (total > i) {
				return l.get(i - s);
			}
			s = total;
		}
		throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + size);
	}

	public int size() {
		return size;
	}
}
