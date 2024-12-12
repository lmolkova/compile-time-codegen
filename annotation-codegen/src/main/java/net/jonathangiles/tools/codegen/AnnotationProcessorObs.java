package net.jonathangiles.tools.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import net.jonathangiles.tools.codegen.annotations.BodyParam;
import net.jonathangiles.tools.codegen.annotations.HeaderParam;
import net.jonathangiles.tools.codegen.annotations.HostParam;
import net.jonathangiles.tools.codegen.annotations.HttpRequestInformation;
import net.jonathangiles.tools.codegen.annotations.PathParam;
import net.jonathangiles.tools.codegen.annotations.QueryParam;
import net.jonathangiles.tools.codegen.annotations.ServiceInterface;
import net.jonathangiles.tools.codegen.models.HttpRequestContext;
import net.jonathangiles.tools.codegen.models.Substitution;
import net.jonathangiles.tools.codegen.models.TemplateInput;
import net.jonathangiles.tools.codegen.templating.TemplateProcessor;
import net.jonathangiles.tools.codegen.utils.PathBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("net.jonathangiles.tools.codegen.annotations.ServiceMethod")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationProcessorObs extends AbstractProcessor {

    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // We iterate through each interface annotated with @ServiceInterface separately.
        // This outer for-loop is not strictly necessary, as we only have one annotation that we care about
        // (@ServiceInterface), but we'll leave it here for now
        annotations.stream()
                .map(roundEnv::getElementsAnnotatedWith)
                .flatMap(Set::stream)
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .forEach(this::processServiceMethod);

        return true;
    }

    private void processServiceMethod(ExecutableElement serviceMethod) {
        MethodScanner methodScanner = new MethodScanner();
        MethodTree methodTree = methodScanner.scan(serviceMethod, this.trees);

        BlockTree body = methodTree.getBody();

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + body.toString());
    }

    private static class MethodScanner extends TreePathScanner<List<MethodTree>, Trees> {
        private List<MethodTree> methodTrees = new ArrayList<>();

        public MethodTree scan(ExecutableElement methodElement, Trees trees) {
            List<MethodTree> methodTrees = this.scan(trees.getPath(methodElement), trees);
            assert methodTrees.size() == 1;

            return methodTrees.get(0);
        }

        @Override
        public List<MethodTree> scan(TreePath treePath, Trees trees) {
            super.scan(treePath, trees);
            return this.methodTrees;
        }

        @Override
        public List<MethodTree> visitMethod(MethodTree methodTree, Trees trees) {
            this.methodTrees.add(methodTree);
            return super.visitMethod(methodTree, trees);
        }
    }
}
