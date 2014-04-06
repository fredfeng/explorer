package test.util;

public class dotEdge extends Edge {
	public dotEdge(Id id) {
		super(id);
	}
	
	@Override
	public boolean isDot() {
		return true;
	}
}
