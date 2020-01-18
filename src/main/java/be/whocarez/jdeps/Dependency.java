package be.whocarez.jdeps;

import java.util.Objects;
import java.util.Optional;

public class Dependency {

	private final String from;
	private final String to;

	public Dependency(String from, String to) {
		this.from = from;
		this.to = to;
	}

	public static Optional<Dependency> ofLine(String line) {
		if (line.contains(" -> ")) {
			final String[] split = line.split(" -> ");
			return Optional.of(new Dependency(split[0], split[1]));
		}
		return Optional.empty();
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
	public String toString() {
		return String.format("\"%s\" --> \"%s\"", from, to);
	}
}
