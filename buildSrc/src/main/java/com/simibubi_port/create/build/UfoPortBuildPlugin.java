package com.simibubi_port.create.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.simibubi_port.create.build.processor.DeduplicationProcessor;
import com.simibubi_port.create.build.processor.ValidationProcessor;

import com.simibubi_port.create.build.tasks.SortAccessWidenerTask;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UfoPortBuildPlugin implements Plugin<Project> {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final List<ProjectProcessor> processors = List.of(
			new ValidationProcessor()
	);

	private final List<ProjectProcessor> lateProcessors = List.of(
			new DeduplicationProcessor()
	);

	@Override
	public void apply(@NotNull Project project) {
		project.getExtensions().create("ufoport", UfoPortExtension.class);
		project.getTasks().register("sortAccessWidener", SortAccessWidenerTask.class);

		this.processors.forEach(processor -> processor.apply(project));

		project.afterEvaluate(p -> this.lateProcessors.forEach(processor -> processor.apply(p)));
	}
}
