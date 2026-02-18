package io.github.fabricators_of_create.porting_lib.entity.extensions;

import org.jetbrains.annotations.Nullable;

import io.github.fabricators_of_create.porting_lib.entity.client.MobEffectRenderer;

public interface MobEffectExtensions {
	@Nullable
	default MobEffectRenderer getRenderer() {
		return null;
	}
}
