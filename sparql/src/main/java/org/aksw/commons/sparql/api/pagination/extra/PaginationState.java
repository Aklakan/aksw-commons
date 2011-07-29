package org.aksw.commons.sparql.api.pagination.extra;

/**
 * @author Claus Stadler
 *         <p/>
 *         Date: 7/26/11
 *         Time: 8:00 PM
 */

import com.hp.hpl.jena.query.Query;

/**
 *
 *
 * @author raven
 *
 */
public class PaginationState
{
	private long nextOffset;
	private Long nextRemaining;

	private Query query;
	private long pageSize;

	/**
	 * Note: The query object's limit and offest will be modified.
	 * Use Query.cloneQuery in order to create a copy.
	 *
	 * @param query
	 * @param pageSize
	 */
	public PaginationState(Query query, long pageSize)
	{
		this.query = query;
		this.pageSize = pageSize;


		nextOffset = query.getOffset() == Query.NOLIMIT ? 0 : query.getOffset();
		nextRemaining = query.getLimit() == Query.NOLIMIT ? null : query.getLimit();
	}


	/**
	 * Returns the next query or null
	 *
	 * @return
	 * @throws Exception
	 */
	public Query next()
			throws Exception
	{
		if(nextOffset == 0) {
			query.setOffset(Query.NOLIMIT);
		} else {
			query.setOffset(nextOffset);
		}

		if(nextRemaining == null) {
			query.setLimit(pageSize);
			nextOffset += pageSize;
		} else {
			long limit = Math.min(pageSize, nextRemaining);
			nextOffset += limit;
			nextRemaining -= limit;

			if(limit == 0) {
				return null;
			}

			query.setLimit(limit);
		}

		return query;
	}
}
