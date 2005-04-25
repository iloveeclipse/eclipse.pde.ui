/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.ClasspathUtil;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

public class PluginImportOperation extends JarImportOperation {

	public static final int IMPORT_BINARY = 1;

	public static final int IMPORT_BINARY_WITH_LINKS = 2;

	public static final int IMPORT_WITH_SOURCE = 3;

	private IPluginModelBase[] fModels;

	private int fImportType;

	private IReplaceQuery fReplaceQuery;

	private Hashtable fProjectClasspaths = new Hashtable();

	public interface IReplaceQuery {
		public static final int CANCEL = 0;

		public static final int NO = 1;

		public static final int YES = 2;

		int doQuery(IProject project);
	}

	public PluginImportOperation(IPluginModelBase[] models, int importType, IReplaceQuery replaceQuery) {
		fModels = models;
		fImportType = importType;
		fReplaceQuery = replaceQuery;
	}

	/*
	 * @see IWorkspaceRunnable#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_creating, fModels.length);
		try {
			if (PDEPlugin.getWorkspace().isAutoBuilding()) {
				IWorkspaceDescription desc = PDEPlugin.getWorkspace().getDescription();
				desc.setAutoBuilding(false);
				PDEPlugin.getWorkspace().setDescription(desc);
			}
			
			MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(),
					IStatus.OK,
					PDEUIMessages.ImportWizard_operation_multiProblem, 
					null);

			for (int i = 0; i < fModels.length; i++) {
				try {
					importPlugin(fModels[i], new SubProgressMonitor(monitor, 1));
				} catch (CoreException e) {
					multiStatus.merge(e.getStatus());
				}
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
			}
			if (!multiStatus.isOK()) {
				throw new CoreException(multiStatus);
			}
		} finally {
			Enumeration keys = fProjectClasspaths.keys();
			while (keys.hasMoreElements()) {
				IProject project = (IProject)keys.nextElement();
				IClasspathEntry[] classpath = (IClasspathEntry[])fProjectClasspaths.get(project);
				JavaCore.create(project).setRawClasspath(classpath, null);
			}
			monitor.done();
		}
	}

	private void importPlugin(IPluginModelBase model, IProgressMonitor monitor)
			throws CoreException {
		String id = model.getPluginBase().getId();
		monitor.beginTask(NLS.bind(PDEUIMessages.ImportWizard_operation_creating2, id), 6);
		try {
			IProject project = findProject(model.getPluginBase().getId());

			if (project.exists()) {
				if (!queryReplace(project))
					return;
				if (RepositoryProvider.isShared(project))
					RepositoryProvider.unmap(project);
				project.delete(true, true, monitor);
			}

			project.create(monitor);
			if (!project.isOpen())
				project.open(monitor);			
			monitor.worked(1);

			switch (fImportType) {
				case IMPORT_BINARY:
					importAsBinary(project, model, true, new SubProgressMonitor(monitor, 4));
					break;
				case IMPORT_BINARY_WITH_LINKS:
					importAsBinaryWithLinks(project, model, new SubProgressMonitor(monitor, 4));
					break;
				case IMPORT_WITH_SOURCE:
					if (isExempt(model)) {
						importAsBinary(project, model, true, new SubProgressMonitor(monitor, 4));
					} else {
						importAsSource(project, model, new SubProgressMonitor(monitor, 4));
					}
			}

			setProjectDescription(project, model);

			if (project.hasNature(JavaCore.NATURE_ID) && project.findMember(".classpath") == null) //$NON-NLS-1$
				fProjectClasspaths .put(project, ClasspathUtil.getClasspath(project, model, true));
		} catch (CoreException e) {
		} finally {
			monitor.done();
		}
	}
	
	private void importAsBinaryWithLinks(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		if (isJARd(model)) {
			extractJARdPlugin(
					project,
					model,
					monitor);
		} else {
			File[] items = new File(model.getInstallLocation()).listFiles();
			if (items != null) {
				monitor.beginTask(PDEUIMessages.PluginImportOperation_linking, items.length + 1); //$NON-NLS-1$
				for (int i = 0; i < items.length; i++) {
					File sourceFile = items[i];
					String name = sourceFile.getName();
					if (sourceFile.isDirectory()) {
						project.getFolder(name).createLink(
							new Path(sourceFile.getPath()),
							IResource.NONE,
							new SubProgressMonitor(monitor, 1));
					} else {
						if (!name.equals(".project")) { //$NON-NLS-1$ //$NON-NLS-2$
							project.getFile(name).createLink(
								new Path(sourceFile.getPath()),
								IResource.NONE,
								new SubProgressMonitor(monitor, 1));
						}
					}
				}
			}
			linkSourceArchives(project, model, new SubProgressMonitor(monitor, 1));
		}
		try {
			RepositoryProvider.map(project, PDECore.BINARY_REPOSITORY_PROVIDER);
		} catch (TeamException e) {
		}
	}

	private void importAsBinary(IProject project, IPluginModelBase model, boolean markAsBinary, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 2); //$NON-NLS-1$
		if (isJARd(model)) {
			extractJARdPlugin(
					project,
					model,
					new SubProgressMonitor(monitor, 1));
		} else {
			importContent(
					new File(model.getInstallLocation()),
					project.getFullPath(),
					FileSystemStructureProvider.INSTANCE,
					null,
					new SubProgressMonitor(monitor, 1));
			importSourceArchives(
					project,
					model,
					new SubProgressMonitor(monitor, 1));			
		}
		
		if (markAsBinary)
			project.setPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY,
					PDECore.BINARY_PROJECT_VALUE);		
	}

	private void importAsSource(IProject project, IPluginModelBase model, SubProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 3); //$NON-NLS-1$
		importAsBinary(project, model, false, new SubProgressMonitor(monitor, 2));
		
		String[] libraries = getLibraryNames(model, false);
		for (int i = 0; i < libraries.length; i++) {
			if (ClasspathUtilCore.containsVariables(libraries[i]))
				continue;
			String name = libraries[i];
			if (name.equals(".") && isJARd(model)) {
				if (project.getFolder("src").exists()) {
					continue;
				} 
				name = new File(model.getInstallLocation()).getName();			
			}
			name = ClasspathUtilCore.expandLibraryName(name);
			IPath libraryPath = new Path(name);
			IResource jarFile = project.findMember(libraryPath);
			if (jarFile != null) {
				IResource srcZip = jarFile.getProject().findMember(ClasspathUtilCore.getSourceZipName(name));
				if (srcZip != null) {
					String jarName = libraries[i].equals(".") ? "" : libraryPath.removeFileExtension().lastSegment();
					IFolder dest = jarFile.getProject().getFolder("src" + (jarName.length() == 0 ? "" : "-" + jarName)); //$NON-NLS-1$
					if (!dest.exists()) {
						dest.create(true, true, null);
					}
					extractZipFile(srcZip.getLocation().toFile(), dest.getFullPath(), monitor);
					if (isJARd(model)) {
						extractJavaResources(jarFile.getLocation().toFile(), dest, monitor);
					} else {
						extractResources(jarFile.getLocation().toFile(), dest, monitor);
					}
					srcZip.delete(true, null);
					jarFile.delete(true, null);
				}
			}
		}			
	}

	private void linkSourceArchives(IProject project, IPluginModelBase model,
			IProgressMonitor monitor) throws CoreException {

		String[] libraries = getLibraryNames(model, true);
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_copyingSource,
				libraries.length);

		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		for (int i = 0; i < libraries.length; i++) {
			String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
			IPath path = new Path(zipName);
			if (project.findMember(path) == null) {
				IPath srcPath = manager.findSourcePath(model.getPluginBase(), path);
				if (srcPath != null) {
					if ("src.zip".equals(zipName) && isJARd(model)) {
						path = new Path(ClasspathUtilCore.getSourceZipName(new File(model
								.getInstallLocation()).getName()));
					}
					project.getFile(path).createLink(
							srcPath,
							IResource.NONE,
							new SubProgressMonitor(monitor, 1));
				}
			}
			monitor.worked(1);
		}
		monitor.done();
	}

	private void importSourceArchives(IProject project, IPluginModelBase model, IProgressMonitor monitor)
			throws CoreException {
		
		String[] libraries = getLibraryNames(model, true);
		monitor.beginTask(PDEUIMessages.ImportWizard_operation_copyingSource, libraries.length);

		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		for (int i = 0; i < libraries.length; i++) {
			String zipName = ClasspathUtilCore.getSourceZipName(libraries[i]);
			IPath path = new Path(zipName);
			if (project.findMember(path) == null) {
				IPath srcPath = manager.findSourcePath(model.getPluginBase(), path);
				if (srcPath != null) {
					if ("src.zip".equals(zipName) && isJARd(model)) {
						path = new Path(ClasspathUtilCore.getSourceZipName(new File(model.getInstallLocation()).getName()));
					}
					importArchive(project, new File(srcPath.toOSString()), path);						
				}
			}
			monitor.worked(1);
		}
		monitor.done();
	}
	
	private String[] getLibraryNames(IPluginModelBase model, boolean expand) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		ArrayList list = new ArrayList();
		for (int i = 0; i < libraries.length; i++) {
			if (expand)
				list.add(ClasspathUtilCore.expandLibraryName(libraries[i].getName()));
			else
				list.add(libraries[i].getName());
		}
		if (libraries.length == 0 && isJARd(model))
			list.add(".");
		return (String[])list.toArray(new String[list.size()]);
	}
	
	private void extractJARdPlugin(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(model.getInstallLocation());
			ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
			ArrayList collected = new ArrayList();
			collectNonJavaResources(provider, provider.getRoot(), collected);
			importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);

			File file = new File(model.getInstallLocation());
			if (hasEmbeddedSource(provider) && fImportType == IMPORT_WITH_SOURCE) {
				collected = new ArrayList();
				collectJavaFiles(provider, provider.getRoot(), collected);
				importContent(provider.getRoot(), project.getFullPath(), provider, collected, monitor);
				collected = new ArrayList();
				collectJavaResources(provider, provider.getRoot(), collected);
				importContent(provider.getRoot(), project.getFullPath().append("src"), provider, collected, monitor);
			} else {
				if (fImportType == IMPORT_BINARY_WITH_LINKS) {
					 project.getFile(file.getName()).createLink(
						new Path(file.getAbsolutePath()),
						IResource.NONE,
					 	null);
				} else {
					importArchive(project, file, new Path(file.getName()));				
				}
				if (!hasEmbeddedSource(provider)) {
					if (fImportType == IMPORT_BINARY_WITH_LINKS) {
						linkSourceArchives(
								project, 
								model, 
								new SubProgressMonitor(monitor, 1));
					} else {
						importSourceArchives(
								project,
								model,
								new SubProgressMonitor(monitor, 1));
					}
				}
			}
			setPermissions(model, project);
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.ERROR, e.getMessage(), e);
			throw new CoreException(status);
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private IProject findProject(String id) {
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getWorkspaceModel();
			if (model != null)
				return model.getUnderlyingResource().getProject();
		}
		return PDEPlugin.getWorkspace().getRoot().getProject(id);
	}

	private boolean queryReplace(IProject project) throws OperationCanceledException {
		switch (fReplaceQuery.doQuery(project)) {
			case IReplaceQuery.CANCEL:
				throw new OperationCanceledException();
			case IReplaceQuery.NO:
				return false;
		}
		return true;
	}
	
	private void setProjectDescription(IProject project, IPluginModelBase model)
			throws CoreException {
		IProjectDescription desc = project.getDescription();
		if (needsJavaNature(project, model))
			desc.setNatureIds(new String[] { JavaCore.NATURE_ID, PDE.PLUGIN_NATURE });
		else
			desc.setNatureIds(new String[] { PDE.PLUGIN_NATURE });
		project.setDescription(desc, null);
	}

	private boolean needsJavaNature(IProject project, IPluginModelBase model) {
		return true;
	}
	
	private boolean isExempt(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		if ("org.apache.ant".equals(id)
			|| "org.eclipse.osgi.util".equals(id)
			|| "org.eclipse.osgi.services".equals(id))
			return true;
		
		if ("org.eclipse.swt".equals(id) && !isJARd(model))
			return true;
		return false;
	}

	private boolean isJARd(IPluginModelBase model) {
		return new File(model.getInstallLocation()).isFile();
	}
	
	private boolean hasEmbeddedSource(ZipFileStructureProvider provider) {
		List children = provider.getChildren(provider.getRoot());
		if (children != null && !children.isEmpty()) {
			for (int i = 0; i < children.size(); i++) {
				Object curr = children.get(i);
				if (provider.isFolder(curr) && provider.getLabel(curr).equals("src")) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void setPermissions(IPluginModelBase model, IProject project) {
		try {
			if (!Platform.getOS().equals(Constants.OS_WIN32) && model instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel)model).getFragment();
				if ("org.eclipse.swt".equals(fragment.getPluginId())) {
					IResource[] children = project.members();
					for (int i = 0; i < children.length; i++) {
						if (children[i] instanceof IFile && isInterestingResource(children[i].getName())) {
							Runtime.getRuntime().exec(new String[] {"chmod", "755", children[i].getLocation().toOSString()}).waitFor();						
						}
					}
				}			
			}
		} catch (CoreException e) {
		} catch (InterruptedException e) {
		} catch (IOException e) {
		}
	}
	
	private boolean isInterestingResource(String name) {
		return name.endsWith(".jnilib")
		|| name.endsWith(".sl")
		|| name.endsWith(".a")
		|| name.indexOf(".so") != -1;
	}

	
}
