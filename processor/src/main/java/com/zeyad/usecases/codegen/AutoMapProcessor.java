package com.zeyad.usecases.codegen;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zeyad.usecases.annotations.AutoMap;
import com.zeyad.usecases.annotations.IgnoreInRealm;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * @author zeyad on 12/12/16.
 */
@SupportedAnnotationTypes("com.zeyad.usecases.annotations.AutoMap")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class AutoMapProcessor extends AbstractProcessor {
    private ErrorReporter mErrorReporter;
    private Types mTypeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mErrorReporter = new ErrorReporter(processingEnv);
        mTypeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Collection<? extends Element> annotatedElements = env.getElementsAnnotatedWith(AutoMap.class);
        List<TypeElement> types = new ImmutableList.Builder<TypeElement>()
                .addAll(ElementFilter.typesIn(annotatedElements))
                .build();
        for (TypeElement type : types) {
            processType(type);
        }
        // We are the only ones handling AutoMap annotations
        return true;
    }

    private void processType(TypeElement type) {
        AutoMap AutoMap = type.getAnnotation(AutoMap.class);
        if (AutoMap == null) {
            mErrorReporter.abortWithError("annotation processor for @AutoMap was invoked with a" +
                    "type annotated differently; compiler bug? O_o", type);
        }
        if (type.getKind() != ElementKind.CLASS) {
            mErrorReporter.abortWithError("@" + AutoMap.class.getName() + " only applies to classes", type);
        }
//        if (ancestorIsAutoMap(type)) {
//            mErrorReporter.abortWithError("One @AutoMap class shall not extend another", type);
//        }
        checkModifiersIfNested(type);
        // get the fully-qualified class name
        String fqClassName = generatedSubclassName(type, 0);
        // class name
        String className = TypeUtil.simpleNameOf(fqClassName);
//        String source = generateClass(type, className, type.getSimpleName().toString(), false);
        String source = generateDataClassFile(type, className);
        source = Reformatter.fixup(source);
        writeSourceFile(className, source, type);
    }

    private void writeSourceFile(String className, String text, TypeElement originatingType) {
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(className, originatingType);
            Writer writer = sourceFile.openWriter();
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            // This should really be an error, but we make it a warning in the hope of resisting Eclipse
            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=367599. If that bug manifests, we may get
            // invoked more than once for the same file, so ignoring the ability to overwrite it is the
            // right thing to do. If we are unable to write for some other reason, we should get a compile
            // error later because user code will have a reference to the code we were supposed to
            // generate (new AutoValue_Foo() or whatever) and that reference will be undefined.
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Could not write generated class " + className + ": " + e);
        }
    }

    private String generatedSubclassName(TypeElement type, int depth) {
        return generatedClassName(type, Strings.repeat("$", depth) + "AutoMap_");
    }

    private String generatedClassName(TypeElement type, String prefix) {
        String name = type.getSimpleName().toString();
        while (type.getEnclosingElement() instanceof TypeElement) {
            type = (TypeElement) type.getEnclosingElement();
            name = type.getSimpleName() + "_" + name;
        }
        String pkg = TypeUtil.packageNameOf(type);
        String dot = pkg.length() == 0 ? "" : ".";
        return pkg + dot + prefix + name;
    }

    private String generateClass(TypeElement type, String className, String classToExtend, boolean isFinal) {
        if (type == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null type", type);
        }
        if (className == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null class name", type);
        }
        if (classToExtend == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null parent class", type);
        }
        List<VariableElement> nonPrivateFields = getAutoMapFieldsOrError(type);
        if (nonPrivateFields.isEmpty()) {
            mErrorReporter.abortWithError("generateClass error, all fields are declared PRIVATE", type);
        }
        // get the properties
        ImmutableList<Property> properties = buildProperties(nonPrivateFields);
        // get the type adapters
        ImmutableMap<TypeMirror, FieldSpec> typeAdapters = getTypeAdapters(properties);
        // get the automap version
        // Generate the AutoMap_??? class
        String pkg = TypeUtil.packageNameOf(type);
        TypeSpec.Builder subClass = TypeSpec.classBuilder(className)
                // Add the version
                .addField(TypeName.INT, "version", PRIVATE)
                // Class must be always final
                .addModifiers(FINAL)
                // extends from original abstract class
                .superclass(ClassName.get(pkg, classToExtend))
                // Add the DEFAULT constructor
                .addMethod(generateConstructor(properties)); // generate writeToParcel()
        if (!ancestorIsAutoMap(processingEnv, type)) {
            // Implement android.os.Parcelable if the ancestor does not do it.
            subClass.addSuperinterface(ClassName.get("android.os", "Parcelable"));
        }
        if (!typeAdapters.isEmpty()) {
            typeAdapters.values().forEach(subClass::addField);
        }
        return JavaFile.builder(pkg, subClass.build()).build().toString();
    }

    private MethodSpec generateConstructor(ImmutableList<Property> properties) {
        List<ParameterSpec> params = Lists.newArrayListWithCapacity(properties.size());
        for (Property property : properties) {
            params.add(ParameterSpec.builder(property.typeName, property.fieldName).build());
        }
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addParameters(params);
        for (ParameterSpec param : params) {
            builder.addStatement("this.$N = $N", param.name, param.name);
        }
        return builder.build();
    }

    /**
     * This method returns a list of all non private fields. If any <code>private</code> fields is
     * found, the method errors out
     *
     * @param type element
     * @return list of all non-<code>private</code> fields
     */
    private List<VariableElement> getAutoMapFieldsOrError(TypeElement type) {
        List<VariableElement> allFields = ElementFilter.fieldsIn(type.getEnclosedElements());
        List<VariableElement> nonPrivateFields = new ArrayList<>();
        for (VariableElement field : allFields) {
            if (!field.getModifiers().contains(PRIVATE)) {
                nonPrivateFields.add(field);
            } else {
                // return error, PRIVATE fields are not allowed
                mErrorReporter.abortWithError("getFieldsError error, PRIVATE fields not allowed", type);
            }
        }
        return nonPrivateFields;
    }

    private ImmutableMap<TypeMirror, FieldSpec> getTypeAdapters(ImmutableList<Property> properties) {
        Map<TypeMirror, FieldSpec> typeAdapters = new LinkedHashMap<>();
        NameAllocator nameAllocator = new NameAllocator();
        nameAllocator.newName("CREATOR");
        for (Property property : properties) {
            if (property.typeAdapter != null && !typeAdapters.containsKey(property.typeAdapter)) {
                ClassName typeName = (ClassName) TypeName.get(property.typeAdapter);
                String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, typeName.simpleName());
                name = nameAllocator.newName(name, typeName);
                typeAdapters.put(property.typeAdapter, FieldSpec.builder(
                        typeName, NameAllocator.toJavaIdentifier(name), PRIVATE, STATIC, FINAL)
                        .initializer("new $T()", typeName).build());
            }
        }
        return ImmutableMap.copyOf(typeAdapters);
    }

    private ImmutableList<Property> buildProperties(List<VariableElement> elements) {
        ImmutableList.Builder<Property> builder = ImmutableList.builder();
        for (VariableElement element : elements) {
            builder.add(new Property(element.getSimpleName().toString(), element));
        }
        return builder.build();
    }

    private boolean ancestorIsAutoMap(TypeElement type) {
        while (true) {
            TypeMirror parentMirror = type.getSuperclass();
            if (parentMirror.getKind() == TypeKind.NONE) {
                return false;
            }
            TypeElement parentElement = (TypeElement) mTypeUtils.asElement(parentMirror);
            if (MoreElements.isAnnotationPresent(parentElement, AutoMap.class)) {
                return true;
            }
            type = parentElement;
        }
    }

    private boolean ancestorIsAutoMap(ProcessingEnvironment env, TypeElement type) {
        // TODO: 15/07/16 check recursively
        TypeMirror classType = type.asType();
        TypeMirror parcelable = env.getElementUtils().getTypeElement("android.os.Parcelable").asType();
        return TypeUtil.isClassOfType(env.getTypeUtils(), parcelable, classType);
    }

    private void checkModifiersIfNested(TypeElement type) {
        ElementKind enclosingKind = type.getEnclosingElement().getKind();
        if (enclosingKind.isClass() || enclosingKind.isInterface()) {
            if (type.getModifiers().contains(PRIVATE)) {
                mErrorReporter.abortWithError("@AutoMap class must not be private", type);
            }
            if (!type.getModifiers().contains(STATIC)) {
                mErrorReporter.abortWithError("Nested @AutoMap class must be static", type);
            }
        }
        // In principle type.getEnclosingElement() could be an ExecutableElement (for a class
        // declared inside a method), but since RoundEnvironment.getElementsAnnotatedWith doesn't
        // return such classes we won't see them here.
    }

    // TODO: 12/14/16 Test With Realm Annotations
    private String generateDataClassFile(TypeElement type, String className) {
        String parameterName = type.asType().getClass().getSimpleName();
        MethodSpec.Builder isEmptyBuilder = MethodSpec.methodBuilder("isEmpty")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(boolean.class)
                .addParameter(type.asType().getClass(), parameterName);
        String isEmptyImplementation = "return ";
        // constructor
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .superclass(ClassName.get("io.realm", "RealmObject"))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructor);

        // get the properties
        List<VariableElement> allFields = ElementFilter.fieldsIn(type.getEnclosedElements());
        ImmutableList<Property> properties = buildProperties(allFields);
        // get the type adapters
        ImmutableMap<TypeMirror, FieldSpec> typeAdapters = getTypeAdapters(properties);

        typeAdapters.values().forEach(fieldSpec -> {
//            if (fieldSpec.annotations.contains(IgnoreInRealm.class)) {
            TypeName typeName = TypeName.get(type.asType().getClass());

            String variableName = fieldSpec.name;
            FieldSpec.Builder builder = FieldSpec.builder(typeName, variableName);
            // add field
            for (Modifier modifier : fieldSpec.modifiers)
                builder.addModifiers(modifier);
//                if (fieldSpec.getAnnotation(RealmPrimaryKey.class) != null)
//                    builder = builder.addAnnotation(PrimaryKey.class);
//                if (fieldSpec.getAnnotation(SerializedName.class) != null)
//                    builder = builder.addAnnotation(AnnotationSpec.builder(SerializedName.class)
//                            .addMember("value", "$S", fieldSpec.getAnnotation(SerializedName.class).value())
//                            .build());
            classBuilder.addField(builder.build());
            // add setter
            classBuilder.addMethod(MethodSpec.methodBuilder("set" + variableName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(type.asType().getClass(), type.asType().getClass().getSimpleName())
                    .addCode("this.$S = $S", variableName)
                    .build());
            // add getter
            classBuilder.addMethod(MethodSpec.methodBuilder("get" + variableName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(typeName)
                    .addCode("return $S", variableName)
                    .build());
//            }
        });
        for (int i = 0, allFieldsSize = allFields.size(); i < allFieldsSize; i++) {
            VariableElement variableElement = allFields.get(i);
            if (variableElement.getAnnotation(IgnoreInRealm.class) != null) {
                TypeName typeName = TypeName.get(type.asType().getClass());

                String variableName = variableElement.getSimpleName().toString();
                FieldSpec.Builder builder = FieldSpec.builder(typeName, variableName);
                // add field
                for (Modifier modifier : variableElement.getModifiers())
                    builder.addModifiers(modifier);

//                if (variableElement.getAnnotation(RealmPrimaryKey.class) != null)
//                    builder = builder.addAnnotation(PrimaryKey.class);
                if (variableElement.getAnnotation(SerializedName.class) != null)
                    builder = builder.addAnnotation(AnnotationSpec.builder(SerializedName.class)
                            .addMember("value", "$S", variableElement.getAnnotation(SerializedName.class).value())
                            .build());
                classBuilder.addField(builder.build());
                // add setter
                classBuilder.addMethod(MethodSpec.methodBuilder("set" + variableName)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class)
                        .addParameter(type.asType().getClass(), type.asType().getClass().getSimpleName())
                        .addCode("this.$S = $S", variableName)
                        .build());
                // add getter
                classBuilder.addMethod(MethodSpec.methodBuilder("get" + variableName)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(typeName)
                        .addCode("return $S", variableName)
                        .build());
                // isEmpty implementation
                if (!variableElement.asType().getKind().isPrimitive()) {
                    isEmptyImplementation += parameterName + "." + variableName + " == null";
                    if (i == allFieldsSize - 1) {
                        isEmptyImplementation += ";";
                    } else {
                        isEmptyImplementation += " && ";
                    }
                }
            }
        }

        MethodSpec isEmpty = isEmptyBuilder.addCode(isEmptyImplementation).build();

//        classBuilder.addMethod(isEmpty);

        String pkg = TypeUtil.packageNameOf(type);

        return JavaFile.builder(pkg, classBuilder.build()).build().toString();
    }

    public TypeSpec generateDAOMapper(TypeElement type, String className) {
        List<VariableElement> allFields = ElementFilter.fieldsIn(type.getEnclosedElements());
//        VariableElement variableElement = nonPrivateFields.get(0);
//        variableElement.getEnclosingElement().asType().getKind();
        String checkIfEmptyStub = "if (" + className + ".isEmpty())\nreturn new " + className + "();\n";
        String instanceVariables = "";
        for (VariableElement variableElement : allFields) {
            instanceVariables += variableElement.getModifiers().toArray().toString() + " ";
            instanceVariables += variableElement.asType().getKind().getClass() + " ";
            instanceVariables += variableElement.getSimpleName() + ";\n";
        }
        String code = checkIfEmptyStub + className + "generatedC";

        MethodSpec mapToDomainManual = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC)
                .returns(Object.class)
                .addAnnotation(Override.class)
                .addParameter(Object.class, "object")
//                .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
                .build();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();

        return TypeSpec.classBuilder(className)
//                .superclass()
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructor)
                .addMethod(mapToDomainManual)
                .build();
    }

    static final class Property {
        final String fieldName;
        final VariableElement element;
        final TypeName typeName;
        final ImmutableSet<String> annotations;
        //        final int version;
        TypeMirror typeAdapter;

        Property(String fieldName, VariableElement element) {
            this.fieldName = fieldName;
            this.element = element;
            this.typeName = TypeName.get(element.asType());
            this.annotations = getAnnotations(element);
            // get the parcel adapter if any
//            ParcelAdapter parcelAdapter = element.getAnnotation(ParcelAdapter.class);
//            if (parcelAdapter != null) {
//                try {
//                    parcelAdapter.value();
//                } catch (MirroredTypeException e) {
//                    this.typeAdapter = e.getTypeMirror();
//                }
//            }
            // get the element version, default 0
//            ParcelVersion parcelVersion = element.getAnnotation(ParcelVersion.class);
//            this.version = parcelVersion == null ? 0 : parcelVersion.from();
        }

        public boolean isNullable() {
            return this.annotations.contains("Nullable");
        }

//        public int version() {
//            return this.version;
//        }

        private ImmutableSet<String> getAnnotations(VariableElement element) {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
            }
            return builder.build();
        }
    }
}