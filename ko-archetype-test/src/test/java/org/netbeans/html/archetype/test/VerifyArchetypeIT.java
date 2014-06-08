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

import java.io.File;
import java.util.Properties;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import org.testng.reporters.Files;

/**
 *
 * @author Jaroslav Tulach
 */
public class VerifyArchetypeIT {
    @Test public void defaultProjectCompiles() throws Exception {
        final File dir = new File("target/tests/fxcompile/").getAbsoluteFile();
        generateFromArchetype(dir);
        
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
        
        v.verifyTextInLog("fxcompile/o-a-test/target/o-a-test-1.0-SNAPSHOT-html.java.net.zip");
    }
    
    @Test public void iBrwsrProjectCompiles() throws Exception {
        final File dir = new File("target/tests/icompile/").getAbsoluteFile();
        generateFromArchetype(dir);
        
        File created = new File(dir, "o-a-test");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        Verifier v = new Verifier(created.getAbsolutePath());
        v.addCliOption("-Pibrwsr");
        v.executeGoal("verify");
        
        v.verifyErrorFreeLog();
        v.verifyTextInLog("icompile/o-a-test/target/o-a-test-1.0-SNAPSHOT-html.java.net.zip");
        
        Verifier v2 = new Verifier(created.getAbsolutePath());
        v2.addCliOption("-Pibrwsr");
        try { 
            v2.executeGoal("robovm:ipad-sim");
        } catch (VerificationException ex) {
            // OK, the run should fail on other systems than mac
        }
        v2.verifyTextInLog("Building RoboVM app for: ios (x86)");
        
        File nbactions = new File(created, "nbactions.xml");
        assertTrue(nbactions.isFile(), "Actions file is in there");
        assertTrue(Files.readFile(nbactions).contains("robovm"), "There should robovm goals in " + nbactions);

        v2.assertFilePresent("target/images/Icon.png");
        v2.assertFilePresent("target/images/Icon@2.png");
        v2.assertFilePresent("target/images/Icon-60.png");
        v2.assertFilePresent("target/images/Icon-60@2.png");
        v2.assertFilePresent("target/images/Icon-72.png");
        v2.assertFilePresent("target/images/Icon-76.png");
    }
    
    @Test public void skipiBrwsrProjectCompiles() throws Exception {
        final File dir = new File("target/tests/noicompile/").getAbsoluteFile();
        generateFromArchetype(dir, "-Dibrwsr=false");
        
        File created = new File(dir, "o-a-test");
        assertTrue(created.isDirectory(), "Project created");
        File pom = new File(created, "pom.xml");
        assertTrue(pom.isFile(), "Pom file is in there");
        assertFalse(Files.readFile(pom).contains("ibrwsr"), "There should be no mention of ibrwsr in " + pom);
        
        Verifier v = new Verifier(created.getAbsolutePath());
        v.executeGoal("package");
        
        v.verifyErrorFreeLog();
        v.verifyTextInLog("noicompile/o-a-test/target/o-a-test-1.0-SNAPSHOT-html.java.net.zip");
        
        File nbactions = new File(created, "nbactions.xml");
        assertTrue(nbactions.isFile(), "Actions file is in there");
        assertFalse(Files.readFile(nbactions).contains("robovm"), "There should be no mention of robovm in " + nbactions);
    }

    @Test public void dlvkbrwsrProjectCompiles() throws Exception {
        final File dir = new File("target/tests/dlvkbrwsrcmp/").getAbsoluteFile();
        generateFromArchetype(dir, "-Ddlvkbrwsr=true");
        
        File created = new File(dir, "o-a-test");
        assertTrue(created.isDirectory(), "Project created");
        assertTrue(new File(created, "pom.xml").isFile(), "Pom file is in there");
        
        Verifier v = new Verifier(created.getAbsolutePath());
        v.addCliOption("-Pdlvkbrwsr");
        v.executeGoal("verify");
        
        v.verifyErrorFreeLog();
        v.verifyTextInLog("dlvkbrwsrcmp/o-a-test/target/o-a-test-1.0-SNAPSHOT-html.java.net.zip");
        
        v.assertFilePresent("target/res/drawable-hdpi/ic_launcher.png");
        v.assertFilePresent("target/res/drawable-mdpi/ic_launcher.png");
        v.assertFilePresent("target/res/drawable-xhdpi/ic_launcher.png");
        v.assertFilePresent("target/res/drawable-xxhdpi/ic_launcher.png");
        
        Verifier v2 = new Verifier(created.getAbsolutePath());
        v2.addCliOption("-Pdlvkbrwsr");
        v2.executeGoal("package");
        v2.verifyTextInLog("android-maven-plugin");
        
//        File nbactions = new File(created, "nbactions.xml");
//        assertTrue(nbactions.isFile(), "Actions file is in there");
//        assertTrue(Files.readFile(nbactions).contains("robovm"), "There should robovm goals in " + nbactions);
    }

    @Test public void withoutDlvkbrwsrProjectCompiles() throws Exception {
        final File dir = new File("target/tests/wdlvkbrwsrcmp/").getAbsoluteFile();
        generateFromArchetype(dir, "-Ddlvkbrwsr=false");
        
        File created = new File(dir, "o-a-test");
        assertTrue(created.isDirectory(), "Project created");
        final File pom = new File(created, "pom.xml");
        assertTrue(pom.isFile(), "Pom file is in there");
        assertFalse(Files.readFile(pom).contains("dlvkbrwsr"), "There should be no mention of dlvkbrwsr in " + pom);
        
        Verifier v = new Verifier(created.getAbsolutePath());
        v.addCliOption("-Pdlvkbrwsr");
        v.executeGoal("verify");
        
        v.verifyErrorFreeLog();
        v.verifyTextInLog("wdlvkbrwsrcmp/o-a-test/target/o-a-test-1.0-SNAPSHOT-html.java.net.zip");
    }
    
    private Verifier generateFromArchetype(final File dir, String... params) throws Exception {
        Verifier v = new Verifier(dir.getAbsolutePath());
        v.setAutoclean(false);
        v.setLogFileName("generate.log");
        v.deleteDirectory("");
        dir.mkdirs();
        Properties sysProp = v.getSystemProperties();
        sysProp.put("groupId", "org.someuser.test");
        sysProp.put("artifactId", "o-a-test");
        sysProp.put("package", "org.someuser.test.oat");
        sysProp.put("archetypeGroupId", "org.apidesign.html");
        sysProp.put("archetypeArtifactId", "knockout4j-archetype");
        sysProp.put("archetypeVersion", ArchetypeVersionIT.findCurrentVersion());
        
        for (String p : params) {
            v.addCliOption(p);
        }
        v.executeGoal("archetype:generate");
        v.verifyErrorFreeLog();
        return v;
    }
}
