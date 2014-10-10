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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import static org.testng.Assert.*;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.reporters.Files;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Jaroslav Tulach
 */
public class VerifyArchetypeIT {
    @BeforeMethod public void cleanUpMavenRepo() throws IOException {
        File repo = new File(new File(
            new File(new File(System.getProperty("user.home"), ".m2"), "repository"),
            "org"), "someuser"
        );
        if (repo.exists()) {
            java.nio.file.Files.walkFileTree(repo.toPath(), new FileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    java.nio.file.Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    java.nio.file.Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    @Test public void defaultProjectCompiles() throws Exception {
        final File dir = new File("target/tests/fxcompile/").getAbsoluteFile();
        generateFromArchetype("o-a-test", dir);
        
        File created = new File(dir, "o-a-test");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        Verifier v = new Verifier(created.getAbsolutePath());
        v.executeGoal("verify");
        
        v.verifyErrorFreeLog();
        
        for (String l : v.loadFile(v.getBasedir(), v.getLogFileName(), false)) {
            if (l.contains("j2js")) {
                fail("No pre-compilaton:\n" + l);
            }
        }
        
        v.verifyTextInLog("fxcompile/o-a-test/client/target/o-a-test-1.0-SNAPSHOT-html.java.net.zip");
        
        v.assertFilePresent("server/nbactions.xml");
    }
    
    @Test
    public void bck2brwsrProjectCompiles() throws Exception {
        final File dir = new File("target/tests/b2bcmp/").getAbsoluteFile();
        generateFromArchetype("b-p-test", dir, "-Dbck2brwsr=true");
        
        File created = new File(new File(dir, "b-p-test"), "client");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        File main = new File(new File(created, "src"), "main");
        File pages = new File(new File(main, "webapp"), "pages");
        File index = new File(pages, "index.html");
        
        String indexContent = Files.readFile(index);
        assertTrue(indexContent.contains("src=\"bck2brwsr.js\""), "There should be bck2brwsr.js reference in " + index);

        {
            Verifier v = new Verifier(created.getParent());
            v.addCliOption("-DskipTests=true");
            v.executeGoal("install");
            v.verifyErrorFreeLog();
        }
        
        Verifier v = new Verifier(created.getAbsolutePath());
        v.addCliOption("-Pbck2brwsr");
        v.executeGoal("package");
        
        v.verifyErrorFreeLog();
        v.verifyTextInLog("b2bcmp/b-p-test/client/target/b-p-test-1.0-SNAPSHOT-bck2brwsr.zip");
        
        v.assertFileNotPresent("target/res/drawable-hdpi/ic_launcher.png");
        v.assertFileNotPresent("target/res/drawable-mdpi/ic_launcher.png");
        v.assertFileNotPresent("target/res/drawable-xhdpi/ic_launcher.png");
        v.assertFileNotPresent("target/res/drawable-xxhdpi/ic_launcher.png");

        v.assertFilePresent("target/b-p-test-1.0-SNAPSHOT-bck2brwsr/");
        v.assertFilePresent("target/b-p-test-1.0-SNAPSHOT-bck2brwsr/public_html/index.html");
        v.assertFilePresent("target/b-p-test-1.0-SNAPSHOT-bck2brwsr/public_html/bck2brwsr.js");
        v.assertFilePresent("target/b-p-test-1.0-SNAPSHOT-bck2brwsr.zip");
        v.assertFilePresent("target/b-p-test.js");

        File nbactions = new File(created, "nbactions.xml");
        assertTrue(nbactions.isFile(), "Actions file is in there");
        assertTrue(Files.readFile(nbactions).contains("bck2brwsr"), "There should bck2brwsr goal in " + nbactions);
    }
    
    @Test public void bck2brwsrAndNbrwsrProjectCompiles() throws Exception {
        final File dir = new File("target/tests/BandN/").getAbsoluteFile();
        generateFromArchetype("b-n-test", dir, "-Dbck2brwsr=true", "-Dnbrwsr=true");
        
        File created = new File(new File(dir, "b-n-test"), "client");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        File main = new File(new File(created, "src"), "main");
        File pages = new File(new File(main, "webapp"), "pages");
        File index = new File(pages, "index.html");
        
        String indexContent = Files.readFile(index);
        assertTrue(indexContent.contains("src=\"bck2brwsr.js\""), "There should be bck2brwsr.js reference in " + index);
        
        {
            Verifier v = new Verifier(created.getParent());
            v.addCliOption("-DskipTests=true");
            v.executeGoal("install");
            v.verifyErrorFreeLog();
        }

        {
            Verifier v = new Verifier(created.getAbsolutePath());
            v.addCliOption("-Pbck2brwsr");
            v.addCliOption("-Dbck2brwsr.obfuscationlevel=NONE");
            v.executeGoal("package");

            v.verifyErrorFreeLog();
            v.verifyTextInLog("BandN/b-n-test/client/target/b-n-test-1.0-SNAPSHOT-bck2brwsr.zip");

            v.assertFileNotPresent("target/res/drawable-hdpi/ic_launcher.png");
            v.assertFileNotPresent("target/res/drawable-mdpi/ic_launcher.png");
            v.assertFileNotPresent("target/res/drawable-xhdpi/ic_launcher.png");
            v.assertFileNotPresent("target/res/drawable-xxhdpi/ic_launcher.png");

            v.assertFilePresent("target/b-n-test-1.0-SNAPSHOT-bck2brwsr/");
            v.assertFilePresent("target/b-n-test-1.0-SNAPSHOT-bck2brwsr/public_html/index.html");
            v.assertFilePresent("target/b-n-test-1.0-SNAPSHOT-bck2brwsr/public_html/bck2brwsr.js");
            v.assertFilePresent("target/b-n-test-1.0-SNAPSHOT-bck2brwsr.zip");
            v.assertFilePresent("target/b-n-test.js");

            File nbactions = new File(created, "nbactions.xml");
            assertTrue(nbactions.isFile(), "Actions file is in there");
            assertTrue(Files.readFile(nbactions).contains("bck2brwsr"), "There should bck2brwsr goal in " + nbactions);
        }
    }
    
    @Test
    public void nbrwsrProjectCompiles() throws Exception {
        final File dir = new File("target/tests/ncmp/").getAbsoluteFile();
        generateFromArchetype("n-p-test", dir, "-Dnbrwsr=true");
        
        File created = new File(new File(dir, "n-p-test"), "client");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        File main = new File(new File(created, "src"), "main");
        File pages = new File(new File(main, "webapp"), "pages");
        File index = new File(pages, "index.html");
        assertTrue(index.exists(), "Index page is there");
        
        File plus = new File(pages, "plus.css");
        plus.createNewFile();
        
        {
            Verifier v = new Verifier(created.getParent());
            v.addCliOption("-DskipTests=true");
            v.executeGoal("install");
            v.verifyErrorFreeLog();
        }
        
        
        Verifier v = new Verifier(created.getAbsolutePath());
//        v.addCliOption("-Pnbrwsr");
        v.executeGoal("package");
        
        v.verifyErrorFreeLog();
        
        v.assertFilePresent("target/classes/META-INF/generated-layer.xml");
        v.assertFilePresent("target/classes/org/someuser/test/oat/index.html");
        v.assertFileNotPresent("target/classes/org/someuser/test/oat/plus.css");

        File jar = new File(new File(created, "target"), "n-p-test-1.0-SNAPSHOT.jar");
        assertTrue(jar.exists(), "File is created: " + jar);
        JarFile jf = new JarFile(jar);
        String cp = jf.getManifest().getMainAttributes().getValue("Class-Path");
        assertNotNull(cp, "Classpath found: " + cp);

        File nbactions = new File(created, "nbactions.xml");
        assertTrue(nbactions.isFile(), "Actions file is in there");
        assertTrue(Files.readFile(nbactions).contains("nbm"), "There should nbm goal in " + nbactions);
    }
    
    @Test
    public void nbrwsrProjectCompilesForNetBeans() throws Exception {
        final File dir = new File("target/tests/nbmcmp/").getAbsoluteFile();
        generateFromArchetype("n-n-test", dir, "-Dnbrwsr=true");
        
        File created = new File(new File(dir, "n-n-test"), "client");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        File main = new File(new File(created, "src"), "main");
        File pages = new File(new File(main, "webapp"), "pages");
        File index = new File(pages, "index.html");

        {
            Verifier v = new Verifier(created.getParent());
            v.executeGoal("install");
            v.verifyErrorFreeLog();
        }
        Verifier v = new Verifier(created.getAbsolutePath());
        v.addCliOption("-Pnbrwsr");
        v.executeGoal("install");
        v.verifyErrorFreeLog();
        
        v.assertFilePresent("target/n-n-test-1.0-SNAPSHOT.nbm");
        
        File jar = new File(new File(created, "target"), "n-n-test-1.0-SNAPSHOT.jar");
        assertTrue(jar.exists(), "File is created: " + jar);
        JarFile jf = new JarFile(jar);
        String cp = jf.getManifest().getMainAttributes().getValue("Class-Path");
        assertNull(cp, "No classpath: " + cp);
    }
    
    @Test
    public void nbrwsrProjectCompilesForNetBeansAndCopiesAllResources() throws Exception {
        final File dir = new File("target/tests/nbmallres/").getAbsoluteFile();
        generateFromArchetype("a-r-test", dir, "-Dnbrwsr=true");
        
        File created = new File(new File(dir, "a-r-test"), "client");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        File main = new File(new File(created, "src"), "main");
        File pages = new File(new File(main, "webapp"), "pages");
        File index = new File(pages, "index.html");
        assertTrue(index.exists(), "Index page is there");
        
        File plus = new File(pages, "plus.css");
        plus.createNewFile();
        
        {
            Verifier v = new Verifier(created.getParent());
            v.executeGoal("install");
            v.verifyErrorFreeLog();
        }
        Verifier v = new Verifier(created.getAbsolutePath());
        v.addCliOption("-Pnbrwsr");
        v.executeGoal("install");
        
        v.verifyErrorFreeLog();
        
        v.assertFilePresent("target/a-r-test-1.0-SNAPSHOT.nbm");
        v.assertFilePresent("target/classes/org/someuser/test/oat/index.html");
        v.assertFilePresent("target/classes/org/someuser/test/oat/plus.css");
        v.assertFilePresent("target/classes/org/someuser/test/oat/icon.png");
        v.assertFilePresent("target/classes/org/someuser/test/oat/icon24.png");
    }
    
    private Verifier generateFromArchetype(String aId, final File dir, String... params) throws Exception {
        Verifier v = new Verifier(dir.getAbsolutePath());
        v.setAutoclean(false);
        v.setLogFileName("generate.log");
        v.deleteDirectory("");
        dir.mkdirs();
        Properties sysProp = v.getSystemProperties();
        sysProp.put("groupId", "org.someuser.test");
        sysProp.put("artifactId", aId);
        sysProp.put("package", "org.someuser.test.oat");
        sysProp.put("archetypeGroupId", "org.apidesign.html");
        sysProp.put("archetypeArtifactId", "crud4j-archetype");
        sysProp.put("archetypeVersion", findCurrentVersion());
        
        for (String p : params) {
            v.addCliOption(p);
        }
        v.executeGoal("archetype:generate");
        v.verifyErrorFreeLog();
        return v;
    }
    
    static String findCurrentVersion() throws XPathExpressionException, IOException, ParserConfigurationException, SAXException, XPathFactoryConfigurationException {
        final ClassLoader l = VerifyArchetypeIT.class.getClassLoader();
        URL u = l.getResource("META-INF/maven/org.apidesign.html/crud4j-archetype/pom.xml");
        assertNotNull(u, "Own pom found: " + System.getProperty("java.class.path"));

        final XPathFactory fact = XPathFactory.newInstance();
        fact.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        XPathExpression xp = fact.newXPath().compile("project/version/text()");
        
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(u.openStream());
        return xp.evaluate(dom);
    }    
}
