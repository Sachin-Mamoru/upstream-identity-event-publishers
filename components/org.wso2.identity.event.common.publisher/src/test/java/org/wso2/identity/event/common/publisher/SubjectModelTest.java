/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.event.common.publisher;

import org.mockito.Mock;
import org.testng.annotations.Test;
import org.wso2.identity.event.common.publisher.model.common.ComplexSubject;
import org.wso2.identity.event.common.publisher.model.common.SimpleSubject;
import org.wso2.identity.event.common.publisher.model.common.Subject;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/*
    Test Class for Subject, SimpleSubject and ComplexSubject Classes
 */
public class SubjectModelTest {

    @Mock
    private SimpleSubject simpleSubject1;
    @Mock
    private SimpleSubject simpleSubject2;
    @Mock
    private SimpleSubject simpleSubject3;
    @Mock
    private SimpleSubject simpleSubject4;
    @Mock
    private SimpleSubject simpleSubject5;
    @Mock
    private SimpleSubject simpleSubject6;

    @Test
    public void testSimpleSubjectAccount() {

        Subject simpleSubject = SimpleSubject.createAccountSubject("test");
        assertNotNull(simpleSubject);
        assertEquals(simpleSubject.getFormat(), "account");
        assertNotNull(simpleSubject.getProperties());
        assertEquals(simpleSubject.getProperty("uri"), "test");
    }

    @Test
    public void testSimpleSubjectEmail() {

        Subject simpleSubject = SimpleSubject.createEmailSubject("test@example.com");
        assertNotNull(simpleSubject);
        assertEquals(simpleSubject.getFormat(), "email");
        assertNotNull(simpleSubject.getProperties());
        assertEquals(simpleSubject.getProperty("email"), "test@example.com");
    }

    @Test
    public void testSimpleSubjectURI() {

        Subject simpleSubject = SimpleSubject.createURISubject("test");
        assertNotNull(simpleSubject);
        assertEquals(simpleSubject.getFormat(), "uri");
        assertNotNull(simpleSubject.getProperties());
        assertEquals(simpleSubject.getProperty("uri"), "test");
        assertNull(simpleSubject.getProperty("email"));
    }

    @Test
    public void testSimpleSubjectPhone() {

        Subject simpleSubject = SimpleSubject.createPhoneSubject("+876756478");
        assertNotNull(simpleSubject);
        assertEquals(simpleSubject.getFormat(), "phone_number");
        assertNotNull(simpleSubject.getProperties());
        assertEquals(simpleSubject.getProperty("phone_number"), "+876756478");
    }

    @Test
    public void testSimpleSubjectIssSub() {

        Subject simpleSubject = SimpleSubject.createIssSubSubject("testIss", "testSub");
        assertNotNull(simpleSubject);
        assertEquals(simpleSubject.getFormat(), "iss_sub");
        assertNotNull(simpleSubject.getProperties());
        assertEquals(simpleSubject.getProperty("iss"), "testIss");
        assertEquals(simpleSubject.getProperty("sub"), "testSub");
    }

    @Test
    public void testSimpleSubjectOpaque() {

        Subject simpleSubject = SimpleSubject.createOpaqueSubject("test");
        assertNotNull(simpleSubject);
        assertEquals(simpleSubject.getFormat(), "opaque");
        assertNotNull(simpleSubject.getProperties());
        assertEquals(simpleSubject.getProperty("id"), "test");
    }

    @Test
    public void testSimpleSubjectDID() {

        Subject simpleSubject = SimpleSubject.createDIDSubject("test");
        assertNotNull(simpleSubject);
        assertEquals(simpleSubject.getFormat(), "did");
        assertNotNull(simpleSubject.getProperties());
        assertEquals(simpleSubject.getProperty("did"), "test");
    }

    @Test
    public void testComplexSubjectTenant() {

        ComplexSubject subject = ComplexSubject.builder()
                .tenant(simpleSubject1)
                .user(simpleSubject2)
                .session(simpleSubject3)
                .application(simpleSubject4)
                .group(simpleSubject5)
                .organization(simpleSubject6)
                .build();

        assertNotNull(subject);

        assertEquals(subject.getFormat(), "complex");
        assertNotNull(subject.getProperties());

        assertEquals(simpleSubject1, subject.getProperty("tenant"));
        assertEquals(simpleSubject2, subject.getProperty("user"));
        assertEquals(simpleSubject3, subject.getProperty("session"));
        assertEquals(simpleSubject4, subject.getProperty("application"));
        assertEquals(simpleSubject5, subject.getProperty("group"));
        assertEquals(simpleSubject6, subject.getProperty("organization"));

    }

    @Test
    public void testComplexSubjectTenantWithNullProperty() {

        ComplexSubject subject = ComplexSubject.builder()
                .tenant(null)
                .user(null)
                .session(null)
                .application(null)
                .group(null)
                .organization(null)
                .build();

        assertNotNull(subject);

        assertEquals(subject.getFormat(), "complex");
        assertNotNull(subject.getProperties());

        assertNull(subject.getProperty("tenant"));
        assertNull(subject.getProperty("user"));
        assertNull(subject.getProperty("session"));
        assertNull(subject.getProperty("application"));
        assertNull(subject.getProperty("group"));
        assertNull(subject.getProperty("organization"));

    }

}
