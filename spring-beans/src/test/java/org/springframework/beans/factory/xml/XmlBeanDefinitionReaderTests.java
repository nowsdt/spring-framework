/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import java.util.Arrays;

import org.junit.Test;
import org.xml.sax.InputSource;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class XmlBeanDefinitionReaderTests {

	@Test
	public void setParserClassSunnyDay() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		new XmlBeanDefinitionReader(registry).setDocumentReaderClass(DefaultBeanDefinitionDocumentReader.class);
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void withOpenInputStream() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
	}

	/**
	 * BeanDefinition 的解析过程已经全部完成了，下面做一个简要的总结：
	 *
	 * 解析 BeanDefinition 的入口在 DefaultBeanDefinitionDocumentReader 的#parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) 方法。该方法会根据命令空间来判断标签是默认标签还是自定义标签，其中：
	 *
	 * 默认标签，由 #parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) 方法来实现
	 * 自定义标签，由 BeanDefinitionParserDelegate 的 #parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) 方法来实现。
	 * 在默认标签解析中，会根据标签名称的不同进行 import、alias、bean、beans 四大标签进行处理。其中 bean 标签的解析为核心，它由 processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) 方法实现。
	 *
	 * processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) 方法，开始进入解析核心工作，分为三步：
	 *
	 * 解析默认标签的默认标签：BeanDefinitionParserDelegate#parseBeanDefinitionElement(Element ele, ...) 方法。该方法会依次解析 <bean> 标签的属性、各个子元素，解析完成后返回一个 GenericBeanDefinition 实例对象。
	 * 解析默认标签下的自定义标签：BeanDefinitionParserDelegate#decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder definitionHolder) 方法。
	 * 注册解析的 BeanDefinition：BeanDefinitionReaderUtils#registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) 方法
	 */
	@Test
	public void withOpenInputStreamAndExplicitValidationMode() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test
	public void withImport() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new ClassPathResource("import.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test
	public void withWildcardImport() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new ClassPathResource("importPattern.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void withInputSource() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
	}

	@Test
	public void withInputSourceAndExplicitValidationMode() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	@Test
	public void withFreshInputStream() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		Resource resource = new ClassPathResource("test.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	private void testBeanDefinitions(BeanDefinitionRegistry registry) {
		assertEquals(24, registry.getBeanDefinitionCount());
		assertEquals(24, registry.getBeanDefinitionNames().length);
		assertTrue(Arrays.asList(registry.getBeanDefinitionNames()).contains("rod"));
		assertTrue(Arrays.asList(registry.getBeanDefinitionNames()).contains("aliased"));
		assertTrue(registry.containsBeanDefinition("rod"));
		assertTrue(registry.containsBeanDefinition("aliased"));
		assertEquals(TestBean.class.getName(), registry.getBeanDefinition("rod").getBeanClassName());
		assertEquals(TestBean.class.getName(), registry.getBeanDefinition("aliased").getBeanClassName());
		assertTrue(registry.isAlias("youralias"));
		String[] aliases = registry.getAliases("aliased");
		assertEquals(2, aliases.length);
		assertTrue(ObjectUtils.containsElement(aliases, "myalias"));
		assertTrue(ObjectUtils.containsElement(aliases, "youralias"));
	}

	@Test
	public void dtdValidationAutodetect() {
		doTestValidation("validateWithDtd.xml");
	}

	@Test
	public void xsdValidationAutodetect() {
		doTestValidation("validateWithXsd.xml");
	}

	private void doTestValidation(String resourceName) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		Resource resource = new ClassPathResource(resourceName, getClass());
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(resource);
		TestBean bean = (TestBean) factory.getBean("testBean");
		assertNotNull(bean);
	}

}
