/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2014 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.netbeans.html.archetype.test;

import java.io.IOException;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public class ArchetypeVersionIT {
    private String version;
    
    public ArchetypeVersionIT() {
    }
    
    @BeforeClass public void readCurrentVersion() throws Exception {
        version = findCurrentVersion();
        assertFalse(version.isEmpty(), "There should be some version string");
    }
    

    @Test public void testComparePomDepsVersions() throws Exception {
        final ClassLoader l = ArchetypeVersionIT.class.getClassLoader();
        URL r = l.getResource("archetype-resources/pom.xml");
        assertNotNull(r, "Archetype pom found");
        
        final XPathFactory fact = XPathFactory.newInstance();
        XPathExpression xp2 = fact.newXPath().compile(
            "//properties/net.java.html.version/text()"
        );
        
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(r.openStream());
        String arch = (String) xp2.evaluate(dom, XPathConstants.STRING);

        assertEquals(arch, version, "net.java.html.json dependency needs to be on latest version");
    }
    
    @Test public void testNbActions() throws Exception {
        final ClassLoader l = ArchetypeVersionIT.class.getClassLoader();
        URL r = l.getResource("archetype-resources/nbactions.xml");
        assertNotNull(r, "Archetype nb file found");
        
        final XPathFactory fact = XPathFactory.newInstance();
        XPathExpression xp2 = fact.newXPath().compile(
            "//goal/text()"
        );
        
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(r.openStream());
        NodeList goals = (NodeList) xp2.evaluate(dom, XPathConstants.NODESET);
        
        for (int i = 0; i < goals.getLength(); i++) {
            String s = goals.item(i).getTextContent();
            if (s.contains("netbeans")) {
                assertFalse(s.matches(".*netbeans.*[0-9].*"), "No numbers: " + s);
            }
        }
    }

    static String findCurrentVersion() throws XPathExpressionException, IOException, ParserConfigurationException, SAXException, XPathFactoryConfigurationException {
        final ClassLoader l = ArchetypeVersionIT.class.getClassLoader();
        URL u = l.getResource("META-INF/maven/org.apidesign.html/knockout4j-archetype/pom.xml");
        assertNotNull(u, "Own pom found: " + System.getProperty("java.class.path"));

        final XPathFactory fact = XPathFactory.newInstance();
        fact.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        XPathExpression xp = fact.newXPath().compile("project/version/text()");
        
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(u.openStream());
        return xp.evaluate(dom);
    }
}
