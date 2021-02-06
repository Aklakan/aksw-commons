package org.aksw.commons.collector.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Supplier;

import org.aksw.commons.collector.domain.Accumulator;
import org.aksw.commons.collector.domain.ParallelAggregator;
import org.aksw.commons.lambda.serializable.SerializableSupplier;


/**
 * Wrapper for a supplier of accumulators with signature I -&gt; Collection&lt;I&gt;
 * This means, that items from the collection can be used as input. This allows
 * for 'natural' combination of accumulators by adding the items of one accumulator to the other.
 * 
 * Note that this is operation always works for accumulators of that signature,
 * yet whether it is semantically meaningful depends on the application context.
 * 
 * @author raven
 *
 * @param <I>
 * @param <C>
 */
public class AggNatural<I, C extends Collection<I>>
	implements ParallelAggregator<I, C, Accumulator<I, C>>, Serializable
{
	private static final long serialVersionUID = 0;

	protected Supplier<? extends Accumulator<I, C>> accSupplier;
	
	public AggNatural(SerializableSupplier<? extends Accumulator<I, C>> accSupplier) {
		super();
		this.accSupplier = accSupplier;
	}

	@Override
	public Accumulator<I, C> createAccumulator() {
		return accSupplier.get();
	}

	@Override
	public Accumulator<I, C> combine(Accumulator<I, C> a, Accumulator<I, C> b) {
		return ParallelAggregators.combineAccumulators(a, b, x -> x, x -> x);
	}
}
