/*****************************************************************************
 * Copyright (C) NanoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package org.nanocontainer.nanowar;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.nanocontainer.integrationkit.ContainerBuilder;
import org.nanocontainer.integrationkit.DefaultLifecycleContainerBuilder;
import org.nanocontainer.integrationkit.PicoCompositionException;
import org.nanocontainer.script.groovy.GroovyContainerBuilder;
import org.nanocontainer.script.xml.XMLContainerBuilder;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.DefaultPicoContainer;

/**
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 * @version $Revision$
 */
public class ServletContainerListenerTestCase extends MockObjectTestCase implements KeyConstants {

    private ServletContainerListener listener;

    private String groovyScript =
        "pico = builder.container(parent:parent, scope:assemblyScope) {\n" +
        "   component(key:'string', instance:'A String')\n" +
        "}";
    
    private String xmlScript =
            "<container>" +
            "<component-instance key='string'>" +
            "      <string>A String</string>" +
            "</component-instance>" +
            "</container>";

    public void setUp(){
        listener = new ServletContainerListener();
    }
    
    public void testApplicationScopeContainerIsCreatedWithInlinedScripts() {
        assertApplicationScopeContainerIsCreatedWithInlinedScript("nanocontainer.groovy", groovyScript, GroovyContainerBuilder.class);
        assertApplicationScopeContainerIsCreatedWithInlinedScript("nanocontainer.xml", xmlScript, XMLContainerBuilder.class);
    }

    private void assertApplicationScopeContainerIsCreatedWithInlinedScript(String scriptName, String script, 
            Class containerBuilder) {
        Mock servletContextMock = mock(ServletContext.class);
        final Vector initParams = new Vector();
        initParams.add(scriptName);
        servletContextMock.expects(once())
                .method("getInitParameterNames")
                .withNoArguments()
                .will(returnValue(initParams.elements()));
        servletContextMock.expects(once())
                .method("getInitParameter")
                .with(eq(scriptName))
                .will(returnValue(script));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(BUILDER), isA(containerBuilder));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(APPLICATION_CONTAINER), isA(PicoContainer.class));

        listener.contextInitialized(new ServletContextEvent((ServletContext) servletContextMock.proxy()));
    }
    
    public void testApplicationScopeContainerIsCreatedWithInSeparateScripts() {
        assertApplicationScopeContainerIsCreatedWithSeparateScript("nanocontainer.groovy", groovyScript, GroovyContainerBuilder.class);
        assertApplicationScopeContainerIsCreatedWithSeparateScript("nanocontainer.xml", xmlScript, XMLContainerBuilder.class);
    }

    private void assertApplicationScopeContainerIsCreatedWithSeparateScript(String scriptName, String script, 
            Class containerBuilder) {
        Mock servletContextMock = mock(ServletContext.class);
        final Vector initParams = new Vector();
        initParams.add(scriptName);
        servletContextMock.expects(once())
                .method("getInitParameterNames")
                .withNoArguments()
                .will(returnValue(initParams.elements()));
        servletContextMock.expects(once())
                .method("getInitParameter")
                .with(eq(scriptName))
                .will(returnValue("/config/"+scriptName));
        servletContextMock.expects(once())
                .method("getResourceAsStream")
                .with(eq("/config/"+scriptName))
                .will(returnValue(new ByteArrayInputStream(script.getBytes())));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(BUILDER), isA(containerBuilder));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(APPLICATION_CONTAINER), isA(PicoContainer.class));

        listener.contextInitialized(new ServletContextEvent((ServletContext) servletContextMock.proxy()));
    }

    public void testApplicationScopeContainerIsNotBuildWhenNoInitParametersAreFound() {
        Mock servletContextMock = mock(ServletContext.class);
        final Vector initParams = new Vector();
        servletContextMock.expects(once())
                .method("getInitParameterNames")
                .withNoArguments()
                .will(returnValue(initParams.elements()));
        servletContextMock.expects(once())
                .method("log")
                .with(isA(String.class), isA(Exception.class));
        try {
            listener.contextInitialized(new ServletContextEvent(
                    (ServletContext) servletContextMock.proxy()));
            fail("PicoCompositionException expected");
        } catch (PicoCompositionException e) {
            assertEquals("Couldn't create a builder from context parameters in web.xml", e.getCause().getMessage());
        }
    }

    public void testApplicationScopeContainerIsNotBuildWhenInvalidParametersAreFound() {
        Mock servletContextMock = mock(ServletContext.class);
        final Vector initParams = new Vector();
        initParams.add("invalid-param");
        servletContextMock.expects(once())
                .method("getInitParameterNames")
                .withNoArguments()
                .will(returnValue(initParams.elements()));
        servletContextMock.expects(once())
                .method("log")
                .with(isA(String.class), isA(Exception.class));
        try {
            listener.contextInitialized(new ServletContextEvent(
                    (ServletContext) servletContextMock.proxy()));
            fail("PicoCompositionException expected");
        } catch (PicoCompositionException e) {
            assertEquals("Couldn't create a builder from context parameters in web.xml", e.getCause().getMessage());
        }
    }       

    public void testApplicationScopeContainerIsNotBuildWhenClassNotFound() {
        String script = 
            "<container>" +
            "<component-implementation class='com.inexistent.Foo'>" +
            "</component-implementation>" +
            "</container>";
        Mock servletContextMock = mock(ServletContext.class);
        final Vector initParams = new Vector();
        initParams.add("nanocontainer.xml");
        servletContextMock.expects(once())
                .method("getInitParameterNames")
                .withNoArguments()
                .will(returnValue(initParams.elements()));
        servletContextMock.expects(once())
                .method("getInitParameter")
                .with(eq("nanocontainer.xml"))
                .will(returnValue(script));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(BUILDER), isA(XMLContainerBuilder.class));
        servletContextMock.expects(once())
                .method("log")
                .with(isA(String.class), isA(Exception.class));
        try {
            listener.contextInitialized(new ServletContextEvent(
                    (ServletContext) servletContextMock.proxy()));
            fail("PicoCompositionException expected");
        } catch (Exception e) {
            assertNull(e.getMessage());
        }
    }       
    

    public void testApplicationScopeContainerIsKilledWhenContextDestroyed() {
        Mock servletContextMock = mock(ServletContext.class);
        Mock containerMock = mock(PicoContainer.class);
        containerMock.expects(once()).method("stop");
        containerMock.expects(once()).method("dispose");
        containerMock.expects(once()).method("getParent");
        servletContextMock.expects(atLeastOnce())
                .method("getAttribute")
                .with(eq(APPLICATION_CONTAINER)).will(returnValue(containerMock.proxy()));
        servletContextMock.expects(once())
                .method("setAttribute");
        listener.contextDestroyed(new ServletContextEvent(
                    (ServletContext) servletContextMock.proxy()));
    }       
        
    public void testSessionScopeContainerIsCreatedWithApplicationScopeContainerAsParent(){
        assertSessionScopeContainerIsCreatedWithApplicationScopeContainerAsParent(groovyScript, GroovyContainerBuilder.class);
        assertSessionScopeContainerIsCreatedWithApplicationScopeContainerAsParent(xmlScript, XMLContainerBuilder.class);
    }
    
    private void assertSessionScopeContainerIsCreatedWithApplicationScopeContainerAsParent(
            String script, Class containerBuilder) {
        Mock servletContextMock = mock(ServletContext.class);
        MutablePicoContainer appScopeContainer = new DefaultPicoContainer();
        servletContextMock.expects(once())
                .method("getAttribute")
                .with(eq(APPLICATION_CONTAINER))
                .will(returnValue(appScopeContainer));
        servletContextMock.expects(once())
                .method("getAttribute")
                .with(eq(BUILDER))
                .will(returnValue(createContainerBuilder(containerBuilder, script)));
        Mock httpSessionMock = mock(HttpSession.class);
        httpSessionMock.expects(once())
                .method("getServletContext")
                .withNoArguments()
                .will(returnValue(servletContextMock.proxy()));
        httpSessionMock.expects(once())
                .method("setAttribute")
                .with(eq(ServletContainerListener.KILLER_HELPER), isA(HttpSessionBindingListener.class));
        httpSessionMock.expects(once())
                .method("setAttribute")
                .with(eq(SESSION_CONTAINER), isA(PicoContainer.class));

        listener.sessionCreated(new HttpSessionEvent((HttpSession) httpSessionMock.proxy()));
    }
    
    
    private ContainerBuilder createContainerBuilder(Class containerBuilder, String script) {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.registerComponentInstance(new StringReader(script));
        pico.registerComponentInstance(getClass().getClassLoader());
        pico.registerComponentImplementation(containerBuilder);
        return (ContainerBuilder)pico.getComponentInstanceOfType(ContainerBuilder.class);
    }

    public void testSessionDestroyedMethodIsIgnored() {
        Mock httpSession = mock(HttpSession.class);
        listener.sessionDestroyed(new HttpSessionEvent((HttpSession)httpSession.proxy()));
    }
    
    public void testScopedContainerComposerIsCreatedWithDefaultConfiguration() {
        Mock servletContextMock = mock(ServletContext.class);
        final Vector initParams = new Vector();
        initParams.add(ServletContainerListener.CONTAINER_COMPOSER);
        servletContextMock.expects(once())
                .method("getInitParameterNames")
                .withNoArguments()
                .will(returnValue(initParams.elements()));
        servletContextMock.expects(once())
                .method("getInitParameter")
                .with(eq(ServletContainerListener.CONTAINER_COMPOSER))
                .will(returnValue(ScopedContainerComposer.class.getName()));
        servletContextMock.expects(once())
                .method("getInitParameter")
                .with(eq(ServletContainerListener.CONTAINER_COMPOSER_CONFIGURATION))
                .will(returnValue(null));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(BUILDER), isA(DefaultLifecycleContainerBuilder.class));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(APPLICATION_CONTAINER), isA(PicoContainer.class));
        listener.contextInitialized(new ServletContextEvent((ServletContext) servletContextMock.proxy()));
    }

    public void testScopedContainerComposerIsCreatedWithConfiguration() {
        String groovyConfig =
            "pico = builder.container(parent:parent, scope:assemblyScope) {\n" +
            "   component(class:'org.nanocontainer.nanowar.ScopedContainerConfigurator', \n"+
            "             parameters:['org.nanocontainer.script.groovy.GroovyContainerBuilder', " +
            "                         'nanowar-application.groovy', 'nanowar-session.groovy', " +
            "                         'nanowar-request.groovy' ])\n" +
            "}";
        assertScopedContainerComposerIsCreatedWithConfiguration("composer-config.groovy", groovyConfig);
        String xmlConfig =
            "<container>" +
            "<component-implementation class='org.nanocontainer.nanowar.ScopedContainerConfigurator'>" +
            "      <parameter><string>org.nanocontainer.script.xml.XMLContainerBuilder</string></parameter>" +
            "      <parameter><string>nanowar-application.xml</string></parameter> " +
            "      <parameter><string>nanowar-session.xml</string></parameter>        " +
            "      <parameter><string>nanowar-request.xml</string></parameter> " +
            "</component-implementation>" +
            "</container>";
        assertScopedContainerComposerIsCreatedWithConfiguration("composer-config.xml", xmlConfig);
    }
    
    private void assertScopedContainerComposerIsCreatedWithConfiguration(String scriptName, String script) {
        Mock servletContextMock = mock(ServletContext.class);
        final Vector initParams = new Vector();
        initParams.add(ServletContainerListener.CONTAINER_COMPOSER);
        servletContextMock.expects(once())
                .method("getInitParameterNames")
                .withNoArguments()
                .will(returnValue(initParams.elements()));
        servletContextMock.expects(once())
                .method("getInitParameter")
                .with(eq(ServletContainerListener.CONTAINER_COMPOSER))
                .will(returnValue(ScopedContainerComposer.class.getName()));
        servletContextMock.expects(once())
                .method("getInitParameter")
                .with(eq(ServletContainerListener.CONTAINER_COMPOSER_CONFIGURATION))
                .will(returnValue("nanowar/"+scriptName));
        servletContextMock.expects(once())
                .method("getResourceAsStream")
                .with(eq("nanowar/"+scriptName))
                .will(returnValue(new ByteArrayInputStream(script.getBytes())));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(BUILDER), isA(DefaultLifecycleContainerBuilder.class));
        servletContextMock.expects(once())
                .method("setAttribute")
                .with(eq(APPLICATION_CONTAINER), isA(PicoContainer.class));
        listener.contextInitialized(new ServletContextEvent(
                    (ServletContext) servletContextMock.proxy()));
    }

}

