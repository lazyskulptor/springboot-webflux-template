package my.lazyskulptor.adapter.support;

import org.springframework.data.repository.core.support.AbstractEntityInformation;

import javax.persistence.metamodel.Metamodel;

class HrsaEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {
    final private Metamodel metamodel;

    public HrsaEntityInformation(Class<T> domainClass, Metamodel metamodel) {
        super(domainClass);
        this.metamodel = metamodel;
    }

    @Override
    public ID getId(T entity) {
        return (ID) metamodel.entity(this.getJavaType()).getId(this.getIdType());
    }

    @Override
    public Class<ID> getIdType() {
        return (Class<ID>) metamodel.entity(this.getJavaType()).getIdType().getJavaType();
    }
}
