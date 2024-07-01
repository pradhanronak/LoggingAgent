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
import java.util.Stack;

public class LoggingAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        new AgentBuilder.Default()
                .type(ElementMatchers.nameStartsWith("com.example")) // Replace with your package
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

        public static final String LOG_FILE_PATH = "C:\\Users\\Rohan\\IdeaProjects\\method_calls.log"; // Specify the full file path
        public static final ThreadLocal<Stack<String>> methodStack = ThreadLocal.withInitial(Stack::new);

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Origin Class<?> clazz, @Advice.Origin("#m") String method, @Advice.AllArguments Object[] args) {
            // Add method to stack
            String methodName = clazz.getName() + "." + method;
            methodStack.get().push(methodName);

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

        @Advice.OnMethodExit
        public static void onExit(@Advice.Origin Class<?> clazz, @Advice.Origin("#m") String method) {
            // Remove method from stack
            String methodName = clazz.getName() + "." + method;
            if (!methodStack.get().isEmpty() && methodStack.get().peek().equals(methodName)) {
                methodStack.get().pop();
            }
        }

        public static void logMethodCall(Class<?> clazz, String method, String paramValue) {
            StringBuilder logMessage = new StringBuilder(paramValue + ": ");
            Stack<String> stack = methodStack.get();
            for (int i = 0; i < stack.size(); i++) {
                logMessage.append(stack.get(i));
                if (i < stack.size() - 1) {
                    logMessage.append(", ");
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
                writer.write(logMessage.toString());
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
