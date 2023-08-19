package my.lazyskulptor.adapter.config;

import my.lazyskulptor.adapter.AdapterRepository;
import my.lazyskulptor.adapter.support.RepositoryFactoryBean;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.core.RepositoryMetadata;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class ConfigurationExtensionSupport extends RepositoryConfigurationExtensionSupport {
    @Override
    public String getModuleName() {
        return "HibernateReactiveAdapter";
    }

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return RepositoryFactoryBean.class.getName();
    }

    @Override
    protected String getModulePrefix() {
        return getModuleName().toLowerCase(Locale.US);
    }

    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Arrays.asList(Entity.class, MappedSuperclass.class);
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.<Class<?>> singleton(AdapterRepository.class);
    }


    @Override
    protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
        return metadata.isReactiveRepository();
    }
}
