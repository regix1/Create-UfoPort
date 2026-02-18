package com.simibubi_port.create.build.processor;

import com.simibubi_port.create.build.ProjectProcessor;

import com.simibubi_port.create.build.tasks.ValidateModuleTask;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;

public class ValidationProcessor implements ProjectProcessor {
	@Override
	public void apply(Project project) {
		if (project == project.getRootProject())
			return; // only apply to modules
		TaskContainer tasks = project.getTasks();
		tasks.register("validateModule", ValidateModuleTask.class, validate -> {
			Task assemble = tasks.getByName("assemble");
			Task validateAw = tasks.getByName("validateAccessWidener");
			assemble.dependsOn(validateAw, validate);
		});
	}
}
