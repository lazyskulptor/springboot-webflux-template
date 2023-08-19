package my.lazyskulptor.adapter;

import org.springframework.data.repository.core.support.AbstractEntityInformation;

import javax.persistence.metamodel.Metamodel;

public class AdapterEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {
    final private Metamodel metamodel;

    public AdapterEntityInformation(Class<T> domainClass, Metamodel metamodel) {
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
