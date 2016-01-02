package com.samczsun.sigma.client.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

public class Events {
	private static Map<Node, Map<Class<?>, Consumer<Event>>> actions = new HashMap<>();

	private static EventHandler<Event> handleAll = (event) -> {
		Map<Class<?>, Consumer<Event>> all = actions.get(event.getTarget());
		if (all != null) {
			Class<?> get = event.getClass();
			Consumer<Event> consumer = all.get(get);
			while (consumer == null) {
				get = get.getSuperclass();
				if (get != Object.class) {
					consumer = all.get(get);
				} else {
					break;
				}
			}
			if (consumer != null) {
				consumer.accept(event);
			}
		}
	};

	public static Then on(Node field, Class<?> eventType) {
		return new Then(field, eventType);
	}

	public static class Then {
		private Node field;
		private Class<?> type;

		Then(Node field, Class<?> type) {
			this.field = field;
			this.type = type;
		}

		public void then(Consumer<Event> consumer) { //TODO: Destory (remove from map)?
			Map<Class<?>, Consumer<Event>> all = actions.get(field);
			if (all == null) {
				all = new HashMap<>();
				actions.put(field, all);
				field.addEventHandler(EventType.ROOT, handleAll);
			}
			all.put(type, consumer);
		}
	}
}
