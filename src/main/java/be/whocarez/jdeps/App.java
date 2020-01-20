package be.whocarez.jdeps;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class App {

	public static void main(String[] args) throws IOException {

		final Path jdepsFile = Paths.get(args[0]);
		final Path pumlFile = Paths.get(args[1]);
		final List<String> includes = List.of(args[2].split(";"));
		final String pathPrefixToIgnore = args[3];

		final List<Dependency> dependencies = Files.lines(jdepsFile)
				.map(Dependency::ofLine)
				.flatMap(Optional::stream)
				.map(dep -> Dependency.stripPath(dep, pathPrefixToIgnore))
				.filter(dep -> includes.contains(dep.from()))
				.filter(dep -> includes.contains(dep.to()))
				.filter(not(Dependency::isSelfReference))
				.collect(toList());

		final List<String> output = new ArrayList<>();

		output.add("@startuml");
		includes.stream()
				.map(jar -> String.format("package \"%s\"{}", jar))
				.forEach(output::add);
		dependencies.forEach(dep -> output.add(dep.toString()));
		output.add("@enduml");

		Files.write(pumlFile, output, UTF_8);

	}



}
