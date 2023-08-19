package my.lazyskulptor.adapter.repository;

import my.lazyskulptor.adapter.annotation.EnableAdapterRepositories;
import my.lazyskulptor.adapter.config.ConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

public class RepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {
    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableAdapterRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new ConfigurationExtensionSupport();
    }
}
