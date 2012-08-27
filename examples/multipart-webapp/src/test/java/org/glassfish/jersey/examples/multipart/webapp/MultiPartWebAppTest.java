/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.examples.multipart.webapp;

import java.io.File;
import java.net.URI;
import java.util.Iterator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.util.SaxHelper;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartClientBinder;
import org.glassfish.jersey.message.internal.FormDataContentDisposition;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import org.w3c.dom.Document;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@code MultipartResource} class.
 *
 * @author Naresh (srinivas.bhimisetty at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class MultiPartWebAppTest extends JerseyTest {

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("multipart-webapp").build();
    }

    @Override
    protected Application configure() {
        return new MyApplication();
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.binders(new MultiPartClientBinder());
    }

    @Test
    public void testApplicationWadl() throws Exception {
        final WebTarget target = target().path("application.wadl");

        final File tmpFile = target.request().get(File.class);

        final DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setNamespaceAware(true);
        bf.setValidating(false);

        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
            bf.setXIncludeAware(false);
        }

        final DocumentBuilder b = bf.newDocumentBuilder();
        final Document d = b.parse(tmpFile);

        final XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new NSResolver("wadl", "http://wadl.dev.java.net/2009/02"));
        String val = (String) xp.evaluate(
                "//wadl:resource[@path='part']/wadl:method/wadl:request/wadl:representation/@mediaType", d, XPathConstants.STRING);

        assertEquals("multipart/form-data", val);
    }

    @Test
    public void testPart() {
        final WebTarget target = target().path("form/part");

        final FormDataMultiPart mp = new FormDataMultiPart();
        final FormDataBodyPart p = new FormDataBodyPart(FormDataContentDisposition.name("part").build(), "CONTENT");
        mp.bodyPart(p);

        final String s = target.request().post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
        assertEquals("CONTENT", s);
    }

    @Test
    public void testPartWithFileName() {
        final WebTarget target = target().path("form/part-file-name");

        final FormDataMultiPart mp = new FormDataMultiPart();
        final FormDataBodyPart p = new FormDataBodyPart(FormDataContentDisposition.name("part").fileName("file").build(),
                "CONTENT");
        mp.bodyPart(p);

        final String s = target.request().post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
        assertEquals("CONTENT:file", s);
    }

    @Test
    public void testXmlJAXBPart() {
        final WebTarget target = target().path("form/xml-jaxb-part");

        final FormDataMultiPart mp = new FormDataMultiPart();
        mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("bean").fileName("bean").build(),
                new Bean("BEAN"),
                MediaType.APPLICATION_XML_TYPE));
        mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("string").fileName("string").build(),
                "STRING"));

        final String s = target.request().post(Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
        assertEquals("STRING:string,BEAN:bean", s);
    }

    class NSResolver implements NamespaceContext {

        private String prefix;
        private String nsURI;

        public NSResolver(String prefix, String nsURI) {
            this.prefix = prefix;
            this.nsURI = nsURI;
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals(this.prefix)) {
                return this.nsURI;
            } else {
                return XMLConstants.NULL_NS_URI;
            }
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (namespaceURI.equals(this.nsURI)) {
                return this.prefix;
            } else {
                return null;
            }
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }

    }

}
