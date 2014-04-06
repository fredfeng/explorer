package test.util;

public abstract class Id {
	Object id;
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Id && id.equals( ((Id)other).id ) ? true : false;
	}
}
