package com.samczsun.sigma.commons;

public class Callbacks {
	@FunctionalInterface
	public static interface Consumer<A, B> {
		public void accept(A a, B b);
	}
}
