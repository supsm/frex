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

package io.vram.frex.impl.material.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.vram.frex.api.material.MaterialView;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.scores.Team;

/**
 * Stripped-down adaptation of vanilla class used for entity loot predicates.
 *
 * <p>Note that prior to 1.19 this had predicates for player and fish hook, but in 1.19
 * those predicate types require server level instances which is not possible for FREX.
 * As a result, they were stripped.
 */
public class EntityOnly extends EntityBiPredicate {
	public static final EntityOnly ANY;

	@Nullable private final MobEffectsPredicate effects;
	@Nullable private final NbtPredicate nbt;
	@Nullable private final EntityFlagsPredicate flags;
	@Nullable private final EntityEquipmentPredicate equipment;
	@Nullable private final String team;
	@Nullable private final ResourceLocation catType;

	private EntityOnly(@Nullable MobEffectsPredicate effects, NbtPredicate nbt, EntityFlagsPredicate flags, EntityEquipmentPredicate equipment, @Nullable String team, @Nullable ResourceLocation catType) {
		this.effects = effects;
		this.nbt = nbt;
		this.flags = flags;
		this.equipment = equipment;
		this.team = team;
		this.catType = catType;
	}

	public boolean test(Entity entity) {
		return test(entity, null);
	}

	@Override
	public boolean test(Entity entity, MaterialView ignored) {
		if (this == ANY) {
			return true;
		} else if (entity == null) {
			return false;
		} else {
			if (effects != null && !effects.matches(entity)) {
				return false;
			} else if (nbt != null && !nbt.matches(entity)) {
				return false;
			} else if (flags != null && !flags.matches(entity)) {
				return false;
			} else if (equipment != null && !equipment.matches(entity)) {
				return false;
			} else {
				if (team != null) {
					final Team abstractTeam = entity.getTeam();

					if (abstractTeam == null || !team.equals(abstractTeam.getName())) {
						return false;
					}
				}

				return catType == null || entity instanceof Cat && ((Cat) entity).getTextureId().equals(catType);
			}
		}
	}

	public static EntityOnly fromJson(@Nullable JsonElement json) {
		if (json != null && !json.isJsonNull()) {
			final JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "entity");
			final MobEffectsPredicate effects = predicateFromJson(jsonObject.get("effects"), MobEffectsPredicate.CODEC);
			final NbtPredicate nbt = predicateFromJson(jsonObject.get("nbt"), NbtPredicate.CODEC);
			final EntityFlagsPredicate flags = predicateFromJson(jsonObject.get("flags"), EntityFlagsPredicate.CODEC);
			final EntityEquipmentPredicate equipment = predicateFromJson(jsonObject.get("equipment"), EntityEquipmentPredicate.CODEC);
			//final PlayerPredicate player = PlayerPredicate.fromJson(jsonObject.get("player"));
			//final FishingHookPredicate fishHook = FishingHookPredicate.fromJson(jsonObject.get("fishing_hook"));
			final String team = GsonHelper.getAsString(jsonObject, "team", (String) null);
			final ResourceLocation catType = jsonObject.has("catType") ? ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "catType")) : null;

			return new EntityOnly(effects, nbt, flags, equipment, team, catType);
		} else {
			return ANY;
		}
	}

	public static <T> T predicateFromJson(@Nullable JsonElement jsonElement, Codec<T> codec) {
		return jsonElement != null && !jsonElement.isJsonNull() ? codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(JsonParseException::new) : null;
	}

	static {
		ANY = new EntityOnly(null, null, null, null, (String) null, (ResourceLocation) null);
	}

	@Override
	public boolean equals(Object obj) {
		// not worth elaborating
		return this == obj;
	}
}
