/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractWriteTest;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * <code>EvaluationTest</code>...
 */
public class WriteTest extends AbstractWriteTest {

    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable(acMgr);
    }

    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    protected Map getRestrictions(Session s, String path) {
        return Collections.EMPTY_MAP;
    }

    public void testAccessControlModification2() throws RepositoryException, NotExecutableException {
        /*
         precondition:
         testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);

        // give 'testUser' READ_AC|MODIFY_AC privileges at 'path'
        Privilege[] privileges = privilegesFromNames(new String[] {
                Privilege.JCR_READ_ACCESS_CONTROL,
                Privilege.JCR_MODIFY_ACCESS_CONTROL
        });
        JackrabbitAccessControlList tmpl = givePrivileges(path, privileges, getRestrictions(superuser, path));
        /*
         testuser must
         - still have the inherited READ permission.
         - must have permission to view AC items at 'path' (and below)
         - must have permission to modify AC items at 'path'

         testuser must not have
         - permission to view AC items outside of the tree defined by path.
        */

        // make sure the 'rep:policy' node has been created.
        assertTrue(superuser.itemExists(tmpl.getPath() + "/rep:policy"));

        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        // test: MODIFY_AC granted at 'path'
        assertTrue(testAcMgr.hasPrivileges(path, privilegesFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL)));

        // test if testuser can READ access control on the path and on the
        // entire subtree that gets the policy inherited.
        AccessControlPolicy[] policies = testAcMgr.getPolicies(path);
        testAcMgr.getEffectivePolicies(path);
        testAcMgr.getEffectivePolicies(childNPath);

        // test: READ_AC privilege does not apply outside of the tree.
        try {
            testAcMgr.getPolicies(siblingPath);
            fail("READ_AC privilege must not apply outside of the tree it has applied to.");
        } catch (AccessDeniedException e) {
            // success
        }

        // test: MODIFY_AC privilege does not apply outside of the tree.
        try {
            testAcMgr.setPolicy(siblingPath, policies[0]);
            fail("MODIFY_AC privilege must not apply outside of the tree it has applied to.");
        } catch (AccessDeniedException e) {
            // success
        }

        // test if testuser can modify AC-items
        // 1) add an ac-entry
        ACLTemplate acl = (ACLTemplate) policies[0];
        acl.addAccessControlEntry(getTestUser().getPrincipal(), privilegesFromName(PrivilegeRegistry.REP_WRITE));
        testAcMgr.setPolicy(path, acl);
        testSession.save();

        assertTrue(testAcMgr.hasPrivileges(path,
                privilegesFromName(Privilege.JCR_REMOVE_CHILD_NODES)));

        // 2) remove the policy
        testAcMgr.removePolicy(path, policies[0]);
        testSession.save();

        // Finally: testuser removed the policy that granted him permission
        // to modify the AC content. Since testuser removed the policy, it's
        // privileges must be gone again...
        try {
            testAcMgr.getEffectivePolicies(childNPath);
            fail("READ_AC privilege has been revoked -> must throw again.");
        } catch (AccessDeniedException e) {
            // success
        }
        // ... and since the ACE is stored with the policy all right except
        // READ must be gone.
        checkReadOnly(path);
    }

    public void testRemovePermission9() throws NotExecutableException, RepositoryException {
        AccessControlManager testAcMgr = getTestACManager();
        /*
          precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Privilege[] rmChildNodes = privilegesFromName(Privilege.JCR_REMOVE_CHILD_NODES);
        Privilege[] rmNode = privilegesFromName(Privilege.JCR_REMOVE_NODE);

        // add 'remove_child_nodes' at 'path and allow 'remove_node' at childNPath
        givePrivileges(path, rmChildNodes, getRestrictions(superuser, path));
        givePrivileges(childNPath, rmNode, getRestrictions(superuser, childNPath));
        /*
         expected result:
         - rep:policy node can still not be remove for it is access-control
           content that requires jcr:modifyAccessControl privilege instead.
         */
        String policyPath = childNPath + "/rep:policy";
        assertFalse(getTestSession().hasPermission(policyPath, javax.jcr.Session.ACTION_REMOVE));
        assertTrue(testAcMgr.hasPrivileges(policyPath, new Privilege[] {rmChildNodes[0], rmNode[0]}));
    }

    public void testApplicablePolicies() throws RepositoryException {
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(childNPath);
        assertTrue(it.hasNext());

        // the same should be true, if the rep:AccessControllable mixin has
        // been manually added
        Node n = (Node) superuser.getItem(childNPath);
        n.addMixin(((SessionImpl) superuser).getJCRName(AccessControlConstants.NT_REP_ACCESS_CONTROLLABLE));
        it = acMgr.getApplicablePolicies(childNPath);
        assertTrue(it.hasNext());
    }

    public void testInheritance2() throws RepositoryException, NotExecutableException {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();

        /*
          precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        // give jcr:write privilege on 'path' and withdraw them on 'childNPath'
        Privilege[] privileges = privilegesFromNames(new String[] {Privilege.JCR_WRITE});
        givePrivileges(path, privileges, getRestrictions(superuser, path));
        withdrawPrivileges(childNPath, privileges, getRestrictions(superuser, path));

        /*
        since evaluation respects inheritance through the node
        hierarchy, the jcr:write privilege must not be granted at childNPath
        */
        assertFalse(testAcMgr.hasPrivileges(childNPath, privileges));

        /*
         ... same for permissions at 'childNPath'
         */
        String actions = Session.ACTION_SET_PROPERTY + "," + Session.ACTION_REMOVE + "," + Session.ACTION_ADD_NODE;

        String nonExistingItemPath = childNPath + "/anyItem";
        assertFalse(testSession.hasPermission(nonExistingItemPath, actions));

        // yet another level in the hierarchy
        Node grandChild = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();
        String gcPath = grandChild.getPath();

        // grant write privilege again
        givePrivileges(gcPath, privileges, getRestrictions(superuser, path));
        assertTrue(testAcMgr.hasPrivileges(gcPath, privileges));
        assertTrue(testSession.hasPermission(gcPath + "/anyProp", Session.ACTION_SET_PROPERTY));
        // however: removing the grand-child nodes must not be allowed as
        // remove_child_node privilege is missing on the direct ancestor.
        assertFalse(testSession.hasPermission(gcPath, Session.ACTION_REMOVE));
    }
}