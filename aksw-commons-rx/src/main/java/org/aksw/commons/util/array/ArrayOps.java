package org.aksw.commons.util.array;


public interface ArrayOps<A> {
	A create(int size);
	
	Object get(A array, int index);
	void set(A array, int index, Object value);
	
	int length(A array);
	
	void fill(A array, int offset, int length, Object value);
	void copy(A src, int srcPos, A dest, int destPos, int length);
	Object getDefaultValue();
	
	@SuppressWarnings("unchecked")
	default void fillRaw(Object array, int offset, int length, Object value) {
		fill((A)array, offset, length, value);
	}

	@SuppressWarnings("unchecked")
	default void copyRaw(Object src, int srcPos, Object dest, int destPos, int length) {
		copy((A)src, srcPos, (A)dest, destPos, length);
	}
	
	@SuppressWarnings("unchecked")
	default Object getRaw(Object array, int index) {
		return get((A)array, index);
	}

	@SuppressWarnings("unchecked")
	default void setRaw(Object array, int index, Object value) {
		set((A)array, index, value);
	}

	@SuppressWarnings("unchecked")
	default void lengthRaw(Object array) {
		length((A)array);
	}
	

	public static final ArrayOpsByte BYTE = new ArrayOpsByte();
	public static final ArrayOpsObject OBJECT = new ArrayOpsObject();
}
