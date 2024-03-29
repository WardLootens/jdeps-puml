package be.whocarez.jdeps;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassDependency {

	private static final Pattern PATTERN = Pattern.compile("\\s+(\\S+)\\s+->\\s+(\\S+)\\s+(\\S*.jar)");

	private final String from;
	private final String to;
	private final String toJar;

	public ClassDependency(String from, String to, String toJar) {
		this.from = from;
		this.to = to;
		this.toJar = toJar;
	}

	public static Optional<ClassDependency> ofLine(String line) {
		final Matcher m = PATTERN.matcher(requireNonNullElse(line, ""));
		if (m.find()) {
			return Optional.of(new ClassDependency(m.group(1), m.group(2), m.group(3)));
		}
		return Optional.empty();
	}

	public static ClassDependency stripPath(ClassDependency dependency, String pathPrefix) {
		return new ClassDependency(
				dependency.from.replace(pathPrefix, ""),
				dependency.to.replace(pathPrefix, ""),
				dependency.toJar
		);
	}

	public static ClassDependency toPackageDependency(ClassDependency dependency, int depth) {
		return new ClassDependency(
				extractPackage(dependency.from, depth),
				extractPackage(dependency.to, depth),
				dependency.toJar
		);
	}

	private static String extractPackage(String className, int depth) {
		return Arrays.stream(className.split("\\."))
				.limit(depth)
				.collect(joining("."));
	}

	public String from() {
		return from;
	}

	public String to() {
		return to;
	}

	public boolean isSelfReference() {
		return Objects.equals(from, to);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClassDependency that = (ClassDependency) o;
		return Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(toJar, that.toJar);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to, toJar);
	}

	public String toPuml(Long count) {
		return String.format("[%s] --> [%s] : %s", from, to, count);
	}

	@Override
	public String toString() {
		return String.format("[%s] --> [%s] (%s)", from, to, toJar);
	}

	public static void main(String[] args) throws IOException {

		final Path jdepsFile = Paths.get(args[0]);
		final Path pumlFile = Paths.get(args[1]);
		final List<String> includes = args[2] == null || args[2].isBlank() ? List.of() : List.of(args[2].split(";"));
		final String pathPrefixToIgnore = args[3];
		final int packageDepth = Integer.parseInt(args[4]);

		final Map<ClassDependency, Long> dependencies = Files.lines(jdepsFile)
				.map(ClassDependency::ofLine)
				.flatMap(Optional::stream)
				.map(dep -> toPackageDependency(dep, packageDepth))
				.map(dep -> stripPath(dep, pathPrefixToIgnore))
				.filter(dep -> includes.isEmpty() || includes.contains(dep.from()))
				.filter(dep -> includes.isEmpty() || includes.contains(dep.to()))
				.filter(not(ClassDependency::isSelfReference))
				.sorted(comparing(ClassDependency::from))
				.collect(groupingBy(it -> it, LinkedHashMap::new, counting()));

		final List<String> output = new ArrayList<>();

		output.add("@startuml");
		dependencies.forEach((dep, count) -> output.add(dep.toPuml(count)));
		output.add("@enduml");

		Files.write(pumlFile, output, UTF_8);

	}

}
