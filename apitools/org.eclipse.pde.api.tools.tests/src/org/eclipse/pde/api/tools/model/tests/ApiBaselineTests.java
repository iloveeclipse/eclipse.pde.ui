/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.BundleVersionRange;
import org.eclipse.pde.api.tools.internal.RequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Test creation of states and components.
 * 
 * @since 1.0.0
 */
public class ApiBaselineTests extends TestCase {
	
	static final String _1_0_0 = "1.0.0";
	static final String COMPONENT_B = "component.b";
	static final String COMPONENT_A = "component.a";
	static final String TEST_PLUGINS = "test-plugins";

	IApiBaseline fBaseline = null;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	public void setUp() throws Exception {
		if(fBaseline == null) {
			fBaseline = TestSuiteHelper.createTestingBaseline(TEST_PLUGINS);
			assertNotNull("the testing baseline should exist", fBaseline);
			List<IRequiredComponentDescription> reqs = new ArrayList<IRequiredComponentDescription>();
			reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange(Util.EMPTY_STRING)));
			validateComponent(fBaseline, COMPONENT_A, "A Plug-in", _1_0_0, "J2SE-1.5", reqs);
	
			reqs = new ArrayList<IRequiredComponentDescription>();
			reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange(Util.EMPTY_STRING)));
			reqs.add(new RequiredComponentDescription(COMPONENT_A, new BundleVersionRange(Util.EMPTY_STRING)));
			validateComponent(fBaseline, COMPONENT_B, "B Plug-in", _1_0_0, "J2SE-1.4", reqs);
		}
	}
	
	/**
	 * Resolves a package
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolvePackage() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline);
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), COMPONENT_A);
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(COMPONENT_A), components[0]);
	}
	
	/**
	 * Resolves a package within a single component
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolvePackageWithinComponent() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline);
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_A), "a.b.c");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(COMPONENT_A), components[0]);
	}	
	
	/**
	 * Resolves a system package
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolveJavaLangPackage() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline);
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), "java.lang");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(fBaseline.getExecutionEnvironment()), components[0]);
	}
	
	/**
	 * Resolves a system package
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolveSystemPackage() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline);
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), "org.w3c.dom");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(fBaseline.getExecutionEnvironment()), components[0]);
	}	
	
	/**
	 * Finds the class file for java.lang.Object
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testFindJavaLangObject() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline);
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), "java.lang");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(fBaseline.getExecutionEnvironment()), components[0]);
		IApiTypeRoot classFile = components[0].findTypeRoot("java.lang.Object");
		assertNotNull("Missing java.lang.Object", classFile);
		assertEquals("Wrong type name", "java.lang.Object", classFile.getTypeName());
	}	
	
	/**
	 * Validates basic component attributes.
	 * 
	 * @param baseline baseline to retrieve the component from
	 * @param id component id
	 * @param name component name
	 * @param version component version
	 * @param environment execution environment id
	 * @param requiredComponents list of {@link IRequiredComponentDescription}
	 * @throws CoreException 
	 */
	private void validateComponent(IApiBaseline baseline, String id, String name, String version, String environment, List<IRequiredComponentDescription> requiredComponents) throws CoreException {
		IApiComponent component = baseline.getApiComponent(id);
		
		assertEquals("Id: ", id , component.getSymbolicName());
		assertEquals("Name: ", name , component.getName());
		assertEquals("Version: ", version , component.getVersion());
		String[] envs = component.getExecutionEnvironments();
		assertEquals("Wrong number of execution environments", 1, envs.length);
		assertEquals("Version: ", environment , envs[0]);
		
		IRequiredComponentDescription[] actual = component.getRequiredComponents();
		assertEquals("Wrong number of required components", requiredComponents.size(), actual.length);
				
		for (int i = 0; i < requiredComponents.size(); i++) {
			assertEquals("Wrong required component", requiredComponents.get(i), actual[i]);
		}		
	}
	
	/**
	 * Tests creating a baseline with a component that has a nested jar 
	 * of class files.
	 * 
	 * @throws CoreException
	 */
	public void testNestedJarComponent() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-nested-jars");
		IApiComponent component = baseline.getApiComponent(COMPONENT_A);
		assertNotNull("missing component.a", component);
		IApiTypeContainer[] containers = component.getApiTypeContainers();
		assertTrue("Missing containers:", containers.length > 0);
		IApiTypeRoot file = null;
		for (int i = 0; i < containers.length; i++) {
			IApiTypeContainer container = containers[i];
			String[] names = container.getPackageNames();
			for (int j = 0; j < names.length; j++) {
				if (names[j].equals(COMPONENT_A)) {
					file = container.findTypeRoot("component.a.A");
					break;
				}
			}
			if (file != null) {
				break;
			}
		}
		assertNotNull("Missing class file", file);
		assertEquals("Wrong type name", "component.a.A", file.getTypeName());
	}
	
	/**
	 * Tests that an x-friends directive works. Component A exports package
	 * component.a.friend.of.b as a friend for b. Note - the package should
	 * still be private.
	 * 
	 * @throws CoreException 
	 */
	public void testXFriendsDirective() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.friend.of.b.FriendOfB"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be PRIVATE", VisibilityModifiers.isPrivate(visibility));
	}
	
	/**
	 * Tests that an x-internal directive works. Component A exports package
	 * component.a.internal as internal.
	 * 
	 * @throws CoreException 
	 */
	public void testXInternalDirective() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.internal.InternalClass"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be private", VisibilityModifiers.isPrivate(visibility));
	}	
	
	/**
	 * Tests that a 'uses' directive works. Component A exports package
	 * component.a. with a 'uses' directive but should still be API.
	 * 
	 * @throws CoreException 
	 */
	public void testUsesDirective() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.A"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be API", VisibilityModifiers.isAPI(visibility));
	}	
	
	/**
	 * Tests that a non-exported package is private. Component A does not export package
	 * component.a.not.exported.
	 * 
	 * @throws CoreException 
	 */
	public void testNotExported() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.not.exported.NotExported"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be private", VisibilityModifiers.isPrivate(visibility));
	}		
	
	/**
	 * Tests component prerequisites.
	 *  
	 * @throws CoreException
	 */
	public void testPrerequisites() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		IApiComponent[] prerequisiteComponents = fBaseline.getPrerequisiteComponents(new IApiComponent[]{component});
		for (int i = 0; i < prerequisiteComponents.length; i++) {
			IApiComponent apiComponent = prerequisiteComponents[i];
			if (apiComponent.getSymbolicName().equals("org.eclipse.osgi")) {
				// done
				return;
			}
		}
		assertTrue("Missing prerequisite bundle", false);
	}
	
	/**
	 * Tests component dependents.
	 *  
	 * @throws CoreException
	 */
	public void testDependents() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		IApiComponent[] dependents = fBaseline.getDependentComponents(new IApiComponent[]{component});
		assertEquals("Wrong number of dependents", 2, dependents.length);
		for (int i = 0; i < dependents.length; i++) {
			IApiComponent apiComponent = dependents[i];
			if (apiComponent.getSymbolicName().equals(COMPONENT_B)) {
				// done
				return;
			}
		}
		assertEquals("Missing dependent component.b", false);
	}	
	
	/**
	 * Tests getting the location from an 'old' baseline
	 */
	public void testGetLocation() throws Exception {
		assertNull("The location must be null", fBaseline.getLocation());
		fBaseline.setLocation("new_loc");
		assertNotNull("The location must not be null", fBaseline.getLocation());
	}
}
