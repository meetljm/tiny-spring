package us.codecraft.tinyioc.beans.factory;

import us.codecraft.tinyioc.beans.AutoWired;
import us.codecraft.tinyioc.beans.BeanDefinition;
import us.codecraft.tinyioc.beans.BeanPostProcessor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yihua.huang@dianping.com
 */
public abstract class AbstractBeanFactory implements BeanFactory {

    private final List<String> beanDefinitionNames = new ArrayList<String>();
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

    @Override
    public Object getBean(String name) throws Exception {
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        if (beanDefinition == null) {
            throw new IllegalArgumentException("No bean named " + name + " is defined");
        }
        Object bean = beanDefinition.getBean();
        if (bean == null) {
            bean = doCreateBean(beanDefinition);
            bean = initializeBean(bean, name);
            beanDefinition.setBean(bean);
        }
        return bean;
    }

    protected Object initializeBean(Object bean, String name) throws Exception {
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            //初始化之前
            bean = beanPostProcessor.postProcessBeforeInitialization(bean, name);
        }

        // TODO:call initialize method
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            //初始化之后  对bean进行增强 原始的bean 被代理bean 的引用持有
            Object preBean = bean;
            bean = beanPostProcessor.postProcessAfterInitialization(bean, name);
            //断言 增强后的 bean 是原始bean的父类
            assert preBean.getClass().isAssignableFrom(bean.getClass());
        }
        return bean;
    }

    protected Object createBeanInstance(BeanDefinition beanDefinition) throws Exception {
        Constructor[] constructors = beanDefinition.getBeanClass().getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i].isAnnotationPresent(AutoWired.class)) {
                List objs = new ArrayList();
                for (Class clazz : constructors[i].getParameterTypes()) {
                    List beansForType = getBeansForType(clazz);
                    if (beansForType.size() == 1) {
                        objs.add(beansForType.get(0));
                    }
                }
                if (objs.size() == constructors[i].getParameterCount()) {
                    return constructors[i].newInstance(objs.toArray());
                }
            }
        }
        return beanDefinition.getBeanClass().newInstance();
    }

    public void registerBeanDefinition(String name, BeanDefinition beanDefinition) throws Exception {
        beanDefinitionMap.put(name, beanDefinition);
        beanDefinitionNames.add(name);
    }

    public void preInstantiateSingletons() throws Exception {
        for (Iterator it = this.beanDefinitionNames.iterator(); it.hasNext(); ) {
            String beanName = (String) it.next();
            getBean(beanName);
        }
    }

    protected Object doCreateBean(BeanDefinition beanDefinition) throws Exception {
        Object bean = createBeanInstance(beanDefinition);
        beanDefinition.setBean(bean);
        applyPropertyValues(bean, beanDefinition);
        return bean;
    }

    protected void applyPropertyValues(Object bean, BeanDefinition beanDefinition) throws Exception {

    }

    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) throws Exception {
        this.beanPostProcessors.add(beanPostProcessor);
    }

    // 所有的bean都是从beanDefinition中取出来的,
    public List getBeansForType(Class type) throws Exception {
        List beans = new ArrayList<Object>();
        for (String beanDefinitionName : beanDefinitionNames) {
            if (type.isAssignableFrom(beanDefinitionMap.get(beanDefinitionName).getBeanClass())) {
                beans.add(getBean(beanDefinitionName));
            }
        }
        return beans;
    }

}
