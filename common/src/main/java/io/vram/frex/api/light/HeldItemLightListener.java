/*
 * This file is part of FREX and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.vram.frex.api.light;

import io.vram.frex.api.config.FrexFeature;
import io.vram.frex.impl.light.HeldItemLightListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface HeldItemLightListener {
	/**
	 * Use this event to add or modify held item lighting due to effects, worn items
	 * or other conditions that aren't completely dependent on held items. The renderer will
	 * call this event once per frame after the held light is retrieved for the current player
	 * or camera entity.
	 *
	 * <p>Held lights for non-camera entities are currently not supported.
	 *
	 * @param holdingEntity The entity that is holding (or not holding) a light provider.
	 *                      Currently this is always the camera entity.
	 *
	 * @param heldStack     The item stack that was used to determine the default result.
	 *                      May be empty. If no light was found, will be from the secondary hand.
	 *
	 * @param defaultResult The light result determined by the renderer. Will be the same in all
	 *                      listener invocations. Use this to know if another listener has
	 *                      already changed the current result.
	 *
	 * @param currentResult The light result that should be returned if the listener does not
	 *                      modify it. May already have been changed by a prior listener.
	 *                      Compare with default result to detect this.
	 *
	 * @return              The light that should be used. Return {@link ItemLight#NONE} to disable
	 *                      held light. Can be modified by subsequent listener invocations.
	 */
	ItemLight onGetHeldItemLight(LivingEntity holdingEntity, ItemStack heldStack, ItemLight defaultResult, ItemLight currentResult);

	static void register(HeldItemLightListener listener) {
		HeldItemLightListenerImpl.register(listener);
	}

	/**
	 * For use by renderer implementations.
	 * Renderers that implement item lights should declare {@link FrexFeature#HELD_ITEM_LIGHTS}.
	 */
	static ItemLight apply(LivingEntity holdingEntity, ItemStack heldStack, ItemLight defaultResult) {
		return HeldItemLightListenerImpl.apply(holdingEntity, heldStack, defaultResult);
	}
}
