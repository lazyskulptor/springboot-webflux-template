package my.lazyskulptor.adapter;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ImportAutoConfiguration(value = AdapterAutoConfiguration.class)
public @interface EnableHibernateReactiveSpringAdapter {
}
