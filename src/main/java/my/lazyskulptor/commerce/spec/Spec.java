package my.lazyskulptor.commerce.spec;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Spec<T> {

    default Spec<T> and(Spec<T>... substitutes) {
        return new AndSpec<>(this, substitutes);
    }
    default Spec<T> or(Spec<T>... substitutes) {
        return new OrSpec<>(this, substitutes);
    }
    default Spec<T> not() {
        return Spec.ne(this);
    }

    /**
     * ne, not equals
     * @param o original Spec
     * @return Spec wrapped with NotSpec
     * @param <S> Domain Model
     */
    static <S> Spec<S> ne(Spec<S> o) {
        return new NotSpec<>(o);
    }

    boolean isSatisfiedBy(T o);
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);

    private static <S> Function<Spec<S>[], Predicate[]> merge(Root<S> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        return specs -> Stream.of(specs)
                .map(r -> r.toPredicate(root, query, criteriaBuilder))
                .collect(Collectors.toList())
                .toArray(new Predicate[]{});
    }
    static <S> Spec<S> not(Spec<S> o) {
        return new NotSpec<>(o);
    }
    class AndSpec<S> implements Spec<S> {
        private final Spec<S> left;
        private final Spec<S>[] right;

        private AndSpec(Spec<S> left, Spec<S>... right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isSatisfiedBy(S o) {
            return left.isSatisfiedBy(o) && Stream.of(right).allMatch(r -> r.isSatisfiedBy(o));
        }

        @Override
        public Predicate toPredicate(Root<S> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            Predicate predicates = criteriaBuilder.and(Spec.merge(root, query, criteriaBuilder).apply(right));
            return criteriaBuilder.and(left.toPredicate(root, query, criteriaBuilder), predicates);
        }
    }
    class OrSpec<S> implements Spec<S> {
        private final  Spec<S> left;
        private final  Spec<S>[] right;

        private OrSpec(Spec<S> left, Spec<S>... right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isSatisfiedBy(S o) {
            return left.isSatisfiedBy(o) || Stream.of(right).anyMatch(r -> r.isSatisfiedBy(o));
        }

        @Override
        public Predicate toPredicate(Root<S> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            Predicate predicates = criteriaBuilder.or(Spec.merge(root, query, criteriaBuilder).apply(right));
            return criteriaBuilder.or(left.toPredicate(root, query, criteriaBuilder), predicates);
        }
    }
    class NotSpec<S> implements Spec<S> {
        private final  Spec<S> original;
        private NotSpec(Spec<S> original) {
            this.original = original;
        }

        @Override
        public boolean isSatisfiedBy(S o) {
            return !original.isSatisfiedBy(o);
        }

        @Override
        public Predicate toPredicate(Root<S> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            return criteriaBuilder.not(original.toPredicate(root, query, criteriaBuilder));
        }
    }
}
