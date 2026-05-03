package com.onboardingmentor.context;

import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;

import java.util.*;

public class ClassContextExtractor {

    public static ClassContext extract(PsiClass psiClass) {
        String className = psiClass.getQualifiedName() != null ? psiClass.getQualifiedName() : psiClass.getName();
        String source = psiClass.getText();

        List<String> annotations = new ArrayList<>();
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            annotations.add(annotation.getText());
        }

        List<String> fields = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
            fields.add(field.getType().getPresentableText() + " " + field.getName());
        }

        List<String> callers = new ArrayList<>();
        ReferencesSearch.search(psiClass).forEach(new Processor<PsiReference>() {
            int count = 0;
            @Override
            public boolean process(PsiReference psiReference) {
                if (count >= 10) return false;
                PsiElement element = psiReference.getElement();
                PsiClass callerClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                if (callerClass != null && callerClass.getQualifiedName() != null) {
                    String callerName = callerClass.getQualifiedName();
                    if (!callers.contains(callerName)) {
                        callers.add(callerName);
                        count++;
                    }
                }
                return true;
            }
        });

        Set<String> calleesSet = new HashSet<>();
        psiClass.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                if (calleesSet.size() >= 10) return;
                PsiMethod method = expression.resolveMethod();
                if (method != null) {
                    PsiClass containingClass = method.getContainingClass();
                    if (containingClass != null && containingClass.getQualifiedName() != null) {
                        calleesSet.add(containingClass.getQualifiedName());
                    }
                }
            }
        });
        List<String> callees = new ArrayList<>(calleesSet);

        return new ClassContext(className, source, callers, callees, annotations, fields);
    }
}
