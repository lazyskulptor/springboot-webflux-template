package my.lazyskulptor.adapter.repository;

import my.lazyskulptor.adapter.annotation.EnableHrsaRepositories;
import my.lazyskulptor.adapter.support.ConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

public class HrsaRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {
    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableHrsaRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new ConfigurationExtensionSupport();
    }
}
