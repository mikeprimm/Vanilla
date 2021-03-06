/*
 * This file is part of Vanilla.
 *
 * Copyright (c) 2011-2012, SpoutDev <http://www.spout.org/>
 * Vanilla is licensed under the SpoutDev License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.vanilla.controller.effect;

/**
 * Represents an entity effect that is applied to an entity.
 */
public class ActiveEntityEffect {
	private final EntityEffect effect;
	private byte amplitude;
	private short duration;

	public ActiveEntityEffect(EntityEffect effect, byte amplitude, short duration) {
		this.effect = effect;
		this.amplitude = amplitude;
		this.duration = duration;
	}

	/**
	 * Gets the effect applied.
	 * @return effect
	 */
	public EntityEffect getEffect() {
		return effect;
	}

	/**
	 * Gets the amplitude of the effect.
	 * @return amplitude of effect.
	 */
	public byte getAmplitude() {
		return amplitude;
	}

	/**
	 * Gets the duration of the effect.
	 * @return duration
	 */
	public short getDuration() {
		return duration;
	}

	/**
	 * Sets the duration of the effect.
	 * @param duration
	 */
	public void setDuration(short duration) {
		this.duration = duration;
	}

	/**
	 * Decrements the effects duration by one.
	 * @return
	 */
	public boolean pulse() {
		if (duration < 1) {
			return false;
		}
		--duration;
		return true;
	}
}
