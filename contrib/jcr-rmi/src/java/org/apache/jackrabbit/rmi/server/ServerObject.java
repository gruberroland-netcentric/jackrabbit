/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDef;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDef;

/**
 * Base class for remote adapters. The purpose of this class is to
 * centralize the handling of the RemoteAdapterFactory instance used
 * to instantiate new server adapters.
 *
 * @author Jukka Zitting
 */
public class ServerObject extends UnicastRemoteObject {

    /** Factory for creating server adapters. */
    protected RemoteAdapterFactory factory;

    /**
     * Creates a basic server adapter that uses the given factory
     * to create new adapters.
     *
     * @param factory remote adapter factory
     */
    protected ServerObject(RemoteAdapterFactory factory)
            throws RemoteException {
        this.factory = factory;
    }

    /**
     * Returns a cleaned version of the given exception. In some cases
     * the underlying repository implementation may throw exceptions
     * that are either unserializable, use exception subclasses that are
     * only locally available, contain references to unserializable or
     * only locally available classes. This method returns a cleaned
     * version of such an exception. The returned exception contains only
     * the message string from the original exception, and uses the public
     * JCR exception class that most specifically matches the original
     * exception.
     *
     * @param ex the original exception
     * @return clean exception
     */
    protected RepositoryException getRepositoryException(
            RepositoryException ex) {
        if (ex instanceof AccessDeniedException) {
            return new AccessDeniedException(ex.getMessage());
        } else if (ex instanceof ConstraintViolationException) {
            return new ConstraintViolationException(ex.getMessage());
        } else if (ex instanceof InvalidItemStateException) {
            return new InvalidItemStateException(ex.getMessage());
        } else if (ex instanceof InvalidQueryException) {
            return new InvalidQueryException(ex.getMessage());
        } else if (ex instanceof InvalidSerializedDataException) {
            return new InvalidSerializedDataException(ex.getMessage());
        } else if (ex instanceof ItemExistsException) {
            return new ItemExistsException(ex.getMessage());
        } else if (ex instanceof ItemNotFoundException) {
            return new ItemNotFoundException(ex.getMessage());
        } else if (ex instanceof LockException) {
            return new LockException(ex.getMessage());
        } else if (ex instanceof LoginException) {
            return new LoginException(ex.getMessage());
        } else if (ex instanceof MergeException) {
            return new MergeException(ex.getMessage());
        } else if (ex instanceof NamespaceException) {
            return new NamespaceException(ex.getMessage());
        } else if (ex instanceof NoSuchNodeTypeException) {
            return new NoSuchNodeTypeException(ex.getMessage());
        } else if (ex instanceof NoSuchWorkspaceException) {
            return new NoSuchWorkspaceException(ex.getMessage());
        } else if (ex instanceof PathNotFoundException) {
            return new PathNotFoundException(ex.getMessage());
        } else if (ex instanceof ReferentialIntegrityException) {
            return new ReferentialIntegrityException(ex.getMessage());
        } else if (ex instanceof UnsupportedRepositoryOperationException) {
            return new UnsupportedRepositoryOperationException(ex.getMessage());
        } else if (ex instanceof ValueFormatException) {
            return new ValueFormatException(ex.getMessage());
        } else if (ex instanceof VersionException) {
            return new VersionException(ex.getMessage());
        } else {
            return new RepositoryException(ex.getMessage());
        }
    }

    /**
     * Utility method for creating a remote reference for a local item.
     * Unlike the factory method for creating remote item references, this
     * method introspects the type of the local item and returns the
     * corresponding node, property, or item remote reference using the
     * remote adapter factory.
     *
     * @param item local node, property, or item
     * @return remote node, property, or item reference
     * @throws RemoteException on RMI errors
     */
    protected RemoteItem getRemoteItem(Item item) throws RemoteException {
        if (item instanceof Property) {
            return factory.getRemoteProperty((Property) item);
        } else if (item instanceof Node) {
            return factory.getRemoteNode((Node) item);
        } else {
            return factory.getRemoteItem(item);
        }
    }

    /**
     * Utility method for creating an array of remote references for
     * local properties. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local property iterator
     * @return remote property array
     * @throws RemoteException on RMI errors
     */
    protected RemoteProperty[] getRemotePropertyArray(PropertyIterator iterator)
            throws RemoteException {
        if (iterator == null) {
            return new RemoteProperty[0]; // for safety
        }

        RemoteProperty[] remotes = new RemoteProperty[(int) iterator.getSize()];
        for (int i = 0; iterator.hasNext(); i++) {
            remotes[i] = factory.getRemoteProperty(iterator.nextProperty());
        }
        return remotes;
    }

    /**
     * Utility method for creating an array of remote references for
     * local nodes. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local node iterator
     * @return remote node array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNode[] getRemoteNodeArray(NodeIterator iterator)
            throws RemoteException {
        if (iterator == null) {
            return new RemoteNode[0]; // for safety
        }

        RemoteNode[] remotes = new RemoteNode[(int) iterator.getSize()];
        for (int i = 0; iterator != null && iterator.hasNext(); i++) {
            remotes[i] = factory.getRemoteNode(iterator.nextNode());
        }
        return remotes;
    }

    /**
     * Utility method for creating an array of remote references for
     * local node types. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param types local node type array
     * @return remote node type array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNodeType[] getRemoteNodeTypeArray(NodeType[] types)
            throws RemoteException {
        if (types == null) {
            return new RemoteNodeType[0]; // for safety
        }

        RemoteNodeType[] remotes = new RemoteNodeType[types.length];
        for (int i = 0; i < types.length; i++) {
            remotes[i] = factory.getRemoteNodeType(types[i]);
        }
        return remotes;
    }

    /**
     * Utility method for creating an array of remote references for
     * local node types. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty iterator.
     *
     * @param iterator local node type iterator
     * @return remote node type array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNodeType[] getRemoteNodeTypeArray(NodeTypeIterator iterator)
            throws RemoteException {
        if (iterator == null) {
            return new RemoteNodeType[0]; // for safety
        }

        RemoteNodeType[] remotes = new RemoteNodeType[(int) iterator.getSize()];
        for (int i = 0; iterator.hasNext(); i++) {
            remotes[i] = factory.getRemoteNodeType(iterator.nextNodeType());
        }
        return remotes;
    }

    /**
     * Utility method for creating an array of remote references for
     * local node definitions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param defs local node definition array
     * @return remote node definition array
     * @throws RemoteException on RMI errors
     */
    protected RemoteNodeDef[] getRemoteNodeDefArray(NodeDef[] defs)
            throws RemoteException {
        if (defs == null) {
            return new RemoteNodeDef[0]; // for safety
        }

        RemoteNodeDef[] remotes = new RemoteNodeDef[defs.length];
        for (int i = 0; i < defs.length; i++) {
            remotes[i] = factory.getRemoteNodeDef(defs[i]);
        }
        return remotes;
    }

    /**
     * Utility method for creating an array of remote references for
     * local property definitions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param defs local property definition array
     * @return remote property definition array
     * @throws RemoteException on RMI errors
     */
    protected RemotePropertyDef[] getRemotePropertyDefArray(PropertyDef[] defs) 
            throws RemoteException {
        if (defs == null) {
            return new RemotePropertyDef[0]; // for safety
        }

        RemotePropertyDef[] remotes = new RemotePropertyDef[defs.length];
        for (int i = 0; i < defs.length; i++) {
            remotes[i] = factory.getRemotePropertyDef(defs[i]);
        }
        return remotes;
    }

}
