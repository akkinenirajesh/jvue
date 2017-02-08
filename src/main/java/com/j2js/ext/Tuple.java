package com.j2js.ext;

public class Tuple<T, R> {

	private T t;
	private R r;

	public Tuple(T t, R r) {
		this.t = t;
		this.r = r;
	}

	public T getT() {
		return t;
	}

	public R getR() {
		return r;
	}
}
