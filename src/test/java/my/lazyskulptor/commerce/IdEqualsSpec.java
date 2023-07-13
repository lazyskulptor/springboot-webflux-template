package my.lazyskulptor.commerce;

import my.lazyskulptor.commerce.model.Account;
import my.lazyskulptor.commerce.spec.Spec;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Objects;

public class IdEqualsSpec implements Spec<Account> {

    private final Long id;

    public IdEqualsSpec(Long id) {
        this.id = id;
    }

    @Override
    public boolean isSatisfiedBy(Account o) {
        return Objects.equals(this.id, o.getId());
    }

    @Override
    public Predicate toPredicate(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        return criteriaBuilder.equal(root.get("id"), this.id);
    }
}
