/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;

/**
 * SimpleCSObject
 *
 */
public abstract class SimpleCSObject extends PlatformObject implements ISimpleCSObject {

	// TODO: MP: TEO: MED: Delete all old simple cs model objects
	// TODO: MP: TEO: MED: Delete all old simple cs model package references in MANIFEST.MF - check plugin.xml too
	
	private transient ISimpleCSModel fModel;
	
	private transient ISimpleCSObject fParent;

	protected static final HashSet TAG_EXCEPTIONS = new HashSet(3);
	
	protected static final HashMap SUBSTITUTE_CHARS = new HashMap(5);

	
	static {
		TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("br/"); //$NON-NLS-1$
		
		SUBSTITUTE_CHARS.put(new Character('&'), "&amp;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('<'), "&lt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('>'), "&gt;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\''), "&apos;"); //$NON-NLS-1$
		SUBSTITUTE_CHARS.put(new Character('\"'), "&quot;"); //$NON-NLS-1$
	}
	
	/**
	 * 
	 */
	public SimpleCSObject(ISimpleCSModel model, ISimpleCSObject parent) {
		fModel = model;
		fParent = parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getModel()
	 */
	public ISimpleCSModel getModel() {
		return fModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getSimpleCS()
	 */
	public ISimpleCS getSimpleCS() {
		return fModel.getSimpleCS();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#setModel(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel)
	 */
	public void setModel(ISimpleCSModel model) {
		fModel = model;
	}

	/**
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChanged(String property, Object oldValue,
			Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
		
	/**
	 * @param object
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	private void firePropertyChanged(ISimpleCSObject object, String property,
		Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
		
	/**
	 * @param child
	 * @param changeType
	 */
	protected void fireStructureChanged(ISimpleCSObject child, int changeType) {
		fireStructureChanged(new ISimpleCSObject[] { child }, changeType);
	}
	
	/**
	 * @param newValue
	 * @param oldValue
	 * @param changeType
	 */
	protected void fireStructureChanged(ISimpleCSObject newValue,
			ISimpleCSObject oldValue) {

		int changeType = -1;
		ISimpleCSObject object = null;
		if (newValue == null) {
			changeType = IModelChangedEvent.REMOVE;
			object = oldValue;
		} else {
			changeType = IModelChangedEvent.INSERT;
			object = newValue;
		}
		fireStructureChanged(object, changeType);
	}
	
	/**
	 * @param children
	 * @param changeType
	 */
	private void fireStructureChanged(ISimpleCSObject[] children,
			int changeType) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider,
					changeType, children, null));
		}
	}
		
	/**
	 * @return
	 */
	public boolean isEditable() {
		return fModel.isEditable();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getParent()
	 */
	public ISimpleCSObject getParent() {
		return fParent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public abstract int getType();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getName()
	 */
	public abstract String getName();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getChildren()
	 */
	public abstract List getChildren();

	
	public IModel getSharedModel() {
		return null;
	}

	public boolean isInTheModel() {
		return false;
	}

	public void setInTheModel(boolean inModel) {
	}

	public void setSharedModel(IModel model) {
	}

	public void addChildNode(IDocumentNode child) {
	}

	public void addChildNode(IDocumentNode child, int position) {
	}

	public void addTextNode(IDocumentTextNode textNode) {
	}

	public IDocumentNode getChildAt(int index) {
		return null;
	}

	public int getChildCount() {
		return 0;
	}

	public IDocumentNode[] getChildNodes() {
		return null;
	}

	public ArrayList getChildNodesList() {
		return null;
	}

	public IDocumentAttributeNode getDocumentAttribute(String name) {
		return null;
	}

	public String getIndent() {
		return null;
	}

	public int getLineIndent() {
		return 0;
	}

	public IDocumentAttributeNode[] getNodeAttributes() {
		return null;
	}

	public TreeMap getNodeAttributesMap() {
		return null;
	}

	public IDocumentNode getParentNode() {
		return null;
	}

	public IDocumentNode getPreviousSibling() {
		return null;
	}

	public IDocumentTextNode getTextNode() {
		return null;
	}

	public String getXMLAttributeValue(String name) {
		return null;
	}

	public String getXMLContent() {
		return null;
	}

	public String getXMLTagName() {
		return null;
	}

	public int indexOf(IDocumentNode child) {
		return 0;
	}

	public boolean isErrorNode() {
		return false;
	}

	public boolean isRoot() {
		return false;
	}

	public void reconnect(IDocumentNode parent, IModel model) {
	}

	public IDocumentNode removeChildNode(IDocumentNode child) {
		return null;
	}

	public IDocumentNode removeChildNode(int index) {
		return null;
	}

	public void removeDocumentAttribute(IDocumentAttributeNode attr) {
	}

	public void removeTextNode() {
	}

	public void setIsErrorNode(boolean isErrorNode) {
	}

	public void setLength(int length) {
	}

	public void setLineIndent(int indent) {
	}

	public void setOffset(int offset) {
	}

	public void setParentNode(IDocumentNode node) {
	}

	public void setPreviousSibling(IDocumentNode sibling) {
	}

	public void setXMLAttribute(IDocumentAttributeNode attribute) {
	}

	public boolean setXMLAttribute(String name, String value) {
		return false;
	}

	public boolean setXMLContent(String text) {
		return false;
	}

	public void setXMLTagName(String tag) {
	}

	public void swap(IDocumentNode child1, IDocumentNode child2) {
	}

	public String write(boolean indent) {
		return null;
	}

	public String writeShallow(boolean terminate) {
		return null;
	}

	public int getLength() {
		return 0;
	}

	public int getOffset() {
		return 0;
	}

	public int getXMLType() {
		return 0;
	}
	
	public boolean isContentCollapsed() {
		return false;
	}

	public boolean isLeafNode() {
		return false;
	}
	
	public boolean hasXMLChildren() {
		return false;
	}

	public boolean hasXMLContent() {
		return false;
	}
	
	public boolean hasXMLAttributes() {
		return false;
	}
	
	public int getNodeAttributesCount() {
		return 0;
	}

	public boolean canTerminateStartTag() {
		return false;
	}
	
}
