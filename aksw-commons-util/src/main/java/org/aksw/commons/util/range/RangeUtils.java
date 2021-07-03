package org.aksw.commons.util.range;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.BoundType;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

public class RangeUtils {
    public static final Range<Long> rangeStartingWithZero = Range.atLeast(0l);

    public static <T> List<T> subList(List<T> list, Range<Integer> subRange) {
        // Subrange's offset must be equal-or-greater-than 0

        Range<Integer> listRange = Range.lessThan(list.size());
        Range<Integer> effectiveRange = listRange.intersection(subRange);
        ContiguousSet<Integer> set = ContiguousSet.create(effectiveRange, DiscreteDomain.integers());
        int first = set.first();
        int last = set.last();
        List<T> result = list.subList(first, last + 1);
        return result;
    }

    public static <T extends Comparable<T>> Optional<T> tryGetSingleton(Range<T> range) {
        boolean isSingleton = isSingleton(range);
        return Optional.ofNullable(isSingleton ? range.lowerEndpoint() : null);
    }

    public static boolean isSingleton(Range<?> range) {
        boolean result =
                range.hasLowerBound() &&
                range.hasUpperBound() &&
                range.lowerBoundType().equals(BoundType.CLOSED) &&
                range.upperBoundType().equals(BoundType.CLOSED) &&
                Objects.equals(range.lowerEndpoint(), range.upperEndpoint());

        return result;
    }

    public static long pickLong(Range<Long> range, Random random) {
        Range<Long> norm = range.intersection(Range.closed(Long.MIN_VALUE, Long.MAX_VALUE));
        long l = norm.lowerEndpoint();
        long u = norm.upperEndpoint();
        double pick = random.nextDouble();
        long result = l + Math.round(pick * (u - l));
        return result;
    }

    public static double pickDouble(Range<Double> range, Random random) {
        Range<Double> norm = range.intersection(Range.closed(Double.MIN_VALUE, Double.MAX_VALUE));
        double l = norm.lowerEndpoint();
        double u = norm.upperEndpoint();
        double pick = random.nextDouble();
        double result = l + pick * (u - l);
        return result;
    }



    public static CountInfo toCountInfo(Range<? extends Number> range) {
        Long min = range.hasLowerBound() ? range.lowerEndpoint().longValue() : 0;
        Long max = range.hasUpperBound() ? range.upperEndpoint().longValue() : null;

        CountInfo result = new CountInfo(
                min,
                max == null ? true : !max.equals(min),
                max
        );
        return result;
    }

    /**
     * Convert a range relative within another one to an absolute range
     *
     * @param outer
     * @param relative
     * @param domain
     * @param addition
     * @return
     */
    public static <C extends Comparable<C>> Range<C> makeAbsolute(Range<C> outer, Range<C> relative, DiscreteDomain<C> domain, BiFunction<C, Long, C> addition) {
        long distance = domain.distance(outer.lowerEndpoint(), relative.lowerEndpoint());

        Range<C> shifted = RangeUtils.shift(relative, distance, domain, addition);
        Range<C> result = shifted.intersection(outer);
        return result;
    }


    public static <C extends Comparable<C>> Range<C> shift(Range<C> range, long distance, DiscreteDomain<C> domain) {
        BiFunction<C, Long, C> addition = (item, d) -> {
            C result = item;
            if(d >= 0) {
                for(int i = 0; i < d; ++i) {
                    result = domain.next(item);
                }
            } else {
                for(int i = 0; i < -d; ++i) {
                    result = domain.previous(item);
                }
            }
            return result;
        };

        Range<C> result = shift(range, distance, domain, addition);
        return result;
    }

    public static <C extends Comparable<C>> Range<C> shift(Range<C> rawRange, long distance, DiscreteDomain<C> domain, BiFunction<C, Long, C> addition) {

        Range<C> range = rawRange.canonical(domain);

        Range<C> result;
        if(range.hasLowerBound()) {
            C oldLower = range.lowerEndpoint();
            C newLower = addition.apply(oldLower, distance);

            if(range.hasUpperBound()) {
                C oldUpper = range.upperEndpoint();
                C newUpper = addition.apply(oldUpper, distance);
                result = Range.closedOpen(newLower, newUpper);
            } else {
                result = Range.atLeast(oldLower);
            }

        } else {
            throw new IllegalArgumentException("Cannot displace a range without lower bound");
        }

        return result;
    }

    public static <K extends Comparable<K>, V> Set<Entry<Range<K>, V>> getIntersectingRanges(Range<K> r, Collection<Entry<Range<K>, V>> ranges) {
        Set<Entry<Range<K>, V>> result = ranges.stream()
            .filter(e -> !r.intersection(e.getKey()).isEmpty())
            .collect(Collectors.toSet());

        return result;
    }

    //public static NavigableMap<T extends Comparable> getOverlapping items

    public static Range<Long> startFromZero(Range<Long> range) {
        Range<Long> result = range.intersection(rangeStartingWithZero);
        return result;
    }

    /**
     * Apply a binary operator (e.g. multiplication, addition, ...) to any endpoint of the range and a given value.
     *
     * @param <I>
     * @param range
     * @param value
     * @param op
     * @return
     */
    public static <I extends Comparable<I>, O extends Comparable<O>, V> Range<O> apply(
            Range<I> range,
            V value,
        BiFunction<? super I, ? super V, ? extends O> op)
    {
        Range<O> result = transform(range, endpoint -> op.apply(endpoint, value));

        return result;
    }

    /**
     * Return a new range with each concrete endpoint of the input range passed through a transformation function
     */
    public static <I extends Comparable<I>, O extends Comparable<O>> Range<O> transform(
            Range<I> range,
            Function<? super I, ? extends O> fn)
    {
        Range<O> result;

        if(range.hasLowerBound()) {
            if(range.hasUpperBound()) {
                result = Range.closedOpen(fn.apply(range.lowerEndpoint()), fn.apply(range.upperEndpoint()));
            } else {
                result = Range.atLeast(fn.apply(range.lowerEndpoint()));
            }
        } else {
            if(range.hasUpperBound()) {
                result = Range.lessThan(fn.apply(range.upperEndpoint()));
            } else {
                result = Range.all();
            }

        }

        return result;
    }


    public static Range<Long> multiplyByPageSize(Range<Long> range, long pageSize) {
        Range<Long> result = apply(range, pageSize, (endpoint, value) -> endpoint * value);
        return result;

// The following code was converted to the method apply(range, value, op)
//
//      Range<Long> result;
//
//      if(range.hasLowerBound()) {
//          if(range.hasUpperBound()) {
//              result = Range.closedOpen(range.lowerEndpoint() * pageSize, range.upperEndpoint() * pageSize);
//          } else {
//              result = Range.atLeast(range.lowerEndpoint() * pageSize);
//          }
//      } else {
//          if(range.hasUpperBound()) {
//              result = Range.lessThan(range.upperEndpoint() * pageSize);
//          } else {
//              result = Range.all();
//          }
//
//      }

    }

    public static PageInfo<Long> computeRange(Range<Long> range, long pageSize) {
        // Example: If pageSize=100 and offset = 130, then we will adjust the offset to 100, and use a subOffset of 30
        long o = range.hasLowerBound() ? range.lowerEndpoint() : 0;

        long subOffset = o % pageSize;
        o -= subOffset;

        // Adjust the limit to a page boundary; the original limit becomes the subLimit
        // And we will extend the new limit to the page boundary again.
        // Example: If pageSize=100 and limit = 130, then we adjust the new limit to 200

        Range<Long> outerRange;
        Range<Long> innerRange;
        if(range.hasUpperBound()) {
            long limit = range.upperEndpoint() - range.lowerEndpoint();
            long l = limit;


            long mod = l % pageSize;
            long extra = mod != 0 ? pageSize - mod : 0;
            l += extra;

            outerRange = Range.closedOpen(o, o + l);
            innerRange = Range.closedOpen(subOffset, limit);
        } else {
            outerRange = Range.atLeast(o);
            innerRange = Range.atLeast(subOffset);
        }

        PageInfo<Long> result = new PageInfo<>(outerRange, innerRange);

        return result;
    }

    /**
     * Compute the set of gaps for the given request range.
     * This is the complement of the given ranges constrained to the request range.
     */
    public static <C extends Comparable<C>> RangeSet<C> gaps(Range<C> request, RangeSet<C> ranges) {
        RangeSet<C> gaps = ranges.complement().subRangeSet(request);
        return gaps;
    }

}
