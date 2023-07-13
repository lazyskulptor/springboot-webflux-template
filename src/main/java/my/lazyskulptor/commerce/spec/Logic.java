package my.lazyskulptor.commerce.spec;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public enum Logic implements Spec<Object> {
    TRUE(true), FALSE(false);

    private final boolean value;
    Logic(boolean value) {
        this.value = value;
    }

    @Override
    public boolean isSatisfiedBy(Object o) {
        return this.value;
    }

    @Override
    public Predicate toPredicate(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (this.value) {
            return criteriaBuilder.equal(criteriaBuilder.literal(1), criteriaBuilder.literal(1));
        }
        return criteriaBuilder.equal(criteriaBuilder.literal(1), criteriaBuilder.literal(2));
    }
}
