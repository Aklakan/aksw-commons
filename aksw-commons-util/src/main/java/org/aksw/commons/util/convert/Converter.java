package org.aksw.commons.util.convert;

import java.util.function.Function;

/** Interface to describe a conversion from one Java type to another */
public interface Converter {
	Class<?> getFrom();
	Class<?> getTo();
	Function<Object, Object> getFunction();
	
	default Converter andThen(Converter next) {
		Class<?> provided = getTo();
		Class<?> accepted = next.getFrom();
		
		if (!accepted.isAssignableFrom(provided)) {
			throw new RuntimeException(String.format("Cannot chain converters because the provided outgoing type %1$s is not accepted by %$2s",
					provided, accepted));
		}

		Class<?> newSrc = getFrom();
		Class<?> newTgt = next.getTo();

		return new ConverterImpl(newSrc, newTgt, in -> {
			Object tmp = convert(in);
			Object r = next.convert(tmp);
			return r;
		});
	}
	
	default Object convert(Object obj) {
		Function<Object, Object> fn = getFunction();
		Object result = fn.apply(obj);
		return result;
	}
}