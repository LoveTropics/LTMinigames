package org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.state;

public final class PlantHealth {
	public static final PlantState.Key<PlantHealth> KEY = PlantState.Key.create();

	private int health;
	private final int maxHealth;

	public PlantHealth(int health) {
		this.health = health;
		maxHealth = health;
	}

	public void decrement(int amount) {
		health = Math.max(0, health - amount);
	}

	public void increment(int amount) {
		health = Math.min(maxHealth, health + amount);
	}

	public boolean isDead() {
		return health <= 0;
	}

	public int health() {
		return health;
	}

	public double healthPercent() {
		return health / (double) maxHealth;
	}
}
