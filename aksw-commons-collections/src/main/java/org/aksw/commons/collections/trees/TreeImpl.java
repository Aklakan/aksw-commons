package org.aksw.commons.collections.trees;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TreeImpl<T>
    extends TreeBase<T>
{
    protected T root;
    protected Function<T, List<T>> parentToChildren;
    protected Function<T, T> childToParent;
    protected BiFunction<T, List<T>, T> copyNode;


    @Override
    public Tree<T> createNew(T root) {
        Tree<T> result = create(root, parentToChildren);
        return result;
    }


    //

//    public TreeImpl(T root, TreeOps<T> ops) {
//        super();
//        this.root = root;
//        this.parentToChild = parentToChildren;
//        this.childToParent = childToParent;
//        this.copyNode = copyNode;
//    }

    public TreeImpl(T root, Function<T, List<T>> parentToChildren, Function<T, T> childToParent, BiFunction<T, List<T>, T> copyNode) {
        super();
        this.root = root;
        this.parentToChildren = parentToChildren;
        this.childToParent = childToParent;
        this.copyNode = copyNode;
    }

    @Override
    public T getRoot() {
        return root;
    }

    @Override
    public List<T> getChildren(T node) {
        // TODO We could by default treat null as a super-root node whose only child is the root node.
        // It would also be consistent in the sense that the parent of the root would be null and its child would be the root
        List<T> result = node == null
                ? (root != null ? Collections.emptyList() : parentToChildren.apply(node))
                : parentToChildren.apply(node);

//        List<T> result = parentToChild.apply(node);
        return result;
    }

    @Override
    public T getParent(T node) {
        T result = childToParent.apply(node);
        return result;
    }

    public static <T> TreeImpl<T> create(T root, Function<T, List<T>> parentToChildren) {
        Map<T, T> childToParent = TreeUtils.parentMap(root, parentToChildren);

        TreeImpl<T> result = new TreeImpl<>(root, parentToChildren, (node) -> childToParent.get(node), (a, b) -> { throw new UnsupportedOperationException(); });
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((childToParent == null) ? 0 : childToParent.hashCode());
        result = prime * result
                + ((parentToChildren == null) ? 0 : parentToChildren.hashCode());
        result = prime * result + ((root == null) ? 0 : root.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TreeImpl<?> other = (TreeImpl<?>) obj;
        if (childToParent == null) {
            if (other.childToParent != null)
                return false;
        } else if (!childToParent.equals(other.childToParent))
            return false;
        if (parentToChildren == null) {
            if (other.parentToChildren != null)
                return false;
        } else if (!parentToChildren.equals(other.parentToChildren))
            return false;
        if (root == null) {
            if (other.root != null)
                return false;
        } else if (!root.equals(other.root))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TreeImpl [root=" + root + ", parentToChild=" + parentToChildren
                + ", childToParent=" + childToParent + "]";
    }

	@Override
	public T copy(T node, List<T> children) {
		T result = copyNode.apply(node, children);
		return result;
	}
}
