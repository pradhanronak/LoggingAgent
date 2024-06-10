package com.example;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Enumeration;

public class LoggingAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        new AgentBuilder.Default()
                .type(ElementMatchers.nameStartsWith("com.example.sumapp")) // Replace with your package
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        return builder.visit(Advice.to(MethodLogger.class).on(ElementMatchers.isMethod()));
                    }
                }).installOn(inst);
    }

    public static class MethodLogger {

        private static final String LOG_FILE = "C:\\Users\\Rohan\\IdeaProjects\\method_calls.log";

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Origin Class<?> clazz, @Advice.Origin("#m") String method, @Advice.AllArguments Object[] args) {
            // Retrieve current HttpServletRequest
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                if ("GET".equalsIgnoreCase(request.getMethod())) {
                    Enumeration<String> parameterNames = request.getParameterNames();
                    while (parameterNames.hasMoreElements()) {
                        String paramName = parameterNames.nextElement();
                        if ("TestCaseID".equalsIgnoreCase(paramName)) {
                            String paramValue = request.getParameter(paramName);
                            logMethodCall(clazz, method, paramValue);
                        }
                    }
                }
            }
        }

        private static void logMethodCall(Class<?> clazz, String method, String paramValue) {
            String logMessage = "Entering method: " + clazz.getName() + "." + method + " with TestCaseID=" + paramValue;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(logMessage);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
