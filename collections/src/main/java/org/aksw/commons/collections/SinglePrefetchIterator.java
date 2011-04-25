package org.aksw.commons.collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * An abstract base class for iterating over containers of unknown size. This
 * works by prefetching junks of the container: Whenever the iterator reaches
 * the end of a chunk, the method "myPrefetch" is called.
 *
 *
 * Note that once the iterator is finished (myPrefetch returned null),
 * myPrefetch will never be called again. This means, that if myPrefetch is
 * called, the iterator hasn't reached its end yet.
 *
 *
 * @author raven_arkadon
 * @param <T>
 */
public abstract class SinglePrefetchIterator<T>
	implements Iterator<T>
{
	private static Logger logger = LoggerFactory.getLogger(SinglePrefetchIterator.class);
	private T	    current		= null;
	private boolean finished	= false;

	private boolean advance     = true;

	protected abstract T prefetch()
		throws Exception;

	protected SinglePrefetchIterator()
	{
	}

	protected T finish()
	{
		this.finished = true;

		close();
		return null;
	}

	private void _prefetch()
	{
		try {
			current = prefetch();
		}
		catch(Exception e) {
			current = null;
			logger.error("Error prefetching data", e);
		}
	}

	@Override
	public boolean hasNext()
	{
		if(advance) {
			_prefetch();
			advance = false;
		}

		return finished == false;
	}

	@Override
	public T next()
	{
		if(finished) {
			throw new IndexOutOfBoundsException();
		}

		if(advance)
			_prefetch();

		advance = true;
		return current;
	}


	/**
	 * An iterator must always free all resources once done with iteration.
	 * However, if iteration is aborted, this method should be called.
	 *
	 */
	public void close()
	{
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("Not supported.");
	}
}
